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

package energy.usef.dso.workflow.coloring;

/**
 * Parameters used by the Post Coloring Process PBC of the 'Coloring Process' workflow.
 */
public final class PostColoringProcessParameter {

    private PostColoringProcessParameter() {
        // private constructor
    }

    /**
     * The ingoing parameters.
     */
    public enum IN {
        PTU_DURATION,
        PERIOD,
        PTU_CONTAINER_DTO_LIST;
    }

    /**
     * No outgoing parameters.
     */
}
