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

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.dto.device.request.DeviceMessageDto;
import energy.usef.agr.model.DeviceMessage;
import energy.usef.agr.model.DeviceMessageStatus;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import javax.enterprise.event.Event;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link AgrControlActiveDemandSupplyCoordinator} class.
 */
@RunWith(PowerMockRunner.class)
public class AgrControlActiveDemandSupplyCoordinatorTest {

    private AgrControlActiveDemandSupplyCoordinator coordinator;

    @Mock
    private WorkflowStepExecuter workflowStepExecuter;

    @Mock
    private Event<ControlActiveDemandSupplyEvent> eventManager;

    @Mock
    private AgrPortfolioBusinessService agrPortfolioBusinessService;

    @Mock
    private ConfigAgr configAgr;

    @Mock
    private Config config;

    @Before
    public void init() {
        coordinator = new AgrControlActiveDemandSupplyCoordinator();
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, workflowStepExecuter);
        Whitebox.setInternalState(coordinator, agrPortfolioBusinessService);
        Whitebox.setInternalState(coordinator, eventManager);
        Whitebox.setInternalState(coordinator, configAgr);
        PowerMockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);
        PowerMockito.when(configAgr.getIntegerProperty(ConfigAgrParam.AGR_CONTROL_ADS_TIMEOUT_IN_SECONDS)).thenReturn(10);
        PowerMockito.when(config.getProperty(ConfigParam.TIME_ZONE)).thenReturn("EUROPE/AMSTERDAM");
        PowerMockito.when(
                agrPortfolioBusinessService.findDeviceMessages(Matchers.any(String.class),
                        Matchers.any(DeviceMessageStatus.class))).then(invocation -> {
            DeviceMessage deviceMessage = new DeviceMessage();
            deviceMessage.setDeviceMessageStatus((DeviceMessageStatus) invocation.getArguments()[1]);
            return Collections.singletonList(deviceMessage);
        });
    }

    @Test
    public void testInvokeWithSuccessResult() {
        ArgumentCaptor<WorkflowContext> contextCaptor = ArgumentCaptor.forClass(WorkflowContext.class);
        ArgumentCaptor<DeviceMessageStatus> statusCaptor = ArgumentCaptor.forClass(DeviceMessageStatus.class);
        Mockito.when(workflowStepExecuter.invoke(Mockito.eq(AGR_CONTROL_ACTIVE_DEMAND_SUPPLY.name()),
                contextCaptor.capture())).thenReturn(buildResultContext(null));

        coordinator.controlActiveDemandSupply(new ControlActiveDemandSupplyEvent());

        Mockito.verify(workflowStepExecuter, Mockito.times(1)).invoke(
                Mockito.eq(AGR_CONTROL_ACTIVE_DEMAND_SUPPLY.name()),
                contextCaptor.capture());

        // verify that we updated the status of the Device Message twice with the right values.
        Mockito.verify(agrPortfolioBusinessService, Mockito.times(2)).updateDeviceMessageStatus(
                Matchers.any(DeviceMessage.class),
                statusCaptor.capture());
        Assert.assertEquals(2, statusCaptor.getAllValues().size());
        Assert.assertEquals(DeviceMessageStatus.IN_PROCESS, statusCaptor.getAllValues().get(0));
        Assert.assertEquals(DeviceMessageStatus.SENT, statusCaptor.getAllValues().get(1));

    }

    @Test
    public void testInvokeWithFailureResult() {
        ArgumentCaptor<DeviceMessageStatus> statusCaptor = ArgumentCaptor.forClass(DeviceMessageStatus.class);
        Mockito.when(workflowStepExecuter.invoke(Mockito.eq(AGR_CONTROL_ACTIVE_DEMAND_SUPPLY.name()),
                Mockito.any())).thenReturn(buildResultContext(new DeviceMessageDto()));
        coordinator.controlActiveDemandSupply(new ControlActiveDemandSupplyEvent());
        Mockito.verify(workflowStepExecuter, Mockito.times(1)).invoke(
                Mockito.eq(AGR_CONTROL_ACTIVE_DEMAND_SUPPLY.name()),
                Mockito.any());

        // verify that we updated the status of the Device Message twice with the right values.
        Mockito.verify(agrPortfolioBusinessService, Mockito.times(2)).updateDeviceMessageStatus(
                Matchers.any(DeviceMessage.class),
                statusCaptor.capture());
        Assert.assertEquals(2, statusCaptor.getAllValues().size());
        Assert.assertEquals(DeviceMessageStatus.IN_PROCESS, statusCaptor.getAllValues().get(0));
        Assert.assertEquals(DeviceMessageStatus.FAILURE, statusCaptor.getAllValues().get(1));
    }

    private WorkflowContext buildResultContext(DeviceMessageDto outputDeviceMessageDto) {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(ControlActiveDemandSupplyStepParameter.OUT.FAILED_DEVICE_MESSAGE_DTO.name(), outputDeviceMessageDto);
        return context;
    }

    private List<PtuPrognosis> buildDPrognosisList() {
        List<PtuPrognosis> dprognoses = new ArrayList<>();
        IntStream.rangeClosed(1, 96).mapToObj(i -> newDPrognosis(i, 1l, "dso1.usef-example.com")).forEach(dprognoses::add);
        IntStream.rangeClosed(1, 96).mapToObj(i -> newDPrognosis(i, 1l, "dso2.usef-example.com")).forEach(dprognoses::add);
        return dprognoses;
    }

    private List<PtuPrognosis> buildAPlanList() {
        List<PtuPrognosis> aplans = new ArrayList<>();
        IntStream.rangeClosed(1, 96).mapToObj(i -> newAPlan(i, 1l, "brp1.usef-example.com")).forEach(aplans::add);
        IntStream.rangeClosed(1, 96).mapToObj(i -> newAPlan(i, 1l, "brp2.usef-example.com")).forEach(aplans::add);
        return aplans;
    }

    private PtuPrognosis newDPrognosis(Integer ptuIndex, Long sequence, String domain) {
        ConnectionGroup connectionGroup = new CongestionPointConnectionGroup();
        connectionGroup.setUsefIdentifier(domain);
        PtuContainer ptuContainer = new PtuContainer();
        ptuContainer.setPtuIndex(ptuIndex);
        ptuContainer.setPtuDate(DateTimeUtil.getCurrentDate());

        PtuPrognosis ptuPrognosis = new PtuPrognosis();
        ptuPrognosis.setConnectionGroup(connectionGroup);
        ptuPrognosis.setPtuContainer(ptuContainer);
        ptuPrognosis.setSequence(sequence);
        ptuPrognosis.setParticipantDomain(domain);
        ptuPrognosis.setType(PrognosisType.D_PROGNOSIS);
        ptuPrognosis.setPower(BigInteger.valueOf(500));
        return ptuPrognosis;
    }

    private PtuPrognosis newAPlan(Integer ptuIndex, Long sequence, String domain) {
        ConnectionGroup connectionGroup = new BrpConnectionGroup();
        connectionGroup.setUsefIdentifier(domain);
        PtuContainer ptuContainer = new PtuContainer();
        ptuContainer.setPtuIndex(ptuIndex);

        PtuPrognosis ptuPrognosis = new PtuPrognosis();
        ptuPrognosis.setConnectionGroup(connectionGroup);
        ptuPrognosis.setPtuContainer(ptuContainer);
        ptuPrognosis.setSequence(sequence);
        ptuPrognosis.setParticipantDomain(domain);
        ptuPrognosis.setType(PrognosisType.A_PLAN);
        ptuPrognosis.setPower(BigInteger.valueOf(500));
        return ptuPrognosis;
    }
}
