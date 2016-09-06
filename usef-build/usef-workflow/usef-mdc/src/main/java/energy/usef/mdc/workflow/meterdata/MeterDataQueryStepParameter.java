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

package energy.usef.mdc.workflow.meterdata;

/**
 * This Class contains the IN and OUT enum's which define the parameters for this PBC.
 */
public class MeterDataQueryStepParameter {

    private MeterDataQueryStepParameter() {
        // private constructor.
    }

    /**
     * The IN parameters for this PBC.
     */
    public enum IN {
        PTU_DURATION, DATE_RANGE_START, DATE_RANGE_END, CONNECTIONS, META_DATA_QUERY_TYPE
    }

    /**
     * The OUT paramters for this PBC.
     */
    public enum OUT {
        METER_DATA
    }
}
