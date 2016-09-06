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

package energy.usef.core.workflow.transformer;

import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.workflow.dto.AcknowledgementStatusDto;

/**
 * Transformer class in charge of the conversion of the {@link AcknowledgementStatus} to its related DTO.
 */
public class AcknoledgementStatusTransformer {

    /**
     * Private constructor to prevent instantiation.
     */
    private AcknoledgementStatusTransformer() {
        // prevent instantiation of the class.
    }

    /**
     * Transforms the enumerated value to its DTO.
     *
     * @param status {@link AcknowledgementStatus}.
     * @return a {@link AcknowledgementStatusDto}.
     */
    public static AcknowledgementStatusDto transform(AcknowledgementStatus status) {
        if (status == null) {
            return null;
        }
        switch (status) {
        case ACCEPTED:
            return AcknowledgementStatusDto.ACCEPTED;
        case NO_RESPONSE:
            return AcknowledgementStatusDto.NO_RESPONSE;
        case REJECTED:
            return AcknowledgementStatusDto.REJECTED;
        case SENT:
            return AcknowledgementStatusDto.SENT;
        default:
            return null;
        }
    }

}
