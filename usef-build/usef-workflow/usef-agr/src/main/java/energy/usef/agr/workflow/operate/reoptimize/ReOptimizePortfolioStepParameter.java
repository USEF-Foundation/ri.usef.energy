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

package energy.usef.agr.workflow.operate.reoptimize;

/**
 * Defined paramaters for conversing with the Re-optimize Portfolio Pluggable Business Component.
 */
public final class ReOptimizePortfolioStepParameter {

    /**
     * Private constructor for this class.
     */
    private ReOptimizePortfolioStepParameter() {
    }

    /**
     * The incoming parameters for this step.
     */
    public enum IN {
        CONNECTION_PORTFOLIO_IN,
        CONNECTION_GROUPS_TO_CONNECTIONS_MAP,
        PTU_DURATION,
        PTU_DATE,
        CURRENT_PTU_INDEX,
        LATEST_A_PLAN_DTO_LIST,
        LATEST_D_PROGNOSIS_DTO_LIST,
        RECEIVED_FLEXORDER_LIST,
        RELEVANT_PROGNOSIS_LIST,
        UDI_EVENTS
    }

    /**
     * The outgoing parameters for this step.
     */
    public enum OUT {
        CONNECTION_PORTFOLIO_OUT,
        DEVICE_MESSAGES_OUT
    }
    /**
     * The outgoing parameters for this step.
     */
    public enum OUT_NON_UDI {
        CONNECTION_PORTFOLIO_OUT
    }

}
