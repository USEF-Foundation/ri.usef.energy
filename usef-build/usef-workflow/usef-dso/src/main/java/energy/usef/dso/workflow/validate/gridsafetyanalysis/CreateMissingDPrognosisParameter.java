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

package energy.usef.dso.workflow.validate.gridsafetyanalysis;

/**
 * Parameters used by the Create Missing D-Prognosis PBC of the 'Grid Safety Analysis' workflow.
 */
public final class CreateMissingDPrognosisParameter {

    private CreateMissingDPrognosisParameter() {
        // private constructor
    }

    /**
     * The ingoing parameters.
     */
    public enum IN {
        CONGESTION_POINT_ENTITY_ADDRESS, AGGREGATOR_DOMAIN, ANALYSIS_DAY, PTU_DURATION, AGGREGATOR_CONNECTION_AMOUNT
    }

    /**
     * The outgoing parameters.
     */
    public enum OUT {
        D_PROGNOSIS
    }

}
