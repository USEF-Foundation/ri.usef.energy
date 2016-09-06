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

package energy.usef.dso.workflow;

/**
 * Enumeration which contains all the names for the DSO WorkflowSteps.
 */
public enum DsoWorkflowStep {
    DSO_CREATE_FLEX_REQUEST,
    DSO_CREATE_GRID_SAFETY_ANALYSIS,
    DSO_CREATE_MISSING_DPROGNOSES,
    DSO_CREATE_NON_AGGREGATOR_FORECAST,
    DSO_DETERMINE_ORANGE_OUTAGE_PERIODS,
    DSO_DETERMINE_ORANGE_REDUCTION_PERIODS,
    DSO_DETERMINE_ORANGE_REGIME_COMPENSATIONS,
    DSO_LIMIT_CONNECTIONS,
    DSO_METER_DATA_QUERY_EVENTS,
    DSO_MONITOR_GRID,
    DSO_PLACE_FLEX_ORDERS,
    DSO_RESTORE_CONNECTIONS,
    DSO_INITIATE_SETTLEMENT,
    DSO_REQUEST_PENALTY_DATA,
    DSO_PREPARE_STEPWISE_LIMITING,
    DSO_POST_COLORING_PROCESS,
    DSO_PLACE_OPERATE_FLEX_ORDERS
}
