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

package energy.usef.core.endpoint;

import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.constant.USEFLogCategory;
import energy.usef.core.data.xml.bean.message.Message;
import energy.usef.core.data.xml.bean.message.SignedMessage;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.exception.TechnicalException;
import energy.usef.core.service.business.IncomingMessageVerificationService;
import energy.usef.core.service.business.MessageEncryptionService;
import energy.usef.core.service.business.MessageFilterService;
import energy.usef.core.service.business.ParticipantDiscoveryService;
import energy.usef.core.service.business.error.IncomingMessageError;
import energy.usef.core.service.business.error.MessageFilterError;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.util.XMLUtil;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class class has the logic to expose a REST endpoint to receive USEF messages. It does some basic checks on the message
 * before sending it to a queue for further processing. It handles the HTTP Response as well.
 */
@Path("/ReceiverService")
public class ReceiverEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiverEndpoint.class);
    private static final Logger LOGGER_CONFIDENTIAL = LoggerFactory.getLogger(USEFLogCategory.CONFIDENTIAL);

    @Inject
    private Config config;

    @Inject
    private JMSHelperService jmsService;

    @Inject
    private IncomingMessageVerificationService incomingMessageVerificationService;

    @Inject
    private MessageEncryptionService messageEncryptionService;

    @Inject
    private MessageFilterService messageFilterService;

    @Inject
    private ParticipantDiscoveryService participantDiscoveryService;

    /**
     * Sends a client message to a queue.
     *
     * @param messageText message
     * @param request {@link HttpServletRequest}
     * @return status
     */
    @POST
    @Path("/receiveMessage")
    @Consumes(TEXT_XML)
    public Response receiveMessage(String messageText, @Context HttpServletRequest request) {
        try {
            // verify that the signed message has not been received yet.
            incomingMessageVerificationService.checkSignedMessageHash(DigestUtils.sha256(messageText));

            // transform the text/xml to a SignedMessage message
            SignedMessage signedMessage = XMLUtil.xmlToMessage(messageText, SignedMessage.class,
                    config.getBooleanProperty(ConfigParam.VALIDATE_INCOMING_XML).booleanValue());



            // Get original senders IP-address, both directly and from the proxy('s).
            String addresslist = request.getRemoteAddr() + "," + request.getRemoteHost();
            String address = request.getHeader("X-Forwarded-For");
            if (address != null) {
                addresslist += "," + address;
            }

            // check if the sender is allowed to send messages to this endpoint
            messageFilterService.filterMessage(signedMessage.getSenderDomain(), addresslist);

            // verify sender by trying to unsing message
            String unsignedContent = verifyMessage(signedMessage);
            LOGGER_CONFIDENTIAL.debug("Received msg: {} ", unsignedContent);

            Message message = (Message) XMLUtil.xmlToMessage(unsignedContent,
                    config.getBooleanProperty(ConfigParam.VALIDATE_INCOMING_XML).booleanValue());

            incomingMessageVerificationService.validateMessageId(message.getMessageMetadata().getMessageID());
            incomingMessageVerificationService.validateMessageValidUntil(message.getMessageMetadata().getValidUntil());

            // Check if the metadata is correct and the participant exists
            incomingMessageVerificationService.validateSender(signedMessage, message);

            jmsService.sendMessageToInQueue(unsignedContent);

            return Response.status(OK).entity("Correctly received msg " + unsignedContent
                    + " and set in to the IN queue").build();

        } catch (BusinessException e) {
            LOGGER.warn(e.getMessage(), e);
            return createBusinessErrorResponse(e);
        } catch (TechnicalException e) {
            LOGGER.error(e.getMessage(), e);
            return createErrorResponse(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return createErrorResponse("Unknown server problem occurred.");
        }
    }

    private Response createBusinessErrorResponse(BusinessException e) {
        String errorMessage = e.getBusinessError().getError();
        Status status = BAD_REQUEST;

        if (e.getBusinessError() == MessageFilterError.PARTICIPANT_NOT_ALLOWLISTED) {
            status = Status.UNAUTHORIZED;
        }
        if (e.getBusinessError() == MessageFilterError.ADDRESS_IS_DENYLISTED) {
            status = Status.UNAUTHORIZED;
        }
        if (e.getBusinessError() == IncomingMessageError.MESSAGE_ID_ALREADY_USED) {
            status = Status.OK;
        }
        if (e.getBusinessError() == IncomingMessageError.ALREADY_RECEIVED_AND_SUCCESSFULLY_PROCESSED) {
            status = Status.OK;
        }
        return Response.status(status).entity(errorMessage).build();
    }

    private String verifyMessage(SignedMessage incomingMessage) throws BusinessException {
        if (incomingMessage == null || incomingMessage.getBody() == null) {
            throw new BusinessException(IncomingMessageError.NULL_MESSAGE);
        }
        String publicKey = participantDiscoveryService.findUnsealingPublicKey(incomingMessage);
        LOGGER_CONFIDENTIAL.debug("Public key found: {}", publicKey);
        byte[] sealedContent = incomingMessage.getBody();

        return messageEncryptionService.verifyMessage(sealedContent, publicKey);
    }

    private Response createErrorResponse(String message) {
        return Response.status(BAD_REQUEST).entity(message).build();
    }
}
