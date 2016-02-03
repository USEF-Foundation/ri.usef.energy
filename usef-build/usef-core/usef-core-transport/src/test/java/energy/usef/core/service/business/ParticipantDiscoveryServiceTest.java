/*
 * Copyright 2015 USEF Foundation
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.participant.Participant;
import energy.usef.core.data.participant.ParticipantRole;
import energy.usef.core.data.participant.ParticipantType;
import energy.usef.core.data.xml.bean.message.Message;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.SignedMessage;
import energy.usef.core.data.xml.bean.message.TestMessage;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.service.business.error.ParticipantDiscoveryError;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
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

/**
 * This test class verifies that the {@link ParticipantDiscoveryService} class works properly.
 */
@RunWith(PowerMockRunner.class)
public class ParticipantDiscoveryServiceTest {

    private static final String MISSING_DOMAIN = "not-existing.usef-example.com";
    private static final String SENDER_DOMAIN = "usef-example.com";
    private static final String agrKey = "nHKbxKyPW3IPVecs/ycGe+3j3K91RaRROr0EhdSZKNo=";
    private static final String dsoKey = "82IDHPcYSl4s0S/8d9+4umtPlN+m9ouPZN+T8hTr0ZQ=";

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantDiscoveryServiceTest.class);

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

        org.xbill.DNS.Message responseMessage = new org.xbill.DNS.Message();
        responseMessage.addRecord(new TXTRecord(new Name("@."), 0, 100L, "2014:I"), Section.ANSWER);

        Mockito.when(resolver.send(Matchers.any(org.xbill.DNS.Message.class))).thenReturn(responseMessage);
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
            assertEquals("usef-example.com", participant.getDomainName());
            assertEquals(USEFRole.AGR, participant.getRoles().get(0).getUsefRole());
            assertEquals("http://usef-example.com", participant.getRoles().get(0).getUrl());
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
    public void testFindUnsealingPublicKeySucceeds() throws BusinessException {
        String actualKey = service.findUnsealingPublicKey(buildSignedMessage(SENDER_DOMAIN, USEFRole.AGR));
        assertEquals("Public key mismatch.", agrKey, actualKey);
    }

    @Test
    public void testFindUnsealingPublicKeyFails() throws BusinessException {
        String actualKey = service.findUnsealingPublicKey(buildSignedMessage("wrong.usef.energy", USEFRole.AGR));
        assertNull("Actual key is not null.", actualKey);
    }

    @Test
    public void testGetUsefKeySucceeds() throws BusinessException, IOException {

        // This is actually an integration test
        String address = service.getUsefText(SENDER_DOMAIN, null);
        assertEquals("2014:I", address);
    }

    @Ignore
    // No longer works because all roles are now in DNS
    public void testGetUsefTextFails() throws BusinessException {
        String text = service.getUsefText(SENDER_DOMAIN, USEFRole.CRO);
        assertNull(text);
    }

    // should be mocked or something (dependent on external configuration)
    @Ignore
    public void testGetUsefKey1Succeeds() throws BusinessException {
        // This is actually an integration test
        String key = service.getUsefText(SENDER_DOMAIN, USEFRole.AGR);

        assertEquals("cs1." + agrKey, key);
    }

    // should be mocked or something (dependent on external configuration)
    @Ignore
    public void testGetUsefKey2Succeeds() throws BusinessException {
        // This is actually an integration test
        String key = service.getUsefText(SENDER_DOMAIN, USEFRole.DSO);

        assertEquals("cs1." + dsoKey, key);
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
        participant.setSpecVersion("2014:I");

        ParticipantRole role = new ParticipantRole(USEFRole.AGR);
        role.setUrl("http://" + SENDER_DOMAIN);
        role.setPublicKeys(Arrays.asList("cs1." + agrKey, "KEY2"));

        participant.setRoles(Collections.singletonList(role));
        return Collections.singletonList(participant);
    }
}
