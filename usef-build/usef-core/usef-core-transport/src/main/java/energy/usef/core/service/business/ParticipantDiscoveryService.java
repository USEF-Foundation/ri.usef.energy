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

package energy.usef.core.service.business;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import javax.ejb.Singleton;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.DClass;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.Section;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.Type;

import energy.usef.core.config.AbstractConfig;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.constant.USEFLogCategory;
import energy.usef.core.data.participant.Participant;
import energy.usef.core.data.participant.ParticipantRole;
import energy.usef.core.data.participant.ParticipantType;
import energy.usef.core.data.xml.bean.message.Message;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.SignedMessage;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.exception.VersionError;
import energy.usef.core.service.business.error.ParticipantDiscoveryError;

/**
 * Service class in charge of the discovery of the participants on the network when a message arrives.
 */
@Singleton
public class ParticipantDiscoveryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantDiscoveryService.class);
    private static final Logger LOGGER_CONFIDENTIAL = LoggerFactory.getLogger(USEFLogCategory.CONFIDENTIAL);
    private static final String PUBLIC_KEY_PREFIX = "cs1.";
    private static final String PARTICIPANTS_YAML = "participants_dns_info.yaml";
    private static final String SUPPORTED_USEF_VERSION = "2015";

    private static Resolver resolver = null;

    static {
        try {
            resolver = new ExtendedResolver();
        } catch (UnknownHostException e) {
            LOGGER.error("No DNS resolver can be found!", e);
        }
    }

    @Inject
    private Config config;

    @Inject
    private ParticipantListBuilder participantListBuilder;

    /**
     * Discover the USEF participants matching the incoming {@link Message} sender.
     *
     * @param incomingMessage - {@link Message}
     * @param participantType the {@link ParticipantType}.
     * @return a {@link Participant} given the domain given in the incoming message and the {@link ParticipantType}.
     * @throws BusinessException
     */
    public Participant discoverParticipant(Message incomingMessage, ParticipantType participantType) throws BusinessException {

        MessageMetadata metadata = incomingMessage.getMessageMetadata();
        if (metadata == null || metadata.getSenderDomain() == null) {
            return null;
        }

        String domain = participantType == ParticipantType.SENDER ? metadata.getSenderDomain() : metadata.getRecipientDomain();
        USEFRole participantRole = participantType == ParticipantType.SENDER ? metadata.getSenderRole() : metadata
                .getRecipientRole();

        // check if one bypasses the DNS verification
        if (byPassDNSCheck()) {
            LOGGER.warn("DNS verification is bypassed.");
            return findParticipantInLocalConfiguration(domain, participantRole);
        } else {
            LOGGER.info("DNS verification is active.");
            return findParticipantInDns(domain, participantRole);
        }
    }

    /**
     * Find the unsealing public key of the message sender.
     *
     * @param incomingMessage - {@link Message}
     * @return a Base64 encoded public key ( {@link String} )
     * @throws BusinessException
     */
    public String findUnsealingPublicKey(SignedMessage incomingMessage) throws BusinessException {
        String value;

        String senderDomain = incomingMessage.getSenderDomain();
        USEFRole senderRole = incomingMessage.getSenderRole();

        if (byPassDNSCheck()) {
            checkSenderDomainAndRoleAvailable(incomingMessage);
            value = findLocalParticipantUnsigningPublicKey(senderDomain, senderRole);
        } else {
            value = getPublicUnsealingKey(senderDomain, senderRole);
        }
        return StringUtils.removeStart(value, PUBLIC_KEY_PREFIX);
    }

    /**
     * Checks whether a participant is found in the list of locally configured USEF participants (YAML file).
     *
     * @param domain - {@link String} containing the participants's domain.
     * @param participantRole - {@link USEFRole} indicating the participant's role.
     * @throws BusinessException if no participant is matching the sender.
     */
    private Participant findParticipantInLocalConfiguration(String domain, USEFRole participantRole)
            throws BusinessException {

        String participantDnsFileName = Config.getConfigurationFolder() +
                config.getProperty(ConfigParam.PARTICIPANT_DNS_INFO_FILENAME);
        if (!isFileExists(participantDnsFileName)) {
            participantDnsFileName = AbstractConfig.DOMAIN_CONFIG_FOLDER + File.separator + PARTICIPANTS_YAML;
        }

        List<Participant> participants = participantListBuilder.buildParticipantList(participantDnsFileName);

        checkParticipantsListIsAvailable(participants);

        /*
         * for each participant role, try to find if the sender's address matches the hostname or the EA. At the end of the loops,
         * if nothing has been found, throw a PARTICIPANT_NOT_FOUND error.
         */
        Participant foundParticipant = iterateOverParticipants(participants, domain, participantRole);
        if (foundParticipant == null) {
            LOGGER_CONFIDENTIAL.debug("Participant [{}:{}] has not been found in the local configuration file", participantRole,
                    domain);
            throw new BusinessException(ParticipantDiscoveryError.PARTICIPANT_NOT_FOUND);
        }
        foundParticipant.setUsefRole(participantRole);

        return foundParticipant;
    }

    /**
     * Iterates over participants to retrieve the one with a given domain name and given role.
     *
     * @param participants - {@link List} of {@link Participant}
     * @param participantDomain - {@link String} Domain name of the participant
     * @param participantRole - {@link USEFRole} Role of the participant
     * @return a {@link Participant} if found in the list, <code>null</code> otherwise.
     */
    private Participant iterateOverParticipants(List<Participant> participants, String participantDomain,
            USEFRole participantRole) {
        for (Participant participant : participants) {
            if (!participantDomain.equals(participant.getDomainName())) {
                continue;
            }
            for (ParticipantRole role : participant.getRoles()) {
                if (participantRole.equals(role.getUsefRole())) {
                    return participant;
                }
            }
        }
        return null;
    }

    /**
     * Gets the USEF version implemented in the given participant domain.
     *
     * @param participantDomain - {@link String} Domain name of the participant
     * @return a {@link String} containing the USEF.
     */
    protected static String getUsefVersion(String participantDomain)  throws BusinessException {
        return getTxtRecord("_usef." + participantDomain + ".");
    }

    /**
     * Gets the USEF endpoint implemented in the given participant domain.
     *
     * @param participantDomain - {@link String} Domain name of the participant
     * @return a {@link String} containing the url.
     */
    protected static String getUsefEndpoint(String participantDomain) throws BusinessException {
        String version = getUsefVersion(participantDomain);

        if(! SUPPORTED_USEF_VERSION.equals(version)) {
                throw new BusinessException(VersionError.VERSION_NOT_SUPPORTED, version);
        }
        return "https://" + participantDomain + "/USEF/" + getUsefVersion(participantDomain) + "/SignedMessage";
    }

    /**
     * Gets the USEF unsealing key implemented of the given participant domain and role.
     *
     * @param participantDomain - {@link String} domain name of the participant
     * @return a {@link String} containing the unsealing key.
     */
    protected static String getPublicUnsealingKey(String participantDomain, USEFRole participantRole) throws BusinessException {
        return getTxtRecord("_" + participantRole.value() + "._usef." + participantDomain + ".");
    }


    /**
     * Checks whether a participant is found in the list of real participants.
     *
     * @param domain - {@link String} containing the participants's domain.
     * @param participantRole - {@link USEFRole} indicating the participant's role.
     * @throws BusinessException if no participant is matching the sender.
     */
    protected static Participant findParticipantInDns(String domain, USEFRole participantRole)
            throws BusinessException {

        Participant participant = new Participant();
        participant.setDomainName(domain);
        participant.setUsefRole(participantRole);
        participant.setSpecVersion(getUsefVersion(domain));
        participant.setUrl(getUsefEndpoint(domain));
        ParticipantRole role = new ParticipantRole(participantRole);
        role.setUrl(getUsefEndpoint(domain));

        String[] keys = getPublicUnsealingKey(domain, participantRole).split(" ");
        for (String singleKey : keys) {
            role.setPublicKeys(Collections.singletonList(StringUtils.removeStart(singleKey, PUBLIC_KEY_PREFIX)));
            participant.setPublicKeys(Collections.singletonList(StringUtils.removeStart(singleKey, PUBLIC_KEY_PREFIX)));
        }

        participant.setRoles(Collections.singletonList(role));
        return participant;
    }

    protected static String getTxtRecord(String name) throws BusinessException {
        Record qr = Record.newRecord(Name.fromConstantString(name), Type.TXT, DClass.IN);
        org.xbill.DNS.Message response;
        try {
            response = resolver.send(org.xbill.DNS.Message.newQuery(qr));
        } catch (IOException e) {
            LOGGER.error("Unable to connect to the DNS", e);
            throw new BusinessException(ParticipantDiscoveryError.DNS_NOT_FOUND);
        }
        Record[] records = response.getSectionArray(Section.ANSWER);
        if (records.length != 1) {
            LOGGER.error("Participant not found in DNS");
            throw new BusinessException(ParticipantDiscoveryError.PARTICIPANT_NOT_FOUND);
        }
        @SuppressWarnings("unchecked")
        List<String> result = ((TXTRecord) records[0]).getStrings();
        return result.get(0);
    }

    private String findLocalParticipantUnsigningPublicKey(String senderDomain, USEFRole senderRole) throws BusinessException {
        String participantDnsFileName = Config.getConfigurationFolder() +
                config.getProperty(ConfigParam.PARTICIPANT_DNS_INFO_FILENAME);
        if (!isFileExists(participantDnsFileName)) {
            participantDnsFileName = AbstractConfig.DOMAIN_CONFIG_FOLDER + File.separator + PARTICIPANTS_YAML;
        }
        List<Participant> participants = participantListBuilder.buildParticipantList(participantDnsFileName);
        checkParticipantsListIsAvailable(participants);

        for (Participant participant : participants) {
            if (!senderDomain.equals(participant.getDomainName())) {
                continue;
            }
            for (ParticipantRole role : participant.getRoles()) {
                if (senderRole.equals(role.getUsefRole())) {
                    return role.getPublicKeys().get(0);
                }
            }
        }
        return null;
    }

    private static boolean isFileExists(String fileName) {
        File f = new File(fileName);
        return f.exists() && !f.isDirectory();
    }

    private void checkParticipantsListIsAvailable(List<Participant> participants) throws BusinessException {
        if (participants == null || participants.isEmpty()) {
            throw new BusinessException(ParticipantDiscoveryError.EMPTY_PARTICIPANT_LIST);
        }
    }

    private void checkSenderDomainAndRoleAvailable(SignedMessage signedMessage) throws BusinessException {
        if (signedMessage == null) {
            return;
        }
        if (signedMessage.getSenderDomain() == null) {
            throw new BusinessException(ParticipantDiscoveryError.SENDER_DOMAIN_NOT_PROVIDED);
        }
        if (signedMessage.getSenderRole() == null) {
            throw new BusinessException(ParticipantDiscoveryError.SENDER_ROLE_NOT_PROVIDED);
        }
    }

    private boolean byPassDNSCheck() {
        return config.getBooleanProperty(ConfigParam.BYPASS_DNS_VERIFICATION);
    }
}
