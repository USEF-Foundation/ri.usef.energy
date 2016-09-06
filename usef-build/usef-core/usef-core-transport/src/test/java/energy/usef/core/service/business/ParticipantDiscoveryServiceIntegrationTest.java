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
import energy.usef.core.data.xml.bean.message.SignedMessage;
import energy.usef.core.data.xml.bean.message.USEFRole;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.modules.junit4.PowerMockRunner;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(PowerMockRunner.class)
public class ParticipantDiscoveryServiceIntegrationTest {
    @Mock
    private ParticipantListBuilder listBuilder;

    @Mock
    private Config config;

    private ParticipantDiscoveryService service;


    @Before
    public void setUp() throws Exception {
        service = new ParticipantDiscoveryService();
        Whitebox.setInternalState(service, "participantListBuilder", listBuilder);
        Whitebox.setInternalState(service, "config", config);
        Mockito.when(config.getBooleanProperty(Matchers.eq(ConfigParam.BYPASS_DNS_VERIFICATION))).thenReturn(Boolean.FALSE);

    }

    @Test
    @Ignore
    public void discoverParticipant() throws Exception {
        Participant participant = service.findParticipantInDns("cro-usef.ict.eu", USEFRole.CRO);

        assertNotNull(participant);
        assertEquals("Domain (cro-usef.ict.eu)", "cro-usef.ict.eu", participant.getDomainName());
        assertEquals("Role (cro-usef.ict.eu)", USEFRole.CRO, participant.getUsefRole());
        assertEquals("Version (cro-usef.ict.eu)", "2015", participant.getSpecVersion());
        assertEquals("Endpoint (cro-usef.ict.eu)", "https://cro-usef.ict.eu/USEF/2015/SignedMessage", participant.getUrl());
        assertEquals("Unsealing key (cro-usef.ict.eu)", "75XTROPS/PalLxxZNdMw5CNPREqKHCldSDWdXJpPjgs=", participant.getPublicKeys().get(0));
    }

    @Test
    @Ignore
    public void findUnsealingPublicKey() throws Exception {
        assertEquals("Unsealing key (cro-usef.ict.eu)", "75XTROPS/PalLxxZNdMw5CNPREqKHCldSDWdXJpPjgs=", service.findUnsealingPublicKey(buildSignedMessage("cro-usef.ict.eu", USEFRole.CRO)));
        assertEquals("Unsealing key (stedin-dso-usef.ict.eu)", "alBPu3iPsYH2ArhrygWzH/PgFjWhCckUVR5Emj8LZ2U=", service.findUnsealingPublicKey(buildSignedMessage("stedin-dso-usef.ict.eu", USEFRole.DSO)));
        assertEquals("Unsealing key (stedin-ndc-usef.ict.eu)", "naHOirRJ3nZ00cDsGJkWI2AAT7s2blbOD38LSc4ypaE=", service.findUnsealingPublicKey(buildSignedMessage("stedin-mdc-usef.ict.eu", USEFRole.MDC)));

    }

    @Test
    @Ignore
    public void getUsefVersion() throws Exception {
        assertEquals("Version (cro-usef.ict.eu)", "2015", service.getUsefVersion("cro-usef.ict.eu"));
        assertEquals("Version (stedin-dso-usef.ict.eu)", "2015", service.getUsefVersion("stedin-dso-usef.ict.eu"));
        assertEquals("Version (stedin-mdc-usef.ict.eu)", "2015", service.getUsefVersion("stedin-mdc-usef.ict.eu"));
    }
    @Test
    @Ignore
    public void getUsefEndpoint() throws Exception {
        assertEquals("Endpoint (cro-usef.ict.eu)", "https://cro-usef.ict.eu/USEF/2015/SignedMessage", service.getUsefEndpoint("cro-usef.ict.eu"));
        assertEquals("Endpoint (stedin-dso-usef.ict.eu)", "https://stedin-dso-usef.ict.eu/USEF/2015/SignedMessage", service.getUsefEndpoint("stedin-dso-usef.ict.eu"));
        assertEquals("Endpoint (stedin-mdc-usef.ict.eu)", "https://stedin-mdc-usef.ict.eu/USEF/2015/SignedMessage", service.getUsefEndpoint("stedin-mdc-usef.ict.eu"));
    }

    @Test
    @Ignore
    public void getPublicUnsealingKey() throws Exception {
        assertEquals("Unsealing Key Entry (cro-usef.ict.eu)", "cs1.75XTROPS/PalLxxZNdMw5CNPREqKHCldSDWdXJpPjgs=", service.getPublicUnsealingKey("cro-usef.ict.eu", USEFRole.CRO));
        assertEquals("Unsealing Key Entry (stedin-dso-usef.ict.eu)", "cs1.alBPu3iPsYH2ArhrygWzH/PgFjWhCckUVR5Emj8LZ2U=", service.getPublicUnsealingKey("stedin-dso-usef.ict.eu", USEFRole.DSO));
        assertEquals("Unsealing Key Entry (stedin-mdc-usef.ict.eu)", "cs1.naHOirRJ3nZ00cDsGJkWI2AAT7s2blbOD38LSc4ypaE=", service.getPublicUnsealingKey("stedin-mdc-usef.ict.eu", USEFRole.MDC));
    }

    private SignedMessage buildSignedMessage(String senderDomain, USEFRole senderRole) {
        SignedMessage message = new SignedMessage();
        message.setSenderDomain(senderDomain);
        message.setSenderRole(senderRole);
        message.setBody("anyB64Body==".getBytes(StandardCharsets.UTF_8));
        return message;
    }


}