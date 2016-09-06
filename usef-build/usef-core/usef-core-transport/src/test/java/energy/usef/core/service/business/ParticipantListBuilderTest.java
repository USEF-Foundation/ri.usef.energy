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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import energy.usef.core.data.participant.Participant;
import energy.usef.core.data.participant.ParticipantRole;
import energy.usef.core.data.xml.bean.message.USEFRole;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test class for the {@link ParticipantListBuilder}. It will verify that a list of participant can be built from a YAML
 * configuration file.
 */
@RunWith(PowerMockRunner.class)
public class ParticipantListBuilderTest {

    private static final String PARTICIPANTS_DNS_INFO_TEST_YAML =
            "src/test/resources/energy/usef/core/service/participants_dns_info_test.yaml";
    private ParticipantListBuilder builder;

    @Before
    public void init() throws Exception {
        builder = new ParticipantListBuilder();
    }

    /**
     * Builds a participant list from the file {@link #PARTICIPANTS_DNS_INFO_TEST_YAML}. Each attribute specified in the yaml file
     * is verified.
     */
    @Test
    public void testBuildParticipantList() {
        List<Participant> participants = builder.buildParticipantList(PARTICIPANTS_DNS_INFO_TEST_YAML);
        assertNotNull(participants);
        assertEquals(5, participants.size());

        Participant p1 = participants.get(0);
        assertEquals("example.com", p1.getDomainName());
        assertEquals("2015", p1.getSpecVersion());
        assertEquals("pNUU96U5br6ZFTpyFs18N7wIveBl+rc5gHNYS473RKI=", p1.getPublicKeys().get(0));
        assertEquals(1, p1.getRoles().size());

        ParticipantRole p1r1 = p1.getRoles().get(0);
        assertNotNull(p1r1);
        assertEquals("http://usef.energy/usef.energy_agr/Something", p1r1.getUrl());
        assertEquals(2, p1r1.getPublicKeys().size());
        assertEquals(USEFRole.AGR, p1r1.getUsefRole());

        Participant p2 = participants.get(1);
        assertNotNull(p2);
        assertEquals("usef.energy", p2.getDomainName());
        assertEquals("2015", p2.getSpecVersion());
        assertEquals("pNUU96U5br6ZFTpyFs18N7wIveBl+rc5gHNYS473RKI=", p2.getPublicKeys().get(0));
        assertEquals(1, p2.getRoles().size());

        ParticipantRole p2r1 = p2.getRoles().get(0);
        assertNotNull(p2r1);
        assertEquals("http://usef.energy/usef.energy_dso/Something", p2r1.getUrl());
        assertEquals(2, p2r1.getPublicKeys().size());
        assertEquals(USEFRole.DSO, p2r1.getUsefRole());

        Participant p3 = participants.get(4);
        Assert.assertNotNull(p3);
        assertEquals("2015", p3.getSpecVersion());
        assertEquals("pNUU96U5br6ZFTpyFs18N7wIveBl+rc5gHNYS473RKI=", p2.getPublicKeys().get(0));
        ParticipantRole p3r1 = p3.getRoles().get(0);
        assertEquals("http://usef.energy/usef.energy_mdc/something", p3r1.getUrl());
        assertEquals(USEFRole.MDC, p3r1.getUsefRole());

    }
}
