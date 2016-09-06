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

package energy.usef.agr.workflow.operate.recreate.prognoses;

/**
 * Class which contains the enumeration of the Worklfow Parameters for the workflow 'Re-Create A-Plans and/or D-Prognoses'.
 */
public class ReCreatePrognosesWorkflowParameter {

    private ReCreatePrognosesWorkflowParameter() {
        // private constructor
    }

    /**
     * Enumeration of the input parameters of the workflow.
     */
    public enum IN {
        /**
         * List of the latest d-prognoses
         */
        LATEST_D_PROGNOSES_DTO_LIST,
        /**
         * List of the latest a-plans
         */
        LATEST_A_PLANS_DTO_LIST,
        /**
         * Current portfolio, respecting the XSD specification.
         */
        CURRENT_PORTFOLIO,
        /**
         * Map containing connection groups to list of connections.
         */
        CONNECTION_GROUPS_TO_CONNECTIONS_MAP,
        /**
         * Period for which prognoses can be recreated.
         */
        PERIOD,
        /**
         * Size of a PTU in minutes.
         */
        PTU_DURATION
    }

    /**
     * Enumeration of the output parameters of the workflow.
     */
    public enum OUT {
        /**
         * List of Long indicating that the creation of new A-Plans is required.
         */
        REQUIRES_NEW_A_PLAN_SEQUENCES_LIST,
        /**
         * List of Long indicating that the creation of new D-Prognoses is required.
         */
        REQUIRES_NEW_D_PROGNOSIS_SEQUENCES_LIST
    }

}
