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

import energy.usef.core.data.xml.bean.message.USEFRole;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

/**
 * Role information on a specific {@link Participant}.
 */
public class ParticipantRole {

    private static final String CS1_PREFIX = "cs1.";
    private static final int FIRST_KEY_INDEX = 0;
    private static final int SECOND_KEY_INDEX = 32;
    private static final int SECOND_KEY_SIZE = 32;

    private List<String> publicKeys;
    private String url;
    private USEFRole usefRole;

    /**
     * Contruct a {@link ParticipantRole} for a specific {@link USEFRole}.
     *
     * @param role
     */
    public ParticipantRole(USEFRole role) {
        this.usefRole = role;
    }

    public void setPublicKeysFromString(String concatenatedKeys) {
        if (concatenatedKeys == null) {
            return;
        }
        if (!concatenatedKeys.startsWith(CS1_PREFIX)) {
            throw new IllegalArgumentException("Keys does not have an recognized prefix.");
        }
        String actualConcatenatedKey = concatenatedKeys.substring(CS1_PREFIX.length());
        byte[] decodedKey = Base64.decodeBase64(actualConcatenatedKey);
        byte[] firstKey = Arrays.copyOfRange(decodedKey, FIRST_KEY_INDEX, SECOND_KEY_INDEX);
        setPublicKeys(Arrays.asList(
                Base64.encodeBase64String(firstKey),
                Base64.encodeBase64String(new byte[SECOND_KEY_SIZE])));
    }

    public List<String> getPublicKeys() {
        return publicKeys;
    }

    public void setPublicKeys(List<String> publicKeys) {
        this.publicKeys = publicKeys;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public USEFRole getUsefRole() {
        return usefRole;
    }

    public void setUsefRole(USEFRole usefRole) {
        this.usefRole = usefRole;
    }

}
