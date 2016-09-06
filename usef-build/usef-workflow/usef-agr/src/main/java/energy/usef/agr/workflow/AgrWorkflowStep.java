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

package energy.usef.agr.workflow;

/**
 * Enumeration which contains all the names for the AGR WorkflowSteps.
 */
public enum AgrWorkflowStep {
    AGR_CREATE_CONNECTION_PROFILE,
    AGR_CREATE_UDI,
    AGR_CREATE_N_DAY_AHEAD_FORECAST,
    AGR_NON_UDI_CREATE_N_DAY_AHEAD_FORECAST,
    AGR_IDENTIFY_CHANGE_IN_FORECAST,
    AGR_UPDATE_ELEMENT_DATA_STORE,
    AGR_REOPTIMIZE_PORTFOLIO,
    AGR_NON_UDI_REOPTIMIZE_PORTFOLIO,
    AGR_RECREATE_PROGNOSES,
    AGR_FLEX_OFFER_DETERMINE_FLEXIBILITY,
    AGR_VALIDATE_SETTLEMENT_ITEMS,
    AGR_DETERMINE_NET_DEMANDS,
    AGR_DETECT_DEVIATION_FROM_PROGNOSES,
    AGR_CONTROL_ACTIVE_DEMAND_SUPPLY,
    AGR_INITIALIZE_NON_UDI_CLUSTERS,
    AGR_INITIATE_SETTLEMENT,
    AGR_NON_UDI_SET_ADS_GOALS,
    AGR_NON_UDI_RETRIEVE_ADS_GOAL_REALIZATION,
    AGR_NON_UDI_DETECT_DEVIATION_FROM_PROGNOSES
}
