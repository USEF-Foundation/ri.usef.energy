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

package energy.usef.brp.workflow.settlement.initiate;

/**
 * Contains the parameters for the BRPRequestCalculateProcuredFlexibility step.
 */
public final class RequestCalculateProcuredFlexibilityParameter {

    private RequestCalculateProcuredFlexibilityParameter() {
    }

    /**
     * The input parameters for this step.
     */
    public enum IN {
        DAY, CONNECTION_LIST
    }

    /**
     * The output parameters for this step.
     */
    public enum OUT {
        PTU_POWER_CONSUMPTION_DTO_LIST
    }
}
