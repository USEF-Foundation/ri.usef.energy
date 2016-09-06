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

import energy.usef.core.data.xml.bean.message.DispositionAcceptedDisputed;
import energy.usef.core.workflow.dto.DispositionAcceptedDisputedDto;

/**
 * Transformer class for the {@link DispositionAcceptedDisputedDto} enumeration.
 */
public class DispositionAcceptedDisputedTransformer {

    private DispositionAcceptedDisputedTransformer() {
        // do nothing, prevent instantiation.
    }

    /**
     * Transform DispositionAcceptedDisputedDto to DispositionAcceptedDisputed.
     *
     * @param disposition DispositionAcceptedDisputedDto
     * @return DispositionAcceptedDisputed
     */
    public static DispositionAcceptedDisputed transformToXml(DispositionAcceptedDisputedDto disposition) {
        if (disposition == null) {
            return null;
        }
        switch (disposition) {
        case ACCEPTED:
            return DispositionAcceptedDisputed.ACCEPTED;
        case DISPUTED:
            return DispositionAcceptedDisputed.DISPUTED;
        default:
            return null;
        }
    }

    /**
     * Transform DispositionAcceptedDisputed to DispositionAcceptedDisputedDto.
     *
     * @param disposition DispositionAcceptedDisputed
     * @return DispositionAcceptedDisputedDto
     */
    public static DispositionAcceptedDisputedDto transform(DispositionAcceptedDisputed disposition) {
        if (disposition == null) {
            return null;
        }
        switch (disposition) {
        case ACCEPTED:
            return DispositionAcceptedDisputedDto.ACCEPTED;
        case DISPUTED:
            return DispositionAcceptedDisputedDto.DISPUTED;
        default:
            return null;
        }
    }

}
