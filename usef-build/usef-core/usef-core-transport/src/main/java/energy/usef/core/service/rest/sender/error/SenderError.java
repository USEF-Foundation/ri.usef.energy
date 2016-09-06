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

package energy.usef.core.service.rest.sender.error;

import energy.usef.core.exception.BusinessError;

/**
 * Enumeration of the BusinessError related to the sender errors.
 */
public enum SenderError implements BusinessError {
    NO_MESSAGE_TYPE("XML has no MessageType.");

    private final String error;

    SenderError(String error) {
        this.error = error;
    }

    @Override
    public String getError() {
        return error;
    }

}
