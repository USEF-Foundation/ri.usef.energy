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

package energy.usef.dso.workflow.plan.connection.forecast;

/**
 * Enumeration for the workflow parameters.
 */
public class WorkflowParameter {
    public static final String CONGESTION_POINTS = "CONGESTION_POINTS";
    public static final String PTU_DATE = "PTU_DATE";
    public static final String CONGESTION_POINT_ENTITY_ADDRESS = "CONGESTION_POINT_ENTITY_ADDRESS";
    public static final String CONGESTION_POINT = "CONGESTION_POINT";
    public static final String AGR_DOMAIN_LIST = "AGR_DOMAIN_LIST";
    public static final String AGR_CONNECTION_COUNT_LIST = "AGR_CONNECTION_COUNT_LIST";
    public static final String PTU_DURATION = "PTU_DURATION";
    public static final String MESSAGE = "MESSAGE";
    public static final String PROGNOSIS_MESSAGE_XML = "PROGNOSIS_MESSAGE_XML";
    public static final String COMMON_REFERENCE_QUERY_RESPONSE_MESSAGE = "COMMON_REFERENCE_QUERY_RESPONSE_MESSAGE";

    /*
     * Those are expected to be produced by the workflow step. Each of them are expected to be arrays of long.
     */
    public static final String POWER = "POWER";
    public static final String START = "START";
    public static final String DURATION = "DURATION";
    public static final String MAXLOAD = "MAXLOAD";

    public static final String SEQUENCE_TOO_SMALL = "New sequence number should be bigger than previous one.";

    private WorkflowParameter() {

    }

}
