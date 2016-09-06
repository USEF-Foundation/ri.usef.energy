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
 * Enumeration of the {@link BusinessError}s related to the message controller.
 */
public enum MessageControllerError implements BusinessError {
    RESPONSE_MESSAGE_NOT_PART_OF_A_CONVERSATION("Response message is not part of an existing conversation."),
    XML_NOT_CONVERTED_TO_OBJECT("Can not convert the xml to an object.");

    private final String error;

    MessageControllerError(String error) {
        this.error = error;
    }

    @Override
    public String getError() {
        return error;
    }

}
