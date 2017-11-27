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

package nl.energieprojecthoogdalem.agr.pbc;

import info.usef.agr.dto.ConnectionGroupPortfolioDto;
import info.usef.agr.workflow.operate.deviation.DetectDeviationFromPrognosisStepParameter.OUT;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import info.usef.core.workflow.dto.PrognosisDto;

import java.util.ArrayList;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hoogdalem Implementation of workflow step "DetectDeviationFromPrognosis".
 * <p>
 * returns no deviation
 * <ul>
 * <li>PERIOD</li> : the period the portfolio ({@link LocalDate}).
 * <li>PTU_DURATION</li> : the duration of PTU expressed in minutes ({@link Integer}).
 * <li>CURRENT_PTU_INDEX</li> : current ptu index ({@link Integer}).
 * <li>CONNECTION_PORTFOLIO_DTO</li> : a {@link ConnectionGroupPortfolioDto} containing the current portfolio.
 * <li>LATEST_PROGNOSIS</li> : a {@link PrognosisDto} containing the latest A-plans and/or D-prognoses.
 * </ul>
 * <p>
 * This PBC must output:
 * <ul>
 * <li>DEVIATION_INDEX_LIST</li> : a list of {@link Integer} which contains the ptu indexes with deviation.
 * </ul>
 */
public class DetectDeviationFromPrognoses implements WorkflowStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DetectDeviationFromPrognoses.class);

    @Override
    public WorkflowContext invoke(WorkflowContext context)
    {
        LOGGER.info("DetectDeviationFromPrognosis returning no deviation");

        context.setValue(OUT.DEVIATION_INDEX_LIST.name(), new ArrayList<>());

        return context;
    }

}
