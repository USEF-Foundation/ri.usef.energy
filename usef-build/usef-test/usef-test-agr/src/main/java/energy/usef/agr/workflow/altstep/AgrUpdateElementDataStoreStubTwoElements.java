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

package energy.usef.agr.workflow.altstep;

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.ElementDto;
import energy.usef.agr.pbcfeederimpl.PbcFeederService;
import energy.usef.agr.workflow.plan.connection.profile.AgrUpdateElementDataStoreParameter.IN;
import energy.usef.agr.workflow.plan.connection.profile.AgrUpdateElementDataStoreParameter.OUT;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import org.joda.time.LocalDate;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of a workflow step to simulate the behavior of the AGR Element Data Store Update. This version of the
 * stub removes the PV elements after being retrieved from the PBC feeder.
 * <p>
 * The PBC receives the following parameters as input:
 * <ul>
 * <li>PERIOD: The period for which elements will be created.</li>
 * <li>PTU_DURATION: The number of minuetes for one ptu.</li>
 * <li>CONNECTION_PORTFOLIO_LIST: The complete connection portfolio ({@link List} of {@link ConnectionPortfolioDto} objects) for which the elements will be created.</li>
 * </ul>
 * <p>
 * The PBC returns the following parameters as output:
 * <ul>
 * <li>ELEMENT_LIST: The created Element list ({@link List} of {@link ElementDto} objects).</li>
 * </ul>
 */
public class AgrUpdateElementDataStoreStubTwoElements implements WorkflowStep {
    private static final String ELEMENT_PV_PROFILE_PREFIX = "PV";

    @Inject
    private PbcFeederService pbcFeederService;

    @Override
    public WorkflowContext invoke(WorkflowContext context) {

        // get the input parameters
        LocalDate period = context.get(IN.PERIOD.name(), LocalDate.class);
        int ptuDuration = context.get(IN.PTU_DURATION.name(), Integer.class);
        @SuppressWarnings("unchecked")
        List<ConnectionPortfolioDto> connectionPortfolioDtoList = context.get(IN.CONNECTION_PORTFOLIO_LIST.name(), List.class);

        int ptusPerDay = PtuUtil.getNumberOfPtusPerDay(period, ptuDuration);

        // retrieve some input from the PBC Feeder
        List<ElementDto> elementDtoList = pbcFeederService
                .fillElementsFromPBCFeeder(connectionPortfolioDtoList, period, ptusPerDay, ptuDuration);

        elementDtoList = elementDtoList.stream()
                .filter(elementDto -> !elementDto.getProfile().equals(ELEMENT_PV_PROFILE_PREFIX))
                .collect(Collectors.toList());

        context.setValue(OUT.ELEMENT_LIST.name(), elementDtoList);

        return context;
    }
}
