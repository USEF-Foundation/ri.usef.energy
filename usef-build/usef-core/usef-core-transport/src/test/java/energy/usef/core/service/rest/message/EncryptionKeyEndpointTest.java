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

package energy.usef.core.service.rest.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import energy.usef.core.endpoint.EncryptionKeyEndpoint;
import energy.usef.core.service.helper.KeystoreHelperService;

import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class for the {@link EncryptionKeyEndpoint} rest enpoint class.
 */
@RunWith(PowerMockRunner.class)
public class EncryptionKeyEndpointTest {

    private static final String SEED = "password";
    private static final String B64_PUBLIC_KEY = "XXl8ePsLJ6UeJbOPOQo/qJA+J1lPBYmOQ5ingBkPbb8=";

    @Mock
    private KeystoreHelperService keystoreHelperService;

    private EncryptionKeyEndpoint encryptionKeyService;

    /**
     * Test initialisation: mocking of the KeystoreHelperService.
     */
    @Before
    public void init() {
        encryptionKeyService = new EncryptionKeyEndpoint();
        Whitebox.setInternalState(encryptionKeyService, "keystoreHelperService", keystoreHelperService);
    }

    /**
     * Tests that the creation of a new key pair with a password returns the correct public key (encoded in Base64 for readability).
     */
    @Test
    public void testCreateNewEncryptionKeyPair() {
        PowerMockito.when(keystoreHelperService.createSecretKey(Matchers.eq(SEED))).thenReturn(Base64.decodeBase64(B64_PUBLIC_KEY));
        Response response = encryptionKeyService.createNewEncryptionKeyPair(SEED);
        assertNotNull(response);
        assertEquals("HTTP response code mismatch.", Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Public key mismatch.", B64_PUBLIC_KEY, response.getEntity().toString());
    }

}
