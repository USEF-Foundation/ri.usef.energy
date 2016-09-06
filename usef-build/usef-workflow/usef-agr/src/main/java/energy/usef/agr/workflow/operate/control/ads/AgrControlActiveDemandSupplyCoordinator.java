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

package energy.usef.agr.workflow.operate.control.ads;

import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_CONTROL_ACTIVE_DEMAND_SUPPLY;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.dto.device.request.DeviceMessageDto;
import energy.usef.agr.model.DeviceMessage;
import energy.usef.agr.model.DeviceMessageStatus;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.agr.transformer.DeviceMessageTransformer;
import energy.usef.agr.workflow.operate.control.ads.ControlActiveDemandSupplyStepParameter.IN;
import energy.usef.agr.workflow.operate.control.ads.ControlActiveDemandSupplyStepParameter.OUT;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.exception.TechnicalException;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main coordinator class for the workflow 'Realize A-Plans and/or D-Prognoses by controlling Active Demand and Supply'.
 * <p>
 * This coordinator is stateless, because of the
 */
@Stateless
public class AgrControlActiveDemandSupplyCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrControlActiveDemandSupplyCoordinator.class);

    private static final int THREAD_POOL_SIZE = 10;

    @Inject
    private Config config;
    @Inject
    private ConfigAgr configAgr;
    @Inject
    private WorkflowStepExecuter workflowStepExecuter;
    @Inject
    private AgrPortfolioBusinessService agrPortfolioBusinessService;
    @Inject
    private Event<ControlActiveDemandSupplyEvent> eventManager;

    /**
     * Handles the ControlActiveDemandSupplyEvent.
     *
     * @param event A {@link ControlActiveDemandSupplyEvent}
     */
    @Asynchronous
    public void controlActiveDemandSupply(
            @Observes(during = TransactionPhase.AFTER_COMPLETION) ControlActiveDemandSupplyEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);

        // pass each UDI Message Control to the PBC, using the thread pool.
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<DeviceMessage> deviceMessages = agrPortfolioBusinessService.findDeviceMessages(null, DeviceMessageStatus.NEW);
        deviceMessages.forEach(deviceMessage -> executorService.submit(() -> handleUdiControlMessage(deviceMessage)));

        // shutdown and wait for the tasks to be terminated or reach a timeout.
        executorService.shutdown();
        try {
            executorService.awaitTermination(configAgr.getIntegerProperty(ConfigAgrParam.AGR_CONTROL_ADS_TIMEOUT_IN_SECONDS),
                    TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new TechnicalException(e.getMessage(), e);
        }
        LOGGER.info("[{}] UDI Device Messages have been processed in the different threads.", deviceMessages.size());

        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private void handleUdiControlMessage(DeviceMessage deviceMessage) {
        agrPortfolioBusinessService.updateDeviceMessageStatus(deviceMessage, DeviceMessageStatus.IN_PROCESS);

        WorkflowContext context = new DefaultWorkflowContext();
        int ptuDuration = config.getIntegerProperty(ConfigParam.PTU_DURATION);

        context.setValue(IN.PTU_DURATION.name(), ptuDuration);
        context.setValue(IN.DEVICE_MESSAGE_DTO.name(), DeviceMessageTransformer.transformToDto(deviceMessage));

        WorkflowContext returnedContext = workflowStepExecuter.invoke(AGR_CONTROL_ACTIVE_DEMAND_SUPPLY.name(), context);
        DeviceMessageDto returnedDeviceMessage = returnedContext.get(OUT.FAILED_DEVICE_MESSAGE_DTO.name(), DeviceMessageDto.class);

        if (returnedDeviceMessage != null) {
            // log the failure
            LOGGER.warn("Impossible to deliver the Device Message to endpoint [{}].", returnedDeviceMessage.getEndpoint());
            agrPortfolioBusinessService.updateDeviceMessageStatus(deviceMessage, DeviceMessageStatus.FAILURE);
        } else {
            agrPortfolioBusinessService.updateDeviceMessageStatus(deviceMessage, DeviceMessageStatus.SENT);
        }
    }
}
