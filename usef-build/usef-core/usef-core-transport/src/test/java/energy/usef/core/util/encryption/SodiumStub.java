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

package energy.usef.core.util.encryption;

import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.byref.LongLongByReference;
import jnr.ffi.types.u_int64_t;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 *
 */
public class SodiumStub implements NaCl.Sodium {
    private static final Logger LOGGER = LoggerFactory.getLogger(SodiumStub.class);

    private byte[] seed;
    private byte[] publicKey;
    private byte[] secretKey;
    private byte[] signedMessage;
    private byte[] unsignedMessage;

    public SodiumStub() {
    }

    public void setSeed(byte[] seed) {
        this.seed = java.util.Arrays.copyOf(seed, seed.length);
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = java.util.Arrays.copyOf(publicKey, publicKey.length);
    }

    public void setSecretKey(byte[] secretKey) {
        this.secretKey = java.util.Arrays.copyOf(secretKey, secretKey.length);
    }

    public void setSignedMessage(byte[] signedMessage) {
        this.signedMessage = java.util.Arrays.copyOf(signedMessage, signedMessage.length);
    }

    public void setUnsignedMessage(byte[] unsignedMessage) {
        this.unsignedMessage = java.util.Arrays.copyOf(unsignedMessage, unsignedMessage.length);
    }

    @Override
    public int sodium_init() {
        return 0;
    }

    @Override
    public String sodium_version_string() {
        return null;
    }

    @Override
    public int crypto_sign_ed25519_seed_keypair(@Out byte[] publicKey, @Out byte[] secretKey, @In byte[] seed) {
        if (Arrays.equals(seed, this.seed)) {
            System.arraycopy(this.publicKey, 0, publicKey, 0, this.publicKey.length);
            System.arraycopy(this.secretKey, 0, secretKey, 0, this.secretKey.length);
        }
        return 0;
    }

    @Override
    public int crypto_sign_ed25519(@Out byte[] buffer, @Out LongLongByReference bufferLen, @In byte[] message, @u_int64_t long length, @In byte[] secretKey) {
        if (Arrays.equals(secretKey, this.secretKey)) {
            if (Arrays.equals(message, this.unsignedMessage)) {
                System.arraycopy(this.signedMessage, 0, buffer, 0, this.signedMessage.length);
            }
        } else {
            LOGGER.info("Unanticipated key: {}", secretKey);
        }
        return 0;
    }

    @Override
    public int crypto_sign_ed25519_open(@Out byte[] buffer, @Out LongLongByReference bufferLen, @In byte[] sigAndMsg, @u_int64_t long length, @In byte[] key) {
        if (Arrays.equals(key, publicKey)) {
            if (Arrays.equals(sigAndMsg, this.signedMessage)) {
                System.arraycopy(this.unsignedMessage, 0, buffer, 0, this.unsignedMessage.length);
            } else {
                LOGGER.info("Unanticipated message: {}", sigAndMsg);
            }
        }
        else {
            LOGGER.info("Unanticipated key: {}", key);
        }
        return 0;
    }
}
