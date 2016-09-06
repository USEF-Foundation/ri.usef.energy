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

package energy.usef.core.service.business.error;

import energy.usef.core.exception.BusinessError;

/**
 * Enumeration of the {@link BusinessError}s concerning the encryption/signature of the messages
 */
public enum MessageEncryptionError implements BusinessError {
    MESSAGE_SEALING_FAILED("The sealing of the message with the private key failed."),
    MESSAGE_UNSEALING_FAILED("The unsealing of the message with the public key failed."),
    EXPECTED_BASE64_PRIVATE_KEY("The private key must be encoded in Base 64"),
    EXPECTED_BASE64_PUBLIC_KEY("The public key must be encoded in Base 64"),
    EXPECTED_512BITS_PRIVATE_KEY("The private key must have 512 bits"),
    EXPECTED_256BITS_PUBLIC_KEY("The public key must have 256 bits"),
    EXPECTED_BASE64_SEALED_MESSAGE("The sealed message must be base64 encoded."),
    IMPOSSIBLE_TO_RETRIEVE_KEY("Impossible to retrieve the private key from the key store.");

    private final String error;

    MessageEncryptionError(String error) {
        this.error = error;
    }

    @Override
    public String getError() {
        return error;
    }
}
