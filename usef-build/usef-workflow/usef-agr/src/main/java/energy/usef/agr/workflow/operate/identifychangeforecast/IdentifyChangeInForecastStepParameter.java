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

package energy.usef.agr.workflow.operate.identifychangeforecast;

/**
 * The defined parameters for the 'AGR identify changes in forecast' workflow.
 */
public class IdentifyChangeInForecastStepParameter {

    private IdentifyChangeInForecastStepParameter() {
        // private constructor
    }

    /**
     * The incoming parameters.
     */
    public enum IN {
        PERIOD,
        CONNECTION_PORTFOLIO,
        LATEST_A_PLANS_DTO_LIST,
        PTU_DURATION
    }

    /**
     * The outgoing parameters.
     */
    public enum OUT {
        FORECAST_CHANGED,
        FORECAST_CHANGED_PTU_CONTAINER_DTO_LIST
    }

}
