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

package energy.usef.brp.workflow.plan.connection.forecast;

/**
 * Class containing the enumeration of the different workflow parameters for the workflow describing the sending of flex requests.
 */
public class PrepareFlexRequestWorkflowParameter {
    /**
     * Input parameter names enumeration.
     */
    public enum IN {
        PTU_DURATION,
        PROCESSED_A_PLAN_DTO_LIST
    }

    /**
     * Output parameter names enumeration.
     */
    public enum OUT {
        FLEX_REQUEST_DTO_LIST,
        ACCEPTED_A_PLAN_DTO_LIST
    }
}
