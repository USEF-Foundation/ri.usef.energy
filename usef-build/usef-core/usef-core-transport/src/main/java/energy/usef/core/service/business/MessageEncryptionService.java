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

import energy.usef.core.exception.BusinessException;
import energy.usef.core.service.business.error.MessageEncryptionError;
import energy.usef.core.service.helper.KeystoreHelperService;
import energy.usef.core.util.encryption.NaCl;
import jnr.ffi.byref.LongLongByReference;
import org.abstractj.kalium.NaCl.Sodium;
import org.apache.commons.codec.binary.Base64;

import javax.ejb.Stateless;
import javax.inject.Inject;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.codec.binary.Base64.decodeBase64;

/**
 * Business Service class in charge of the encryption/decryption/signing of the different messages.
 */
@Stateless
public class MessageEncryptionService {
    @Inject
    private KeystoreHelperService keystoreHelperService;

    /**
     * Seal a message (UTF-8 encoded) using a Base64 private key (512 bits decoded).
     *
     * @param xmlMessage - {@link String} XML message to encode
     * @return a Base64 encoded {@link String}
     * @throws BusinessException
     */
    public byte[] sealMessage(String xmlMessage) throws BusinessException {
        if (xmlMessage == null) {
            return new byte[0];
        }
        return sealMessage(xmlMessage.getBytes(UTF_8), getPrivateKeyForSigning());
    }

    /**
     * Unseal a message (Base64 encoded) using a Base64 public key (256 bits decoded).
     *
     * @param sealedMessage - {@link String} sealed message
     * @param b64PublicKey - {@link String} base64 encoded public key (256 bits decoded)
     * @return a UTF-8 encoded {@link String}
     * @throws BusinessException
     */
    public String verifyMessage(byte[] sealedMessage, String b64PublicKey) throws BusinessException {
        if (sealedMessage == null) {
            return null;
        }
        if (!Base64.isBase64(sealedMessage)) {
            throw new BusinessException(MessageEncryptionError.EXPECTED_BASE64_SEALED_MESSAGE);
        }
        requireNonNull(b64PublicKey);
        if (!Base64.isBase64(b64PublicKey)) {
            throw new BusinessException(MessageEncryptionError.EXPECTED_BASE64_PUBLIC_KEY);
        }
        byte[] publicKey = decodeBase64(b64PublicKey);
        verifyPublicKeyLength(publicKey);
        return verifyMessage(decodeBase64(sealedMessage), publicKey);
    }

    private byte[] sealMessage(byte[] xmlMessage, byte[] privateKey) throws BusinessException {
        verifyPrivateKeyLength(privateKey);

        byte[] cipher = new byte[xmlMessage.length + Sodium.SIGNATURE_BYTES];

        // seal the message
        int result = NaCl.sodium()
                .crypto_sign_ed25519(cipher, new LongLongByReference(), xmlMessage, xmlMessage.length, privateKey);
        // check status of the operation (0=success)
        if (result != 0) {
            throw new BusinessException(MessageEncryptionError.MESSAGE_SEALING_FAILED);
        }

        // encode result in Base64
        return Base64.encodeBase64(cipher);
    }

    private String verifyMessage(byte[] sealedMessage, byte[] publicKey) throws BusinessException {
        byte[] decipher = new byte[sealedMessage.length - Sodium.SIGNATURE_BYTES];

        // unseal the (not-anymore Base64-encoded) message
        int result = NaCl.sodium().crypto_sign_ed25519_open(decipher, new LongLongByReference(),
                sealedMessage,
                sealedMessage.length,
                publicKey);

        // check status of the operation (0=success)
        if (result != 0) {
            throw new BusinessException(MessageEncryptionError.MESSAGE_UNSEALING_FAILED);
        }
        // return a UTF-8 string
        return new String(decipher, UTF_8);
    }

    private byte[] getPrivateKeyForSigning() {
        // get the private key in the java key store
        return keystoreHelperService.loadSecretKey();
    }

    private void verifyPrivateKeyLength(byte[] privateKey) throws BusinessException {
        if (privateKey.length != Sodium.SIGNATURE_BYTES) {
            throw new BusinessException(MessageEncryptionError.EXPECTED_512BITS_PRIVATE_KEY);
        }
    }

    private void verifyPublicKeyLength(byte[] publicKey) throws BusinessException {
        if (publicKey.length != Sodium.PUBLICKEY_BYTES) {
            throw new BusinessException(MessageEncryptionError.EXPECTED_256BITS_PUBLIC_KEY);
        }
    }
}
