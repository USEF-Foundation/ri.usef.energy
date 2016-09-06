/*
 * Copyright 2015-2016 USEF Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package energy.usef.core.service.rest.sender;

import static energy.usef.core.service.business.error.ParticipantDiscoveryError.RECIPIENT_ROLE_NOT_PROVIDED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.MediaType.TEXT_XML;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.constant.USEFLogCategory;
import energy.usef.core.data.participant.Participant;
import energy.usef.core.data.participant.ParticipantRole;
import energy.usef.core.data.participant.ParticipantType;
import energy.usef.core.data.xml.bean.message.Message;
import energy.usef.core.data.xml.bean.message.MessagePrecedence;
import energy.usef.core.data.xml.bean.message.SignedMessage;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.exception.TechnicalException;
import energy.usef.core.model.MessageDirection;
import energy.usef.core.service.business.MessageEncryptionService;
import energy.usef.core.service.business.MessageService;
import energy.usef.core.service.business.ParticipantDiscoveryService;
import energy.usef.core.service.business.error.IncomingMessageError;
import energy.usef.core.service.helper.NotificationHelperService;
import energy.usef.core.service.rest.sender.error.SenderError;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.XMLUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpBackOffIOExceptionHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.BackOff;
import com.google.api.client.util.ExponentialBackOff;

/**
 * This sender service is designed to send outgoing messages to a recipient.
 */
@Stateless
public class SenderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SenderService.class);
    private static final Logger LOGGER_CONFIDENTIAL = LoggerFactory.getLogger(USEFLogCategory.CONFIDENTIAL);

    @Inject
    private MessageService messageService;

    @Inject
    private MessageEncryptionService messageEncryptionService;

    @Inject
    private ParticipantDiscoveryService participantDiscoveryService;

    @Inject
    private NotificationHelperService notificationHelperService;

    @Inject
    private Config config;

    /**
     * Sends an outgoing message to a recipient. If an attempt is not successful, the application tries the repeat the attempt a
     * predefined number of times.
     *
     * @param xmlString XML representation of the message
     * @throws BusinessException
     */
    public void sendMessage(String xmlString) {
        LOGGER.debug("Started sending message");
        LOGGER_CONFIDENTIAL.debug("Trying to send message {} ", xmlString);

        Message dtoMessage = (Message) XMLUtil
                .xmlToMessage(xmlString, config.getBooleanProperty(ConfigParam.VALIDATE_OUTGOING_XML).booleanValue());
        energy.usef.core.model.Message storedMessage = messageService.storeMessage(xmlString, dtoMessage, MessageDirection.OUTBOUND);

        LocalDateTime validUntil = dtoMessage.getMessageMetadata().getValidUntil();
        if (validUntil != null && validUntil.isBefore(DateTimeUtil.getCurrentDateTime())) {
            reportMessageNotSentError(storedMessage, dtoMessage, "validUntil time has passed", null);
            return;
        }
        try {
            // create a Backoff Strategy based on Precedence
            BackOff backoff = createExponentialBackOff(dtoMessage.getMessageMetadata().getPrecedence());
            // get retries
            int retries = getHttpRequestMaxRetries(dtoMessage
                    .getMessageMetadata().getPrecedence());

            // create the url
            String url = createUrl(dtoMessage);

            SignedMessage signedMessage = createSignedMessage(xmlString, dtoMessage);

            HttpRequest request = buildHttpRequest(url, signedMessage, backoff, retries);

            // send request
            HttpResponse response = request.execute();

            notificationHelperService.notifyNoMessageResponse(xmlString, dtoMessage);

            handleResponseStatuses(storedMessage, dtoMessage, response);
            response.disconnect();

        } catch (HttpResponseException e) {
            reportMessageNotSentError(storedMessage, dtoMessage, e.getMessage(), e.getStatusCode());
        } catch (IOException | BusinessException e) {
            reportMessageNotSentError(storedMessage, dtoMessage, e.getMessage(), null);
        }
    }

    /**
     * Handles the message according to the response status.
     * 
     * @param storedMessage - {@link energy.usef.core.model.Message}
     * @param dtoMessage - {@link Message}
     * @param response - {@link HttpResponse}
     * @throws IOException if the content of the HttpResponse cannot be read.
     */
    private void handleResponseStatuses(energy.usef.core.model.Message storedMessage, Message dtoMessage, HttpResponse response)
            throws IOException {
        int statusCode = response.getStatusCode();
        String errorMessage = IOUtils.toString(response.getContent());
        if (Status.OK.getStatusCode() != statusCode) {
            reportMessageNotSentError(storedMessage, dtoMessage, errorMessage, statusCode);
        } else if (IncomingMessageError.MESSAGE_ID_ALREADY_USED.getError().equals(errorMessage)) {
            reportPermanentError(storedMessage, errorMessage);
        } else if (IncomingMessageError.ALREADY_RECEIVED_AND_SUCCESSFULLY_PROCESSED.getError().equals(errorMessage)) {
            reportPermanentError(storedMessage, errorMessage);
        }
    }

    /**
     * Stores a permanent error of a message which has been successfully delivered (HTTP code 200).
     *
     * @param storedMessage {@link energy.usef.core.model.Message}
     * @param errorMessage content of the HTTP response.
     */
    private void reportPermanentError(energy.usef.core.model.Message storedMessage, String errorMessage) {
        LOGGER.error("The sending was successful but an error has occurred: {}", errorMessage);
        messageService.storeMessageError(storedMessage, errorMessage, Status.OK.getStatusCode());
    }

    /**
     * Stores an error about a message which has not been successfully delivered (HTTP code not 200).
     * 
     * @param storedMessage - {@link energy.usef.core.model.Message}
     * @param dtoMessage - {@link Message}
     * @param errorMessage - content of the HTTP response
     * @param errorCode - HTTP error code.
     */
    private void reportMessageNotSentError(energy.usef.core.model.Message storedMessage, Message dtoMessage, String errorMessage,
            Integer errorCode) {
        LOGGER.error("Sending message to {} {} failed: {}", dtoMessage.getMessageMetadata().getRecipientRole(), dtoMessage
                .getMessageMetadata().getRecipientDomain(), errorMessage);

        messageService.storeMessageError(storedMessage, errorMessage, errorCode);
        notificationHelperService.notifyMessageNotSent(storedMessage.getXml(), dtoMessage);
    }

    private HttpRequest buildHttpRequest(String address, SignedMessage message, BackOff backoff, int retries) throws IOException {
        GenericUrl targetURL = new GenericUrl(address);

        LOGGER.debug("Sending message to the target URL: {}", targetURL);

        // Create new HTTP Transport given the configuration (bypass TLS or not)
        HttpTransport httpTransport = createHttpTransport();

        HttpRequestFactory requestFactory = httpTransport.createRequestFactory();

        String xmlMessage = XMLUtil.messageObjectToXml(message);

        ByteArrayContent content = new ByteArrayContent(TEXT_XML,
                xmlMessage.getBytes(UTF_8));

        HttpRequest request = requestFactory.buildPostRequest(targetURL, content);

        // Setting Unsuccessful Response Handler
        request.setUnsuccessfulResponseHandler(new HttpBackOffUnsuccessfulResponseHandler(
                backoff, config.getIntegerPropertyList(ConfigParam.RETRY_HTTP_ERROR_CODES)) {
            public boolean handleResponse(
                    HttpRequest request, HttpResponse response, boolean supportsRetry) throws IOException {
                boolean result = super.handleResponse(request, response, supportsRetry);
                if (result) {
                    LOGGER.warn("The sending attempt was not successful, next attempt will be done.");
                }
                return result;
            }
        });

        // Setting IO Exception Handler
        request.setIOExceptionHandler(new HttpBackOffIOExceptionHandler(
                backoff) {
            public boolean handleIOException(HttpRequest request, boolean supportsRetry) throws IOException {
                boolean result = super.handleIOException(request, supportsRetry);
                if (result) {
                    LOGGER.warn("The sending attempt was not successful, next attempt will be done.");
                }
                return result;
            }
        });

        request.setNumberOfRetries(retries);

        return request;

    }

    /**
     * Creates a {@link NetHttpTransport} object which will validate or not (depending on the property file) the certificate of the
     * destination party.
     *
     * @return a {@link NetHttpTransport}
     */
    private HttpTransport createHttpTransport() {
        if (config.getBooleanProperty(ConfigParam.BYPASS_TLS_VERIFICATION)) {
            LOGGER.warn("TLS/SSL verification is disabled. Certificates of the destination of the message will not be checked.");
            try {
                return new NetHttpTransport.Builder()
                        .doNotValidateCertificate()
                        .build();
            } catch (GeneralSecurityException e) {
                throw new TechnicalException(e);
            }
        }
        return new NetHttpTransport.Builder().build();
    }

    /*
     * Creates a SignedMessage object
     */
    private SignedMessage createSignedMessage(String xmlMessage, Message dtoMessage) throws BusinessException {
        SignedMessage signedMessage = new SignedMessage();
        signedMessage.setSenderDomain(config.getProperty(ConfigParam.HOST_DOMAIN));
        signedMessage.setSenderRole(dtoMessage.getMessageMetadata().getSenderRole());
        signedMessage.setBody(messageEncryptionService.sealMessage(xmlMessage));
        return signedMessage;
    }

    /*
     * Build a correct url
     */
    private String createUrl(Message message) throws BusinessException {
        Participant participant = participantDiscoveryService.discoverParticipant(message, ParticipantType.RECIPIENT);

        USEFRole targetRole = message.getMessageMetadata().getRecipientRole();
        ParticipantRole participantRole = null;
        for (ParticipantRole role : participant.getRoles()) {
            if (targetRole.equals(role.getUsefRole())) {
                participantRole = role;
                break;
            }
        }

        if (participantRole == null) {
            throw new BusinessException(RECIPIENT_ROLE_NOT_PROVIDED);
        }

        return participantRole.getUrl();
    }

    /*
     * Creates a BackOff strategy dependent on MessagePrecedence
     */
    private BackOff createExponentialBackOff(MessagePrecedence messagePrecedence) throws BusinessException {

        if (MessagePrecedence.ROUTINE.equals(messagePrecedence)) {
            return new ExponentialBackOff.Builder()
                    .setInitialIntervalMillis(
                            config.getIntegerProperty(ConfigParam.ROUTINE_EXPONENTIAL_BACKOFF_INITIAL_INTERVAL_MILLIS))
                    .setMaxElapsedTimeMillis(
                            config.getIntegerProperty(ConfigParam.ROUTINE_EXPONENTIAL_BACKOFF_MAX_ELAPSED_TIME_MILLIS))
                    .setMaxIntervalMillis(
                            config.getIntegerProperty(ConfigParam.ROUTINE_EXPONENTIAL_BACKOFF_MAX_INTERVAL_MILLIS))
                    .setMultiplier(config.getDoubleProperty(ConfigParam.ROUTINE_EXPONENTIAL_BACKOFF_MULTIPLIER))
                    .setRandomizationFactor(
                            config.getDoubleProperty(ConfigParam.ROUTINE_EXPONENTIAL_BACKOFF_RANDOMIZATION_FACTOR))
                    .build();
        } else if (MessagePrecedence.TRANSACTIONAL.equals(messagePrecedence)) {
            return new ExponentialBackOff.Builder()
                    .setInitialIntervalMillis(
                            config.getIntegerProperty(ConfigParam.TRANSACTIONAL_EXPONENTIAL_BACKOFF_INITIAL_INTERVAL_MILLIS))
                    .setMaxElapsedTimeMillis(
                            config.getIntegerProperty(ConfigParam.TRANSACTIONAL_EXPONENTIAL_BACKOFF_MAX_ELAPSED_TIME_MILLIS))
                    .setMaxIntervalMillis(
                            config.getIntegerProperty(ConfigParam.TRANSACTIONAL_EXPONENTIAL_BACKOFF_MAX_INTERVAL_MILLIS))
                    .setMultiplier(config.getDoubleProperty(ConfigParam.TRANSACTIONAL_EXPONENTIAL_BACKOFF_MULTIPLIER))
                    .setRandomizationFactor(
                            config.getDoubleProperty(ConfigParam.TRANSACTIONAL_EXPONENTIAL_BACKOFF_RANDOMIZATION_FACTOR))
                    .build();
        } else if (MessagePrecedence.CRITICAL.equals(messagePrecedence)) {
            return new ExponentialBackOff.Builder()
                    .setInitialIntervalMillis(
                            config.getIntegerProperty(ConfigParam.CRITICAL_EXPONENTIAL_BACKOFF_INITIAL_INTERVAL_MILLIS))
                    .setMaxElapsedTimeMillis(
                            config.getIntegerProperty(ConfigParam.CRITICAL_EXPONENTIAL_BACKOFF_MAX_ELAPSED_TIME_MILLIS))
                    .setMaxIntervalMillis(
                            config.getIntegerProperty(ConfigParam.CRITICAL_EXPONENTIAL_BACKOFF_MAX_INTERVAL_MILLIS))
                    .setMultiplier(config.getDoubleProperty(ConfigParam.CRITICAL_EXPONENTIAL_BACKOFF_MULTIPLIER))
                    .setRandomizationFactor(
                            config.getDoubleProperty(ConfigParam.CRITICAL_EXPONENTIAL_BACKOFF_RANDOMIZATION_FACTOR))
                    .build();
        }

        LOGGER.error("No supported message type");
        throw new BusinessException(SenderError.NO_MESSAGE_TYPE);
    }

    /*
     * Retrieve the correct amount of retries based on the Precedence
     */
    private int getHttpRequestMaxRetries(MessagePrecedence messagePrecedence) throws BusinessException {
        if (MessagePrecedence.ROUTINE.equals(messagePrecedence)) {
            return config.getIntegerProperty(ConfigParam.ROUTINE_HTTP_REQUEST_MAX_RETRIES);
        } else if (MessagePrecedence.TRANSACTIONAL.equals(messagePrecedence)) {
            return config.getIntegerProperty(ConfigParam.TRANSACTIONAL_HTTP_REQUEST_MAX_RETRIES);
        } else if (MessagePrecedence.CRITICAL.equals(messagePrecedence)) {
            return config.getIntegerProperty(ConfigParam.CRITICAL_HTTP_REQUEST_MAX_RETRIES);
        }
        LOGGER.error("No supported message type");
        throw new BusinessException(SenderError.NO_MESSAGE_TYPE);
    }

}
