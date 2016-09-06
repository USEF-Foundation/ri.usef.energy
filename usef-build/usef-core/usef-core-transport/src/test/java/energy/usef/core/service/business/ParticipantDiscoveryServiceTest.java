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

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.participant.Participant;
import energy.usef.core.data.participant.ParticipantRole;
import energy.usef.core.data.participant.ParticipantType;
import energy.usef.core.data.xml.bean.message.*;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.service.business.error.ParticipantDiscoveryError;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Name;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.Section;
import org.xbill.DNS.TXTRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * This test class verifies that the {@link ParticipantDiscoveryService} class works properly.
 */
@RunWith(PowerMockRunner.class)
public class ParticipantDiscoveryServiceTest {

    private static final String MISSING_DOMAIN = "not-existing.usef-example.com";
    private static final String SENDER_DOMAIN = "usef-example.com";
    private static final String UNSEALING_KEY = "cs1.nHKbxKyPW3IPVecs/ycGe+3j3K91RaRROr0EhdSZKNo=";

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantDiscoveryServiceTest.class);
    public static final String USEF_EXAMPLE_COM_ENDPOINT = "http://usef-example.com/USEF/2015/SignedMessage";

    @Mock
    private ParticipantListBuilder listBuilder;

    @Mock
    private Resolver resolver;

    private ParticipantDiscoveryService service;

    @Mock
    private Config config;

    @Before
    public void init() throws IOException {
        service = new ParticipantDiscoveryService();
        Whitebox.setInternalState(service, "participantListBuilder", listBuilder);
        Whitebox.setInternalState(service, "config", config);
        Whitebox.setInternalState(service, "resolver", resolver);

        Mockito.when(config.getBooleanProperty(ConfigParam.BYPASS_DNS_VERIFICATION)).thenReturn(true);

        Mockito.when(listBuilder.buildParticipantList(Matchers.anyString()))
                .thenReturn(buildParticipantList());
    }

    /**
     * This test tries to send a message with a participant which is not present in the YAML configuration file (DNS bypass being
     * activated in the config.properties file). In case the the {@link ParticipantDiscoveryService} does not throw an exception,
     * the test will fail.
     *
     * The test will succeed if the {@link ParticipantDiscoveryError#PARTICIPANT_NOT_FOUND} is thrown in the
     * {@link BusinessException}
     */
    @Test
    public void testDNSVerificationFailsOnMissingParticipant() {
        try {
            service.discoverParticipant(buildIncomingMessage(MISSING_DOMAIN, USEFRole.AGR),
                    ParticipantType.SENDER);
            fail("Test should have resulted in a BusinessException.");
        } catch (BusinessException e) {
            assertEquals(ParticipantDiscoveryError.PARTICIPANT_NOT_FOUND, e.getBusinessError());
            LOGGER.trace("Correctly caught the exception during the execution of the test.", e);
        }
    }

    /**
     * This test sends a message with a participant defined in the local YAML configuration file (DNS bypass being actived in the
     * config.properties file). Test will succeed if no {@link BusinessException} is thrown and fail otherwise.
     */
    @Test
    public void testDNSVerificationSucceeds() {
        Mockito.when(config.getBooleanProperty(Matchers.eq(ConfigParam.BYPASS_DNS_VERIFICATION))).thenReturn(Boolean.TRUE);
        try {
            Participant participant = service.discoverParticipant(buildIncomingMessage(SENDER_DOMAIN, USEFRole.AGR),
                    ParticipantType.SENDER);
            assertNotNull(participant);
            assertEquals("Domain", "usef-example.com", participant.getDomainName());
            assertEquals("Role", USEFRole.AGR, participant.getUsefRole());
            assertEquals("Version", "2015", participant.getSpecVersion());
            assertEquals("Endpoint", USEF_EXAMPLE_COM_ENDPOINT, participant.getUrl());
            assertEquals("Unsealing key", UNSEALING_KEY, participant.getPublicKeys().get(0));


            assertEquals(USEFRole.AGR, participant.getRoles().get(0).getUsefRole());
            assertEquals(USEF_EXAMPLE_COM_ENDPOINT, participant.getRoles().get(0).getUrl());



        } catch (BusinessException e) {
            LOGGER.error("Exception has been caught during the execution of the test.", e);
            fail("Test should not have resulted in the following exception: "
                    + e.getBusinessError().getError());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDNSNotAvailableError() throws IOException {
        Mockito.when(config.getBooleanProperty(Matchers.eq(ConfigParam.BYPASS_DNS_VERIFICATION))).thenReturn(Boolean.FALSE);

        Mockito.when(resolver.send(Matchers.any(org.xbill.DNS.Message.class))).thenThrow(IOException.class);

        try {
            service.discoverParticipant(buildIncomingMessage(SENDER_DOMAIN, USEFRole.AGR),
                    ParticipantType.SENDER);
            fail("Test should have resulted in the an exception ");
        } catch (BusinessException e) {
            assertEquals(ParticipantDiscoveryError.DNS_NOT_FOUND.getError(), e.getBusinessError().getError());
        }
    }

    @Test
    public void testFindUnsealingPublicKey() throws BusinessException, IOException {
        org.xbill.DNS.Message responseMessage = new org.xbill.DNS.Message();
        responseMessage.addRecord(new TXTRecord(new Name("@."), 0, 100L, UNSEALING_KEY), Section.ANSWER);
        Mockito.when(resolver.send(Matchers.any(org.xbill.DNS.Message.class))).thenReturn(responseMessage);

        assertEquals("Public key mismatch.", UNSEALING_KEY, service.getPublicUnsealingKey(SENDER_DOMAIN, USEFRole.AGR));
        assertEquals("Public key mismatch.", UNSEALING_KEY, service.getPublicUnsealingKey(SENDER_DOMAIN, USEFRole.BRP));
        assertEquals("Public key mismatch.", UNSEALING_KEY, service.getPublicUnsealingKey(SENDER_DOMAIN, USEFRole.CRO));
        assertEquals("Public key mismatch.", UNSEALING_KEY, service.getPublicUnsealingKey(SENDER_DOMAIN, USEFRole.DSO));
        assertEquals("Public key mismatch.", UNSEALING_KEY, service.getPublicUnsealingKey(SENDER_DOMAIN, USEFRole.MDC));
    }

    @Test
    public void testFindUnsealingPublicKeyFails() throws BusinessException {
        String actualKey = service.findUnsealingPublicKey(buildSignedMessage("wrong.usef.energy", USEFRole.AGR));
        assertNull("Actual key is not null.", actualKey);
    }

    @Test
    public void testGetUsefVersionSucceeds() throws BusinessException, IOException {
        org.xbill.DNS.Message responseMessage = new org.xbill.DNS.Message();
        responseMessage.addRecord(new TXTRecord(new Name("@."), 0, 100L, "2015"), Section.ANSWER);
        Mockito.when(resolver.send(Matchers.any(org.xbill.DNS.Message.class))).thenReturn(responseMessage);

        assertEquals("2015", service.getUsefVersion(SENDER_DOMAIN));
    }

    private Message buildIncomingMessage(String senderDomain, USEFRole senderRole) {
        Message message = new TestMessage();
        MessageMetadata metadata = new MessageMetadata();
        metadata.setSenderDomain(senderDomain);
        metadata.setSenderRole(senderRole);
        message.setMessageMetadata(metadata);
        return message;
    }

    private SignedMessage buildSignedMessage(String senderDomain, USEFRole senderRole) {
        SignedMessage message = new SignedMessage();
        message.setSenderDomain(senderDomain);
        message.setSenderRole(senderRole);
        message.setBody("anyB64Body==".getBytes(StandardCharsets.UTF_8));
        return message;
    }

    private List<Participant> buildParticipantList() {
        Participant participant = new Participant();
        participant.setDomainName(SENDER_DOMAIN);
        participant.setSpecVersion("2015");
        participant.setPublicKeys(Arrays.asList(UNSEALING_KEY, "KEY2"));
        participant.setUrl(USEF_EXAMPLE_COM_ENDPOINT);

        ParticipantRole role = new ParticipantRole(USEFRole.AGR);
        role.setUrl(USEF_EXAMPLE_COM_ENDPOINT);
        role.setPublicKeys(Arrays.asList( UNSEALING_KEY, "KEY2"));

        participant.setRoles(Collections.singletonList(role));
        return Collections.singletonList(participant);
    }
}
