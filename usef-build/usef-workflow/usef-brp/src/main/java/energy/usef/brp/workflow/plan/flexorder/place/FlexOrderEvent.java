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

package energy.usef.brp.workflow.plan.flexorder.place;

/**
 * Event class for triggering the placement of Flex Orders on the BRP side.
 */
public class FlexOrderEvent {

    /**
     * Default constructor.
     */
    public FlexOrderEvent() {
        // do nothing
    }

    @Override
    public String toString() {
        return "FlexOrderEvent" + "[" + "]";
    }
}
