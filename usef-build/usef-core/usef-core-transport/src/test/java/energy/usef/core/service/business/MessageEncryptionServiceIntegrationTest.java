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
import energy.usef.core.data.xml.bean.message.Message;
import energy.usef.core.data.xml.bean.message.SignedMessage;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.service.business.error.MessageEncryptionError;
import energy.usef.core.service.helper.KeystoreHelperService;
import energy.usef.core.util.encryption.SodiumStub;
import jnr.ffi.byref.LongLongByReference;
import org.abstractj.kalium.NaCl;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(PowerMockRunner.class)
@PrepareForTest({KeystoreHelperService.class, Config.class})
public class MessageEncryptionServiceIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageEncryptionServiceIntegrationTest.class);

    private static final String SEED = "us";

    private static final int PUBLIC_KEY_SIZE = 32;
    private static final int PRIVATE_KEY_SIZE = 64;

    private static final String JCEKS = "JCEKS";
    private static final String ALGORITHM = "NaCl";

    private static final String JL_PRIVATE = "4ffda13c11d61d2b9568e54bec06ea59368e84874883087645e64e5e9653422e667827d16bab821940689382a580f6b2c4d27cf60b92cce77d471087ff50d83e";
    private static final String JL_PUBLIC = "Zngn0WurghlAaJOCpYD2ssTSfPYLksznfUcQh/9Q2D4=";



    byte[] publicKey = new byte[PUBLIC_KEY_SIZE];
    byte[] privateKey = new byte[PRIVATE_KEY_SIZE];


    private static final String HELLO_MESSAGE = "<TestMessage>Hello</TestMessage>";

    private MessageEncryptionService service;



    @Mock
    private KeystoreHelperService keystoreHelperService;

    @Mock
    private Config config;

    @Rule
    public TestName name = new TestName();

    @Before
    public void initTest() throws UnsupportedEncodingException {
        Whitebox.setInternalState(keystoreHelperService, "config", config);
        service = new MessageEncryptionService();
        Whitebox.setInternalState(service, "keystoreHelperService", keystoreHelperService);

        energy.usef.core.util.encryption.NaCl.sodium().crypto_sign_ed25519_seed_keypair(publicKey, privateKey, SEED.getBytes(UTF_8));
        SecretKey secretKey = new SecretKeySpec(privateKey, ALGORITHM);

        LOGGER.info("Public Key: [{}]", new String(publicKey, StandardCharsets.UTF_8));
        LOGGER.info("Private Key: [{}]", new String(privateKey, StandardCharsets.UTF_8));
        LOGGER.info("Secret Key Algorithm: [{}]", secretKey.getAlgorithm());
        LOGGER.info("Secret Key Format: [{}]", secretKey.getFormat());
        LOGGER.info("Secret Key Encoded: [{}]", new String(secretKey.getEncoded(), StandardCharsets.UTF_8));

        LOGGER.info("### Executing test: {}", name.getMethodName());

        Mockito.when(keystoreHelperService.loadSecretKey()).thenReturn(Arrays.copyOf(privateKey, privateKey.length));
    }

    @Test
    public void sealMessage() throws Exception {
        String message = HELLO_MESSAGE;
        String publicKeyB64 = Base64.encodeBase64String(publicKey);
        try {
            byte[] encodedMessage = service.sealMessage(message);
            assertNotNull(encodedMessage);

            String decodedMessage = service.verifyMessage(encodedMessage, publicKeyB64);

            assertEquals("DecodedMessage", HELLO_MESSAGE, decodedMessage);
        } catch (BusinessException e) {
            LOGGER.error("Exception caught during the test.", e);
            fail(e.getBusinessError().getError());
        }
    }


}
