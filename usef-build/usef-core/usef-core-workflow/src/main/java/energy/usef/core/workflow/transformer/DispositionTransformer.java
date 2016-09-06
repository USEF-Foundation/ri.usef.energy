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

import energy.usef.core.model.DispositionAvailableRequested;
import energy.usef.core.workflow.dto.DispositionTypeDto;

/**
 * Transformer class for transforming between different disposition types.
 */
public class DispositionTransformer {

    private DispositionTransformer() {
        // private method for static class
    }

    /**
     * Transform DB model DispositionAvailableRequested to DispositionAvailableRequested.
     * 
     * @param disposition - The DB model DispositionAvailableRequested
     * @return - The correct DispositionAvailableRequested
     */
    public static DispositionTypeDto transform(DispositionAvailableRequested disposition) {
        if (disposition == null) {
            return null;
        }
        if (DispositionAvailableRequested.AVAILABLE.equals(disposition)) {
            return DispositionTypeDto.AVAILABLE;
        } else {
            return DispositionTypeDto.REQUESTED;
        }
    }

    /**
     * Transform DispositionType to DispositionAvailableRequested.
     * 
     * @param disposition - The DispositionType
     * @return - The correct DispositionAvailableRequested
     */
    public static DispositionAvailableRequested transform(DispositionTypeDto disposition) {
        if (disposition == null) {
            return null;
        }
        if (DispositionTypeDto.AVAILABLE.equals(disposition)) {
            return DispositionAvailableRequested.AVAILABLE;
        } else {
            return DispositionAvailableRequested.REQUESTED;
        }
    }

    /**
     * Transform DispositionTypeDto to DispositionAvailableRequested.
     * 
     * @param disposition DispositionTypeDto
     * @return DispositionAvailableRequested
     */
    public static energy.usef.core.data.xml.bean.message.DispositionAvailableRequested transformToXml(DispositionTypeDto disposition) {
        if (disposition == null) {
            return null;
        }
        if (DispositionTypeDto.AVAILABLE.equals(disposition)) {
            return energy.usef.core.data.xml.bean.message.DispositionAvailableRequested.AVAILABLE;
        } else {
            return energy.usef.core.data.xml.bean.message.DispositionAvailableRequested.REQUESTED;
        }
    }

}
