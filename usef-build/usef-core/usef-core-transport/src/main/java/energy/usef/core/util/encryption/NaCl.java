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

import energy.usef.core.util.VersionUtil;
import jnr.ffi.LibraryLoader;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.byref.LongLongByReference;
import jnr.ffi.types.u_int64_t;

/***
 * NaCl class to load sodium.
 */
public class NaCl {
    private static final String LIBRARY_NAME = "sodium";
    private static final String VERSION = "1.0.8";

    /**
     * Creates a sodium instance.
     *
     * @return a sodium instance as singleton.
     */
    public static Sodium sodium() {
        Sodium sodium = SingletonHolder.SODIUM_INSTANCE;

        if (VersionUtil.compareVersions(VERSION, sodium.sodium_version_string()) > 0) {
            String message = String.format("Unsupported libsodium version: %s. Please update",
                    sodium.sodium_version_string());
            throw new UnsupportedOperationException(message);
        }
        return sodium;
    }

    private static final class SingletonHolder {
        public static final Sodium SODIUM_INSTANCE = LibraryLoader.create(Sodium.class).load(LIBRARY_NAME);
        static { // added to make sure library inits
            SODIUM_INSTANCE.sodium_init();
        }
    }

    private NaCl() {
    }

    /***
     * Sodium interface.
     */
    public interface Sodium {

        /**
         * This function isn't thread safe. Be sure to call it once, and before performing other operations.
         *
         * Check libsodium's documentation for more info.
         */
        int sodium_init();

        /**
         * Sodium version number.
         *
         * @return the sodium version number.
         */
        String sodium_version_string();

        /**
         * crypto_sign_ed25519_seed_keypair.
         *
         * @param publicKey the public key.
         * @param secretKey the secret key.
         * @param seed the seed.
         * @return an integer.
         */
        int crypto_sign_ed25519_seed_keypair(@Out byte[] publicKey, @Out byte[] secretKey, @In byte[] seed);

        /**
         * crypto_sign_ed25519.
         *
         * @param buffer the buffer.
         * @param bufferLen the bufferLen.
         * @param message the message.
         * @param length the length.
         * @param secretKey the secretKey.
         * @return an integer.
         */
        int crypto_sign_ed25519(@Out byte[] buffer, @Out LongLongByReference bufferLen, @In byte[] message, @u_int64_t long length,
                @In byte[] secretKey);

        /**
         * crypto_sign_ed25519_open.
         *
         * @param buffer the buffer.
         * @param bufferLen the bufferLen.
         * @param sigAndMsg the sigAnMsg.
         * @param length the length.
         * @param key the key.
         * @return an integer.
         */
        int crypto_sign_ed25519_open(@Out byte[] buffer, @Out LongLongByReference bufferLen, @In byte[] sigAndMsg,
                @u_int64_t long length, @In byte[] key);
    }
}
