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

package energy.usef.core.data.participant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import energy.usef.core.data.xml.bean.message.USEFRole;

import java.util.ArrayList;

import org.junit.Test;

public class ParticipantTest {

    @Test
    public void testParticipant() {
        Participant p = new Participant();
        ParticipantRole r = new ParticipantRole(USEFRole.CRO);
        r.setUsefRole(USEFRole.AGR);
        r.setPublicKeysFromString(null);
        p.addParticipantRoles();
        p.addParticipantRoles(r);
        assertEquals(USEFRole.AGR, p.getRoles().get(0).getUsefRole());

        p.setRoles(null);
        p.setDomainName("mydomain1");
        assertEquals("mydomain1", p.getDomainName());
        p.setSpecVersion("2015:I");
        assertEquals("2015:I", p.getSpecVersion());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParticipantRole() {
        ParticipantRole r = new ParticipantRole(USEFRole.AGR);
        r.setPublicKeysFromString(null);
        assertNull(r.getPublicKeys());
        r.setUrl("myhost");
        assertEquals("myhost", r.getUrl());
        r.setPublicKeys(new ArrayList<>());
        r.setPublicKeysFromString("cs1.nHKbxKyPW3IPVecs/ycGe+3j3K91RaRROr0EhdSZKNo=");
        r.setPublicKeysFromString("no-cs1");
    }

    @Test
    public void testParticipantType() {
        ParticipantType t1 = ParticipantType.SENDER;
        assertEquals("SENDER", t1.name());
        ParticipantType t2 = ParticipantType.RECIPIENT;
        assertEquals("RECIPIENT", t2.name());
    }

    @Test
    public void testPublicKey() {
        ParticipantRole r = new ParticipantRole(USEFRole.AGR);
        r.setUsefRole(USEFRole.AGR);
        r.setPublicKeysFromString("cs1.thisisaverylongstring");
        assertTrue(!r.getPublicKeys().isEmpty());
        assertEquals("thisisaverylongstrinAAAAAAAAAAAAAAAAAAAAAAA=", r.getPublicKeys().iterator().next());

    }

}
