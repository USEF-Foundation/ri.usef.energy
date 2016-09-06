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

package energy.usef.agr.workflow.step;

import energy.usef.agr.dto.device.request.DeviceMessageDto;
import energy.usef.agr.workflow.operate.control.ads.ControlActiveDemandSupplyStepParameter.IN;
import energy.usef.agr.workflow.operate.control.ads.ControlActiveDemandSupplyStepParameter.OUT;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a workflow step to simulate the behavior of an Aggregator controlling Active Demands and Supplies.
 * <p>
 * This workflow step receives as input a UDI Control Message ({@link DeviceMessageDto}) to be processed/sent.
 * <p>
 * The output of this workflow step is the {@link DeviceMessageDto} which failed to be processed, if ever.
 */
public class AgrControlActiveDemandSupplyStub implements WorkflowStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrControlActiveDemandSupplyStub.class);
    private static final int MAX_PROBABILITY = 100;

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.trace("Started PBC 'AGRControlActiveDemandSupply'.");
        int randomResult = new Random().nextInt(MAX_PROBABILITY);
        DeviceMessageDto inputDeviceMessage = context.get(IN.DEVICE_MESSAGE_DTO.name(), DeviceMessageDto.class);
        // 1% chance to fail anyway (if randomResult == 0)
        if (!sendDeviceMessageToAds(inputDeviceMessage) || randomResult == 0) {
            context.setValue(OUT.FAILED_DEVICE_MESSAGE_DTO.name(), inputDeviceMessage);
        }
        return context;
    }

    private boolean sendDeviceMessageToAds(DeviceMessageDto deviceMessageDto) {
        LOGGER.trace("Processing device message with endpoint [{}]", deviceMessageDto.getEndpoint());
        LOGGER.trace("Messsage with endpoint [{}] processed successfully and sent to ADS.", deviceMessageDto.getEndpoint());
        return true;
    }
}
