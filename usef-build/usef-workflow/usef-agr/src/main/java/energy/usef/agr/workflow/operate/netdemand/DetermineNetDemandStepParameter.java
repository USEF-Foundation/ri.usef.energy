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

package energy.usef.agr.workflow.operate.netdemand;

/**
 * List of the possible input/ouptut parameters for the 'DetermineNetDemand' workflow (AGR side).
 */
public final class DetermineNetDemandStepParameter {

    private DetermineNetDemandStepParameter() {
        // private constructor.
    }

    /**
     * Input paramaters
     */
    public enum IN {
        PERIOD,
        PTU_DURATION,
        CONNECTION_PORTFOLIO_DTO_LIST,
        UDI_EVENT_DTO_MAP
    }

    /**
     * Output parameters
     */
    public enum OUT {
        CONNECTION_PORTFOLIO_DTO_LIST,
        UPDATED_UDI_EVENT_DTO_LIST
    }

}
