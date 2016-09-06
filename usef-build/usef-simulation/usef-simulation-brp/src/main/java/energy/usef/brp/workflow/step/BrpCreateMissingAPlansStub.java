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

package energy.usef.brp.workflow.step;

import energy.usef.brp.workflow.plan.aplan.missing.BrpCreateMissingAPlansParamater;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PrognosisTypeDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;

import java.math.BigInteger;
import java.util.Random;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;

/**
 * Workflow step implementation for the Workflow 'Brp Create Missing A-Plans'. This step is responsible for missing A-Plans
 * creation. This implementation expects to find the following parameters as input:
 * <p>
 * <ul>
 * <li>AGGREGATOR_DOMAIN: The aggregator domain ({@link String}).</li>
 * <li>CONNECTION_COUNT: The number of connections for this aggregator.</li>
 * <li>PTU_DURATION: The duration of one ptu in minutes.</li>
 * <li>PERIOD: The period of the A-Plans ({@link LocalDate}).</li>
 * </ul>
 * The step provides the following parameters as output:
 * <ul>
 * <li>PROGNOSIS_DTO: A {@link PrognosisDto} object containing the missing A-Plans.</li>
 * </ul>
 * <p>
 * The reference implementation of this PBC will generate a replacement A-plan with a random power value for each PTU equal to the
 * sum of M random numbers between -500 and 500, where M is the number of AGR connections from the PBC input list.
 */
public class BrpCreateMissingAPlansStub implements WorkflowStep {
    private static final Random RANDOM = new Random();
    private static final int POWER_LIMIT = 501;

    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        // get the input parameters
        String aggregatorDomain = context.get(BrpCreateMissingAPlansParamater.IN.AGGREGATOR_DOMAIN.name(), String.class);
        LocalDate period = context.get(BrpCreateMissingAPlansParamater.IN.PERIOD.name(), LocalDate.class);
        int ptuDuration = context.get(BrpCreateMissingAPlansParamater.IN.PTU_DURATION.name(), Integer.class);
        int connectionCount = context.get(BrpCreateMissingAPlansParamater.IN.CONNECTION_COUNT.name(), Integer.class);

        PrognosisDto prognosisDto = generateAPlan(aggregatorDomain, period, connectionCount, ptuDuration);
        context.setValue(BrpCreateMissingAPlansParamater.OUT.PROGNOSIS_DTO.name(), prognosisDto);

        return context;
    }

    private PrognosisDto generateAPlan(String aggregatorDomain, LocalDate period, int connectionCount, int ptuDuration) {
        int numberOfPtusPerDay = PtuUtil.getNumberOfPtusPerDay(period, ptuDuration);

        PrognosisDto prognosisDto = new PrognosisDto();
        prognosisDto.setConnectionGroupEntityAddress(aggregatorDomain);
        prognosisDto.setPeriod(period);
        prognosisDto.setType(PrognosisTypeDto.A_PLAN);
        IntStream.rangeClosed(1, numberOfPtusPerDay).mapToObj(ptuIndex -> {
            PtuPrognosisDto ptuPrognosisDto = new PtuPrognosisDto();
            ptuPrognosisDto.setPtuIndex(BigInteger.valueOf(ptuIndex));
            ptuPrognosisDto.setPower(generateRandomPowerValue(connectionCount));
            return ptuPrognosisDto;
        }).forEach(ptuPrognosisDto -> prognosisDto.getPtus().add(ptuPrognosisDto));

        return prognosisDto;
    }

    private BigInteger generateRandomPowerValue(int connectionCount) {
        // generate a random number between -500 and +500 multiplied by the number of connections
        return BigInteger.valueOf(connectionCount * RANDOM.nextInt(POWER_LIMIT) * (RANDOM.nextBoolean() ? 1 : -1));
    }
}
