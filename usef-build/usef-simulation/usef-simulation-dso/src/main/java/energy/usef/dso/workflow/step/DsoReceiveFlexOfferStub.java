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

package energy.usef.dso.workflow.step;

import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.dso.workflow.validate.create.flexoffer.PlaceFlexOfferStepParameter.IN;
import energy.usef.dso.workflow.validate.create.flexoffer.PlaceFlexOfferStepParameter.OUT;
import org.joda.time.LocalDate;

/**
 * Workflow step implementation for the Workflow 'Receive Flexibility Offers'. This implementation expects to find the following
 * parameters as input:
 * <ul>
 * <li>CONGESTION_POINT_ENTITY_ADDRESS: the entity address of the congestion point ({@link String})</li>
 * <li>PERIOD: day for which one wants to send flex requests ({@link LocalDate})</li>
 * <li>FLEX_OFFER_DTO: Array of {@link energy.usef.core.workflow.dto.FlexOfferDto} containing the data of the flex offer.</li>
 * </ul>
 */
public class DsoReceiveFlexOfferStub implements WorkflowStep {

    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        FlexOfferDto flexOfferDto = (FlexOfferDto) context.getValue(IN.FLEX_OFFER_DTO.name());
        context.setValue(OUT.ACCEPTED_FLEX_OFFER_DTO.name(), flexOfferDto);
        return context;
    }
}
