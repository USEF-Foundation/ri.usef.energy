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

package energy.usef.brp.workflow.plan.aplan.missing;

/**
 * Parameters used by the BRP Create Missing A-Plans PBC.
 */
public class BrpCreateMissingAPlansParamater {

    private BrpCreateMissingAPlansParamater() {
        // private constructor
    }

    /**
     * The ingoing parameters.
     */
    public enum IN {
        AGGREGATOR_DOMAIN, CONNECTION_COUNT, PERIOD, PTU_DURATION
    }

    /**
     * The outgoing parameters.
     */
    public enum OUT {
        PROGNOSIS_DTO
    }

}
