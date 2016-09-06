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

package energy.usef.agr.workflow.plan.connection.profile;

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.ElementDto;
import energy.usef.agr.dto.device.capability.UdiEventDto;

/**
 * This class contains the enumerations of possible parameters for the workflow population the Profile power values in the
 * connection portfolio.
 */
public class CreateUdiStepParameter {

    /**
     * Input parameters: <br/>
     * PERIOD: {@link java.time.LocalDate} <br/>
     * PTU_DURATION: {@link Integer} Duration in minutes.<br/>
     * CONNECTION_PORTFOLIO_DTO_LIST: List of {@link ConnectionPortfolioDto}'s.<br/>
     * ELEMENT_PER_CONNECTION_MAP: Map of String (connectionEntityAddress) to a List of {@link ElementDto}'s.
     *
     */
    public enum IN {
        PERIOD,
        PTU_DURATION,
        CONNECTION_PORTFOLIO_DTO_LIST,
        ELEMENT_PER_CONNECTION_MAP
    }

    /**
     * Output parameters:  <br/>
     * CONNECTION_PORTFOLIO_DTO_LIST: List of {@link ConnectionPortfolioDto}'s.<br/>
     * UDI_EVENTS_PER_UDI_MAP: Map of String (udi endpoint) to a List of {@link UdiEventDto}'s.
     */
    public enum OUT {
        CONNECTION_PORTFOLIO_DTO_LIST,
        UDI_EVENTS_PER_UDI_MAP
    }
}
