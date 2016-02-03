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

import static energy.usef.core.service.business.error.MessageEncryptionError.EXPECTED_256BITS_PUBLIC_KEY;
import static energy.usef.core.service.business.error.MessageEncryptionError.EXPECTED_BASE64_PUBLIC_KEY;
import static energy.usef.core.service.business.error.MessageEncryptionError.EXPECTED_BASE64_SEALED_MESSAGE;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import energy.usef.core.config.Config;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.service.helper.KeystoreHelperService;
import energy.usef.core.util.encryption.NaCl;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
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

/**
 * Class unit-testing the {@link MessageEncryptionService} business class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ KeystoreHelperService.class, Config.class })
public class MessageEncryptionServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("CONFIDENTIAL");

    private static final String HELLO_MESSAGE = "<TestMessage>Hello</TestMessage>";
    private static final String SIGNED_HELLO_MESSAGE = "�q�%l�V4z��ڭ�풝�-V��5��@��U$�F��ɶo?�M��h�$�u˂*�ySPM<TestMessage>Hello</TestMessage>";
    private static final byte[] B64_SEALED_HELLO_MESSAGE = Base64.encodeBase64(SIGNED_HELLO_MESSAGE
            .getBytes(StandardCharsets.UTF_8));

    private static final String TG_MESSAGE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Prognosis Type=\"A-Plan\" PTU-Duration=\"PT15M\" Period=\"2015-04-07\" TimeZone=\"Europe/Amsterdam\" Sequence=\"20150407115758730\"><MessageMetadata SenderDomain=\"agr.usef-hhw.net\" SenderRole=\"AGR\" RecipientDomain=\"brp.usef-hhw.net\" RecipientRole=\"BRP\" TimeStamp=\"2015-04-07T11:57:58.732\" MessageID=\"2191333e-4ff5-411a-8a30-ef5b35595d7d\" ConversationID=\"4ea7f432-2cc3-489a-84af-413aecc8c86e\" Precedence=\"Transactional\"/><PTU Power=\"0\" Start=\"50\" Duration=\"1\"/><PTU Power=\"0\" Start=\"51\" Duration=\"1\"/><PTU Power=\"0\" Start=\"52\" Duration=\"1\"/><PTU Power=\"0\" Start=\"53\" Duration=\"1\"/><PTU Power=\"0\" Start=\"54\" Duration=\"1\"/><PTU Power=\"0\" Start=\"55\" Duration=\"1\"/><PTU Power=\"0\" Start=\"56\" Duration=\"1\"/><PTU Power=\"0\" Start=\"57\" Duration=\"1\"/><PTU Power=\"0\" Start=\"58\" Duration=\"1\"/><PTU Power=\"0\" Start=\"59\" Duration=\"1\"/><PTU Power=\"0\" Start=\"60\" Duration=\"1\"/><PTU Power=\"0\" Start=\"61\" Duration=\"1\"/><PTU Power=\"0\" Start=\"62\" Duration=\"1\"/><PTU Power=\"0\" Start=\"63\" Duration=\"1\"/><PTU Power=\"0\" Start=\"64\" Duration=\"1\"/><PTU Power=\"0\" Start=\"65\" Duration=\"1\"/><PTU Power=\"0\" Start=\"66\" Duration=\"1\"/><PTU Power=\"0\" Start=\"67\" Duration=\"1\"/><PTU Power=\"0\" Start=\"68\" Duration=\"1\"/><PTU Power=\"0\" Start=\"69\" Duration=\"1\"/><PTU Power=\"0\" Start=\"70\" Duration=\"1\"/><PTU Power=\"0\" Start=\"71\" Duration=\"1\"/><PTU Power=\"0\" Start=\"72\" Duration=\"1\"/><PTU Power=\"0\" Start=\"73\" Duration=\"1\"/><PTU Power=\"0\" Start=\"74\" Duration=\"1\"/><PTU Power=\"0\" Start=\"75\" Duration=\"1\"/><PTU Power=\"0\" Start=\"76\" Duration=\"1\"/><PTU Power=\"0\" Start=\"77\" Duration=\"1\"/><PTU Power=\"0\" Start=\"78\" Duration=\"1\"/><PTU Power=\"0\" Start=\"79\" Duration=\"1\"/><PTU Power=\"0\" Start=\"80\" Duration=\"1\"/><PTU Power=\"0\" Start=\"81\" Duration=\"1\"/><PTU Power=\"0\" Start=\"82\" Duration=\"1\"/><PTU Power=\"0\" Start=\"83\" Duration=\"1\"/><PTU Power=\"0\" Start=\"84\" Duration=\"1\"/><PTU Power=\"0\" Start=\"85\" Duration=\"1\"/><PTU Power=\"0\" Start=\"86\" Duration=\"1\"/><PTU Power=\"0\" Start=\"87\" Duration=\"1\"/><PTU Power=\"0\" Start=\"88\" Duration=\"1\"/><PTU Power=\"0\" Start=\"89\" Duration=\"1\"/><PTU Power=\"0\" Start=\"90\" Duration=\"1\"/><PTU Power=\"0\" Start=\"91\" Duration=\"1\"/><PTU Power=\"0\" Start=\"92\" Duration=\"1\"/><PTU Power=\"0\" Start=\"93\" Duration=\"1\"/><PTU Power=\"0\" Start=\"94\" Duration=\"1\"/><PTU Power=\"0\" Start=\"95\" Duration=\"1\"/><PTU Power=\"0\" Start=\"96\" Duration=\"1\"/></Prognosis>";
    private static final String SIGNED_TG_MESSAGE = "cG0vM1hqVHFWRnJmSjFSMWFuSFl2V29yblpMRE9mS0NOZk1RR2Y1bndOU2Z5cUt5di8wZi9BRFhhc0J4U0ZQMnpHek5qVUswcE1Ya1l4VkxBWjhDQVR3L2VHMXNJSFpsY25OcGIyNDlJakV1TUNJZ1pXNWpiMlJwYm1jOUlsVlVSaTA0SWlCemRHRnVaR0ZzYjI1bFBTSjVaWE1pUHo0OFVISnZaMjV2YzJseklGUjVjR1U5SWtFdFVHeGhiaUlnVUZSVkxVUjFjbUYwYVc5dVBTSlFWREUxVFNJZ1VHVnlhVzlrUFNJeU1ERTFMVEEwTFRBM0lpQlVhVzFsV205dVpUMGlSWFZ5YjNCbEwwRnRjM1JsY21SaGJTSWdVMlZ4ZFdWdVkyVTlJakl3TVRVd05EQTNNVEUxTnpVNE56TXdJajQ4VFdWemMyRm5aVTFsZEdGa1lYUmhJRk5sYm1SbGNrUnZiV0ZwYmowaVlXZHlMblZ6WldZdGFHaDNMbTVsZENJZ1UyVnVaR1Z5VW05c1pUMGlRVWRTSWlCU1pXTnBjR2xsYm5SRWIyMWhhVzQ5SW1KeWNDNTFjMlZtTFdob2R5NXVaWFFpSUZKbFkybHdhV1Z1ZEZKdmJHVTlJa0pTVUNJZ1ZHbHRaVk4wWVcxd1BTSXlNREUxTFRBMExUQTNWREV4T2pVM09qVTRMamN6TWlJZ1RXVnpjMkZuWlVsRVBTSXlNVGt4TXpNelpTMDBabVkxTFRReE1XRXRPR0V6TUMxbFpqVmlNelUxT1RWa04yUWlJRU52Ym5abGNuTmhkR2x2YmtsRVBTSTBaV0UzWmpRek1pMHlZMk16TFRRNE9XRXRPRFJoWmkwME1UTmhaV05qT0dNNE5tVWlJRkJ5WldObFpHVnVZMlU5SWxSeVlXNXpZV04wYVc5dVlXd2lMejQ4VUZSVklGQnZkMlZ5UFNJd0lpQlRkR0Z5ZEQwaU5UQWlJRVIxY21GMGFXOXVQU0l4SWk4K1BGQlVWU0JRYjNkbGNqMGlNQ0lnVTNSaGNuUTlJalV4SWlCRWRYSmhkR2x2YmowaU1TSXZQanhRVkZVZ1VHOTNaWEk5SWpBaUlGTjBZWEowUFNJMU1pSWdSSFZ5WVhScGIyNDlJakVpTHo0OFVGUlZJRkJ2ZDJWeVBTSXdJaUJUZEdGeWREMGlOVE1pSUVSMWNtRjBhVzl1UFNJeElpOCtQRkJVVlNCUWIzZGxjajBpTUNJZ1UzUmhjblE5SWpVMElpQkVkWEpoZEdsdmJqMGlNU0l2UGp4UVZGVWdVRzkzWlhJOUlqQWlJRk4wWVhKMFBTSTFOU0lnUkhWeVlYUnBiMjQ5SWpFaUx6NDhVRlJWSUZCdmQyVnlQU0l3SWlCVGRHRnlkRDBpTlRZaUlFUjFjbUYwYVc5dVBTSXhJaTgrUEZCVVZTQlFiM2RsY2owaU1DSWdVM1JoY25ROUlqVTNJaUJFZFhKaGRHbHZiajBpTVNJdlBqeFFWRlVnVUc5M1pYSTlJakFpSUZOMFlYSjBQU0kxT0NJZ1JIVnlZWFJwYjI0OUlqRWlMejQ4VUZSVklGQnZkMlZ5UFNJd0lpQlRkR0Z5ZEQwaU5Ua2lJRVIxY21GMGFXOXVQU0l4SWk4K1BGQlVWU0JRYjNkbGNqMGlNQ0lnVTNSaGNuUTlJall3SWlCRWRYSmhkR2x2YmowaU1TSXZQanhRVkZVZ1VHOTNaWEk5SWpBaUlGTjBZWEowUFNJMk1TSWdSSFZ5WVhScGIyNDlJakVpTHo0OFVGUlZJRkJ2ZDJWeVBTSXdJaUJUZEdGeWREMGlOaklpSUVSMWNtRjBhVzl1UFNJeElpOCtQRkJVVlNCUWIzZGxjajBpTUNJZ1UzUmhjblE5SWpZeklpQkVkWEpoZEdsdmJqMGlNU0l2UGp4UVZGVWdVRzkzWlhJOUlqQWlJRk4wWVhKMFBTSTJOQ0lnUkhWeVlYUnBiMjQ5SWpFaUx6NDhVRlJWSUZCdmQyVnlQU0l3SWlCVGRHRnlkRDBpTmpVaUlFUjFjbUYwYVc5dVBTSXhJaTgrUEZCVVZTQlFiM2RsY2owaU1DSWdVM1JoY25ROUlqWTJJaUJFZFhKaGRHbHZiajBpTVNJdlBqeFFWRlVnVUc5M1pYSTlJakFpSUZOMFlYSjBQU0kyTnlJZ1JIVnlZWFJwYjI0OUlqRWlMejQ4VUZSVklGQnZkMlZ5UFNJd0lpQlRkR0Z5ZEQwaU5qZ2lJRVIxY21GMGFXOXVQU0l4SWk4K1BGQlVWU0JRYjNkbGNqMGlNQ0lnVTNSaGNuUTlJalk1SWlCRWRYSmhkR2x2YmowaU1TSXZQanhRVkZVZ1VHOTNaWEk5SWpBaUlGTjBZWEowUFNJM01DSWdSSFZ5WVhScGIyNDlJakVpTHo0OFVGUlZJRkJ2ZDJWeVBTSXdJaUJUZEdGeWREMGlOekVpSUVSMWNtRjBhVzl1UFNJeElpOCtQRkJVVlNCUWIzZGxjajBpTUNJZ1UzUmhjblE5SWpjeUlpQkVkWEpoZEdsdmJqMGlNU0l2UGp4UVZGVWdVRzkzWlhJOUlqQWlJRk4wWVhKMFBTSTNNeUlnUkhWeVlYUnBiMjQ5SWpFaUx6NDhVRlJWSUZCdmQyVnlQU0l3SWlCVGRHRnlkRDBpTnpRaUlFUjFjbUYwYVc5dVBTSXhJaTgrUEZCVVZTQlFiM2RsY2owaU1DSWdVM1JoY25ROUlqYzFJaUJFZFhKaGRHbHZiajBpTVNJdlBqeFFWRlVnVUc5M1pYSTlJakFpSUZOMFlYSjBQU0kzTmlJZ1JIVnlZWFJwYjI0OUlqRWlMejQ4VUZSVklGQnZkMlZ5UFNJd0lpQlRkR0Z5ZEQwaU56Y2lJRVIxY21GMGFXOXVQU0l4SWk4K1BGQlVWU0JRYjNkbGNqMGlNQ0lnVTNSaGNuUTlJamM0SWlCRWRYSmhkR2x2YmowaU1TSXZQanhRVkZVZ1VHOTNaWEk5SWpBaUlGTjBZWEowUFNJM09TSWdSSFZ5WVhScGIyNDlJakVpTHo0OFVGUlZJRkJ2ZDJWeVBTSXdJaUJUZEdGeWREMGlPREFpSUVSMWNtRjBhVzl1UFNJeElpOCtQRkJVVlNCUWIzZGxjajBpTUNJZ1UzUmhjblE5SWpneElpQkVkWEpoZEdsdmJqMGlNU0l2UGp4UVZGVWdVRzkzWlhJOUlqQWlJRk4wWVhKMFBTSTRNaUlnUkhWeVlYUnBiMjQ5SWpFaUx6NDhVRlJWSUZCdmQyVnlQU0l3SWlCVGRHRnlkRDBpT0RNaUlFUjFjbUYwYVc5dVBTSXhJaTgrUEZCVVZTQlFiM2RsY2owaU1DSWdVM1JoY25ROUlqZzBJaUJFZFhKaGRHbHZiajBpTVNJdlBqeFFWRlVnVUc5M1pYSTlJakFpSUZOMFlYSjBQU0k0TlNJZ1JIVnlZWFJwYjI0OUlqRWlMejQ4VUZSVklGQnZkMlZ5UFNJd0lpQlRkR0Z5ZEQwaU9EWWlJRVIxY21GMGFXOXVQU0l4SWk4K1BGQlVWU0JRYjNkbGNqMGlNQ0lnVTNSaGNuUTlJamczSWlCRWRYSmhkR2x2YmowaU1TSXZQanhRVkZVZ1VHOTNaWEk5SWpBaUlGTjBZWEowUFNJNE9DSWdSSFZ5WVhScGIyNDlJakVpTHo0OFVGUlZJRkJ2ZDJWeVBTSXdJaUJUZEdGeWREMGlPRGtpSUVSMWNtRjBhVzl1UFNJeElpOCtQRkJVVlNCUWIzZGxjajBpTUNJZ1UzUmhjblE5SWprd0lpQkVkWEpoZEdsdmJqMGlNU0l2UGp4UVZGVWdVRzkzWlhJOUlqQWlJRk4wWVhKMFBTSTVNU0lnUkhWeVlYUnBiMjQ5SWpFaUx6NDhVRlJWSUZCdmQyVnlQU0l3SWlCVGRHRnlkRDBpT1RJaUlFUjFjbUYwYVc5dVBTSXhJaTgrUEZCVVZTQlFiM2RsY2owaU1DSWdVM1JoY25ROUlqa3pJaUJFZFhKaGRHbHZiajBpTVNJdlBqeFFWRlVnVUc5M1pYSTlJakFpSUZOMFlYSjBQU0k1TkNJZ1JIVnlZWFJwYjI0OUlqRWlMejQ4VUZSVklGQnZkMlZ5UFNJd0lpQlRkR0Z5ZEQwaU9UVWlJRVIxY21GMGFXOXVQU0l4SWk4K1BGQlVWU0JRYjNkbGNqMGlNQ0lnVTNSaGNuUTlJamsySWlCRWRYSmhkR2x2YmowaU1TSXZQand2VUhKdloyNXZjMmx6UGc9PQ==";
    private static final byte[] B64_SEALED_TG_MESSAGE = Base64.decodeBase64(SIGNED_TG_MESSAGE
            .getBytes(StandardCharsets.UTF_8));
    private static final byte[] TG_PUBLIC_KEY = Base64.decodeBase64("jP7MfwL4NjlBD0BPrk7+TvImaJTQK24ZjivzJzeOUX4=".getBytes());

    private final byte[] seed = "IhaveA32BytesPrivateKey!And8Char".getBytes(StandardCharsets.UTF_8);
    private byte[] publicKey = new byte[32];
    private byte[] privateKey = new byte[64];

    private MessageEncryptionService messageEncryptionService;

    @Mock
    private KeystoreHelperService keystoreHelperService;

    @Mock
    private Config config;

    @Rule
    public TestName name = new TestName();

    @Before
    public void initTest() throws UnsupportedEncodingException {
        Whitebox.setInternalState(keystoreHelperService, "config", config);
        messageEncryptionService = new MessageEncryptionService();
        Whitebox.setInternalState(messageEncryptionService, "keystoreHelperService", keystoreHelperService);

        LOGGER.info("### Executing test: {}", name.getMethodName());
        // generate a key pair
        NaCl.sodium().crypto_sign_ed25519_seed_keypair(publicKey, privateKey, seed);
        LOGGER.debug("UTF-8 Seed: {}, length={}", new String(seed), seed.length);
        LOGGER.debug("B64 Private key: {}", Base64.encodeBase64(privateKey));
        LOGGER.debug("B64 Public key: {}", Base64.encodeBase64(publicKey));
        Mockito.when(keystoreHelperService.loadSecretKey()).thenReturn(Arrays.copyOf(privateKey, privateKey.length));
    }

    @Test
    public void testSealingOfTheMessageSucceeds() throws UnsupportedEncodingException {
        String message = HELLO_MESSAGE;
        try {
            byte[] cipher = messageEncryptionService.sealMessage(message);

            assertNotNull(cipher);

            String signedContent = new String(Base64.decodeBase64(cipher), StandardCharsets.UTF_8);
            LOGGER.info("B64 Sealed message: {}", signedContent);

            assertEquals("Cipher mismatch.", SIGNED_HELLO_MESSAGE, signedContent);
        } catch (BusinessException e) {
            LOGGER.error("Exception caught during the test.", e);
            fail(e.getBusinessError().getError());
        }

    }

    @Test
    public void testNullMessageGivesEmptySeal() throws BusinessException {
        byte[] result = messageEncryptionService.sealMessage(null);
        assertEquals("Excepted empty sealed output.", 0, result.length);
    }

    /*
     * Tests for unsealing message
     */
    @Test
    public void testUnsealingOfTheMessageSucceeds() throws BusinessException {
        String b64PublicKey = Base64.encodeBase64String(publicKey);
        String expectedUnsealedMessage = HELLO_MESSAGE;
        try {
            byte[] sealedMessage = messageEncryptionService.sealMessage(HELLO_MESSAGE);
            String unsealedMessage = messageEncryptionService.verifyMessage(
                    sealedMessage, b64PublicKey);
            assertEquals(expectedUnsealedMessage, unsealedMessage);
        } catch (BusinessException e) {
            LOGGER.error("Exception caught during the execution of the test.", e);
            fail(e.getBusinessError().getError());
        }
    }

    /*
     * Tests for unsealing message
     */
    @Test
    public void testUnsealingTestingGroundMessage() throws BusinessException {
        String b64PublicKey = Base64.encodeBase64String(TG_PUBLIC_KEY);
        String expectedUnsealedMessage = TG_MESSAGE;
        try {
            String unsealedMessage = messageEncryptionService.verifyMessage(B64_SEALED_TG_MESSAGE, b64PublicKey);
            assertEquals(expectedUnsealedMessage, unsealedMessage);
        } catch (BusinessException e) {
            LOGGER.error("Exception caught during the execution of the test.", e);
            fail(e.getBusinessError().getError());
        }
    }

    @Test(expected = NullPointerException.class)
    public void testPublicKeyCannotBeNullForUnsealing() throws BusinessException {
        messageEncryptionService.verifyMessage(B64_SEALED_HELLO_MESSAGE, null);
    }

    @Test
    public void testUnsealingAcceptsOnly256bitsPublicKey() {
        String b64PublicKey = encodeBase64String("NotLongEnoughPublicKey".getBytes());
        try {
            messageEncryptionService.verifyMessage(B64_SEALED_HELLO_MESSAGE, b64PublicKey);
            fail("Public key must be 256 bits long. Excepted Business Exception.");
        } catch (BusinessException e) {
            assertEquals("Business Error mismatch.", EXPECTED_256BITS_PUBLIC_KEY, e.getBusinessError());
            LOGGER.trace("Correctly caught the exception during the execution of the test.", e);
        }
    }

    @Test
    public void testUnsealingAcceptsOnlyBase64PublicKey() {
        String publicKeyString = new String(publicKey); // not base 64
        try {
            messageEncryptionService.verifyMessage(B64_SEALED_HELLO_MESSAGE, publicKeyString);
            fail("Public key is not encoded in Base64. Expected Business Exception.");
        } catch (BusinessException e) {
            assertEquals(EXPECTED_BASE64_PUBLIC_KEY, e.getBusinessError());
            LOGGER.trace("Correctly caught the exception during the execution of the test.", e);
        }
    }

    @Test
    public void testUnsealingRequiresBase64InputMessage() {
        try {
            messageEncryptionService.verifyMessage(HELLO_MESSAGE.getBytes(StandardCharsets.UTF_8),
                    Base64.encodeBase64String(publicKey));
            fail("Input message for unsealing must be in base64. Excepted business exception.");
        } catch (BusinessException e) {
            assertEquals(EXPECTED_BASE64_SEALED_MESSAGE, e.getBusinessError());
            LOGGER.trace("Correctly caught the exception during the execution of the test.", e);
        }
    }

    @Test
    public void testNullSealGivesNullMessage() throws BusinessException {
        String result = messageEncryptionService.verifyMessage(null, encodeBase64String(publicKey));
        assertNull("Excepted null message from unsealing.", result);
    }
}
