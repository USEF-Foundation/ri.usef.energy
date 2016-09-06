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

package energy.usef.agr.workflow.operate.deviation;

/**
 * Parameters used by the 'AGR Non-Udi Detect deviations from Prognoses' workflow.
 */
public final class NonUdiDetectDeviationFromPrognosisStepParameter {

    private NonUdiDetectDeviationFromPrognosisStepParameter() {
        // private constructor.
    }

    /**
     * The incoming parameters for the non-udi Detect deviations from Prognoses step.
     */
    public enum IN {
        USEF_IDENTIFIER, CONNECTION_PORTFOLIO_DTO, LATEST_PROGNOSIS, PTU_DURATION, PERIOD, CURRENT_PTU_INDEX
    }

    /**
     * The outgoing parameters for the non-udi Detect deviations from Prognoses step.
     */
    public enum OUT {
        DEVIATION_INDEX_LIST
    }

}
