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

package energy.usef.brp.workflow.plan.flexrequest.create;

import energy.usef.core.workflow.dto.FlexRequestDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Event which can trigger the workflow creating and sending flex requests from the BRP to the AGR.
 */
public class CreateFlexRequestEvent {

    private final List<FlexRequestDto> flexRequestDtos;

    /**
     * Private constructor.
     */
    @SuppressWarnings("unused")
    private CreateFlexRequestEvent() {
        flexRequestDtos = new ArrayList<>();
    }

    /**
     * Constructor.
     *
     * @param flexRequestDtos {@link List} of prefilled {@link FlexRequestDto} (prognosis sequence, origin and period and PTUs
     * should be filled in).
     */
    public CreateFlexRequestEvent(List<FlexRequestDto> flexRequestDtos) {
        super();
        this.flexRequestDtos = new ArrayList<>(flexRequestDtos);
    }

    public List<FlexRequestDto> getFlexRequestDtos() {
        return flexRequestDtos;
    }

    @Override
    public String toString() {
        return "CreateFlexRequestEvent" + "[" +
                "#flexRequestDtos=" + flexRequestDtos.size() +
                "]";
    }
}
