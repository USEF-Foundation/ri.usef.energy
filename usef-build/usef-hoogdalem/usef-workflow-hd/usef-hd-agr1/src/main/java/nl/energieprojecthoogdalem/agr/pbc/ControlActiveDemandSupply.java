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

import info.usef.agr.dto.device.request.DeviceMessageDto;
import info.usef.agr.workflow.operate.control.ads.ControlActiveDemandSupplyStepParameter.IN;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import nl.energieprojecthoogdalem.messageservice.scheduleservice.MessageScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Hoogdalem Implementation of workflow step of Aggregator controlling Active Demands and Supplies.
 * <p>
 * This workflow step receives as input a UDI Control Message ({@link DeviceMessageDto}) to be processed/sent.
 * <p>
 * The output of this workflow step is the {@link DeviceMessageDto} which failed to be processed, if ever.
 */
public class ControlActiveDemandSupply implements WorkflowStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(info.usef.agr.workflow.step.AgrControlActiveDemandSupplyStub.class);

    @Inject
    private MessageScheduler scheduler;

    /**
     * shifts the batteries by scheduling device messages in the {@link MessageScheduler}
    * */
    @Override
    public WorkflowContext invoke(WorkflowContext context)
    {
        DeviceMessageDto deviceMessageDto = context.get(IN.DEVICE_MESSAGE_DTO.name(), DeviceMessageDto.class);
        LOGGER.trace("Received message for {}", deviceMessageDto.getEndpoint());

        /*
        * only one shift request will be created reason:
        *
        * the charging of the battery can only be shifted once
        * a new deviceMessage will be created when the battery needs to be shifted to a different PTU slot
        *
        * (no other capabilities are available)
        * */
        scheduler.scheduleShiftTask(deviceMessageDto.getEndpoint(), deviceMessageDto.getShiftRequestDtos().get(0));

        return context;
    }

}
