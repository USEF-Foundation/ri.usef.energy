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
 * Parameters used by the 'Grid Safety Analysis' workflow.
 */
public final class CreateGridSafetyAnalysisStepParameter {

    private CreateGridSafetyAnalysisStepParameter() {

    }

    /**
     * The ingoing parameters for the create grid safety analysis step.
     */
    public enum IN {
        PTU_DURATION,
        CONGESTION_POINT_ENTITY_ADDRESS,
        D_PROGNOSIS_LIST,
        NON_AGGREGATOR_FORECAST,
        PERIOD;
    }

    /**
     * The outgoing parameters for the create grid safety analysis step.
     */
    public enum OUT {
        GRID_SAFETY_ANALYSIS
    }

}
