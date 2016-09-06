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

package energy.usef.dso.workflow.operate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.model.PtuState;
import energy.usef.core.model.RegimeType;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.DispositionTypeDto;
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.util.ReflectionUtil;
import energy.usef.dso.workflow.DsoWorkflowStep;
import energy.usef.dso.workflow.dto.GridSafetyAnalysisDto;
import energy.usef.dso.workflow.dto.PtuGridSafetyAnalysisDto;
import energy.usef.dso.workflow.validate.create.flexrequest.DsoCreateFlexRequestCoordinator;

/**
 * Test class in charge of the unit tests related to the {@link DsoCreateFlexRequestCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class DsoOperateCoordinatorTest {

    private static final String CONGESTION_POINT_ENTITY_ADDRESS = "ean.123456789012345678";
    private static final LocalDate PTU_DATE = new LocalDate(2014, 11, 28);

    @Mock
    private Logger LOGGER;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private DsoPlanboardBusinessService planboardBusinessService;
    @Mock
    private WorkflowStepExecuter workflowStubLoader;
    @Mock
    private Config config;
    @Mock
    private ConfigDso configDso;
    @Mock
    private JMSHelperService jmsHelperService;

    private DsoOperateCoordinator coordinator;

    @Before
    public void init() throws Exception {
        coordinator = new DsoOperateCoordinator();
        SequenceGeneratorService sequenceGeneratorService = new SequenceGeneratorService();
        ReflectionUtil.setFinalStatic(DsoOperateCoordinator.class.getDeclaredField("LOGGER"), LOGGER);

        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, planboardBusinessService);
        Whitebox.setInternalState(coordinator, workflowStubLoader);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, configDso);
        Whitebox.setInternalState(coordinator, jmsHelperService);
        Whitebox.setInternalState(coordinator, sequenceGeneratorService);

        PowerMockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(1440);
        PowerMockito.when(config.getProperty(ConfigParam.TIME_ZONE)).thenReturn("Europe/Amsterdam");

        PowerMockito.when(
                planboardBusinessService.findOrCreatePtuState(Matchers.any(PtuContainer.class), Matchers.any(ConnectionGroup.class)))
                .thenReturn(buildPtuState());
        PowerMockito.when(planboardBusinessService
                .findActiveCongestionPointConnectionGroup(Matchers.any(LocalDate.class)))
                .thenReturn(buildCongestionPointConnectionGroups());
        PowerMockito.when(planboardBusinessService.findPtuContainer(Matchers.any(LocalDate.class), Matchers.any(Integer.class)))
                .thenReturn(buildPtuContainer());

        PowerMockito.when(planboardBusinessService.findNewOffers()).thenReturn(buildOffers());

        PowerMockito.when(corePlanboardBusinessService.findPtuFlexOffer(Matchers.any(Long.class), Matchers.any(String.class)))
                .thenReturn(buildFlexOffers().stream().collect(Collectors.toMap(fo -> fo.getPtuContainer().getPtuIndex(),
                        Function.identity())));
        PowerMockito.when(planboardBusinessService
                .findLimitedPower(Matchers.any(PtuContainer.class), Matchers.any(ConnectionGroup.class))).thenReturn(
                Optional.of(10L));

    }

    @Test
    public void testInvokeWorkflow() throws BusinessException {
        WorkflowContext testContext = getWorkflowContext();

        PowerMockito.when(workflowStubLoader.invoke(Matchers.eq(DsoWorkflowStep.DSO_MONITOR_GRID.name()), Mockito.any())).then(
                call -> {
                    WorkflowContext context = (WorkflowContext) call.getArguments()[1];
                    context.setValue(DsoMonitorGridStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name(),
                            CONGESTION_POINT_ENTITY_ADDRESS);
                    context.setValue(DsoMonitorGridStepParameter.IN.PERIOD.name(), PTU_DATE);
                    context.setValue(DsoMonitorGridStepParameter.OUT.ACTUAL_LOAD.name(), 200L);
                    context.setValue(DsoMonitorGridStepParameter.OUT.MAX_LOAD.name(), 200L);
                    context.setValue(DsoMonitorGridStepParameter.OUT.MIN_LOAD.name(), 0L);
                    context.setValue(DsoMonitorGridStepParameter.OUT.CONGESTION.name(), Boolean.TRUE);
                    List<Long> acceptedOffers = new ArrayList<>();
                    acceptedOffers.add(1L);
                    context.setValue("ACCEPTED_FLEXOFFERS", acceptedOffers);
                    return context;
                });
        PowerMockito.when(workflowStubLoader.invoke(Matchers.eq(DsoWorkflowStep.DSO_LIMIT_CONNECTIONS.name()), Mockito.any())).then(
                call -> {
                    WorkflowContext context = (WorkflowContext) call.getArguments()[1];
                    context.setValue(DsoLimitConnectionsStepParameter.OUT.POWER_DECREASE.name(), 100l);
                    return context;
                });
        PowerMockito.when(workflowStubLoader.invoke(Matchers.eq(DsoWorkflowStep.DSO_PLACE_OPERATE_FLEX_ORDERS.name()), Mockito.any()))
                .then(call -> {
                    WorkflowContext context = (WorkflowContext) call.getArguments()[1];
                    context.setValue(PlaceOperateFlexOrdersStepParameter.OUT.ACCEPTED_FLEX_OFFER_DTO_LIST.name(),
                            Collections.singletonList(1l));
                    return context;
                });

        PowerMockito.when(planboardBusinessService.findPtuContainer(Mockito.any(), Mockito.any()))
                .thenReturn(buildPtuContainer(DateTimeUtil.getCurrentDate(), 1));

        PowerMockito.when(planboardBusinessService.findOrderableFlexOffers()).thenReturn(createFlexOfferMap());
        PowerMockito.when(planboardBusinessService.createGridSafetyAnalysisRelatedToFlexOffersDtoMap())
                .thenReturn(createGridSafetyAnalysisMap());

        PowerMockito.when(workflowStubLoader.invoke(Matchers.eq(DsoWorkflowStep.DSO_PLACE_OPERATE_FLEX_ORDERS.name()), Mockito.any())).thenReturn(
                testContext);

        PowerMockito.doNothing().when(corePlanboardBusinessService).storeFlexOrder(Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any());

        PowerMockito.doNothing().when(jmsHelperService).sendMessageToOutQueue(Mockito.any());
        PowerMockito.when(workflowStubLoader.invoke(Matchers.eq(DsoWorkflowStep.DSO_RESTORE_CONNECTIONS.name()), Mockito.any())).thenReturn(null);

        coordinator.sendOperate(new SendOperateEvent());

        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(Matchers.any(String.class));

        Mockito.verify(workflowStubLoader, Mockito.times(1)).invoke(Matchers.eq(DsoWorkflowStep.DSO_LIMIT_CONNECTIONS.name()), Mockito.any());
        Mockito.verify(workflowStubLoader, Mockito.times(0)).invoke(Matchers.eq(DsoWorkflowStep.DSO_RESTORE_CONNECTIONS.name()), Mockito.any());
    }

    @Test
    public void testInvokeWorkflowWithoutOffers() throws BusinessException {
        PowerMockito.when(workflowStubLoader.invoke(Matchers.eq(DsoWorkflowStep.DSO_MONITOR_GRID.name()), Mockito.any())).then(
                call -> {
                    WorkflowContext context = (WorkflowContext) call.getArguments()[1];
                    context.setValue(DsoMonitorGridStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name(),
                            CONGESTION_POINT_ENTITY_ADDRESS);
                    context.setValue(DsoMonitorGridStepParameter.IN.PERIOD.name(), PTU_DATE);
                    context.setValue(DsoMonitorGridStepParameter.OUT.ACTUAL_LOAD.name(), 200L);
                    context.setValue(DsoMonitorGridStepParameter.OUT.MAX_LOAD.name(), 200L);
                    context.setValue(DsoMonitorGridStepParameter.OUT.MIN_LOAD.name(), 0L);
                    context.setValue(DsoMonitorGridStepParameter.OUT.CONGESTION.name(), Boolean.TRUE);
                    List<Long> acceptedOffers = new ArrayList<>();
                    acceptedOffers.add(1L);
                    context.setValue("ACCEPTED_FLEXOFFERS", acceptedOffers);
                    return context;
                });
        PowerMockito.when(workflowStubLoader.invoke(Matchers.eq(DsoWorkflowStep.DSO_LIMIT_CONNECTIONS.name()), Mockito.any())).then(
                call -> {
                    WorkflowContext context = (WorkflowContext) call.getArguments()[1];
                    context.setValue(DsoLimitConnectionsStepParameter.OUT.POWER_DECREASE.name(), 100l);
                    return context;
                });
        PowerMockito.when(workflowStubLoader.invoke(Matchers.eq(DsoWorkflowStep.DSO_PLACE_OPERATE_FLEX_ORDERS.name()), Mockito.any()))
                .then(call -> {
                    WorkflowContext context = (WorkflowContext) call.getArguments()[1];
                    context.setValue(PlaceOperateFlexOrdersStepParameter.OUT.ACCEPTED_FLEX_OFFER_DTO_LIST.name(),
                            Collections.singletonList(1l));
                    return context;
                });

        PowerMockito.when(planboardBusinessService.findOrderableFlexOffers()).thenReturn(new HashMap<>());
        PowerMockito.when(planboardBusinessService.createGridSafetyAnalysisRelatedToFlexOffersDtoMap())
                .thenReturn(createGridSafetyAnalysisMap());
        coordinator.sendOperate(new SendOperateEvent());

        Mockito.verify(jmsHelperService, Mockito.times(0)).sendMessageToOutQueue(Matchers.any(String.class));

        Mockito.verify(workflowStubLoader, Mockito.times(0)).invoke(Matchers.eq(DsoWorkflowStep.DSO_LIMIT_CONNECTIONS.name()), Mockito.any());
        Mockito.verify(workflowStubLoader, Mockito.times(0)).invoke(Matchers.eq(DsoWorkflowStep.DSO_RESTORE_CONNECTIONS.name()), Mockito.any());
    }

    @Test
    public void testInvokeWorkflowWithInvokeRestoreLimitedConnections() throws BusinessException {
        PowerMockito.when(workflowStubLoader.invoke(Matchers.eq(DsoWorkflowStep.DSO_MONITOR_GRID.name()), Mockito.any())).then(
                call -> {
                    WorkflowContext context = (WorkflowContext) call.getArguments()[1];
                    context.setValue(DsoMonitorGridStepParameter.IN.CONGESTION_POINT_ENTITY_ADDRESS.name(),
                            CONGESTION_POINT_ENTITY_ADDRESS);
                    context.setValue(DsoMonitorGridStepParameter.IN.PERIOD.name(), PTU_DATE);
                    context.setValue(DsoMonitorGridStepParameter.OUT.ACTUAL_LOAD.name(), 200L);
                    context.setValue(DsoMonitorGridStepParameter.OUT.MAX_LOAD.name(), 200L);
                    context.setValue(DsoMonitorGridStepParameter.OUT.MIN_LOAD.name(), 0L);
                    context.setValue(DsoMonitorGridStepParameter.OUT.CONGESTION.name(), Boolean.FALSE);
                    List<Long> acceptedOffers = new ArrayList<>();
                    acceptedOffers.add(1L);
                    context.setValue("ACCEPTED_FLEXOFFERS", acceptedOffers);
                    return context;
                });

        coordinator.sendOperate(new SendOperateEvent());

        Mockito.verify(workflowStubLoader, Mockito.times(1)).invoke(Matchers.eq(DsoWorkflowStep.DSO_RESTORE_CONNECTIONS.name()),
                Mockito.any());
    }

    /**
     * @return
     */
    private List<PtuFlexOffer> buildFlexOffers() {
        List<PtuFlexOffer> result = new ArrayList<>();
        PtuFlexOffer flexOffer = new PtuFlexOffer();
        PtuContainer ptuContainer = new PtuContainer();
        ptuContainer.setPtuIndex(1);
        flexOffer.setPtuContainer(ptuContainer);
        flexOffer.setPower(BigInteger.valueOf(1000));
        result.add(flexOffer);
        return result;
    }

    /**
     * @return
     */
    private List<PlanboardMessage> buildOffers() {
        List<PlanboardMessage> result = new ArrayList<>();
        PlanboardMessage offer = new PlanboardMessage();
        CongestionPointConnectionGroup congestionPoint = new CongestionPointConnectionGroup();
        congestionPoint.setUsefIdentifier(CONGESTION_POINT_ENTITY_ADDRESS);
        offer.setConnectionGroup(congestionPoint);
        offer.setSequence(1L);
        offer.setOriginSequence(1L);
        result.add(offer);
        return result;
    }

    /**
     * @return
     */
    private PtuContainer buildPtuContainer() {
        PtuContainer result = new PtuContainer();
        return result;
    }

    /**
     * @return
     */
    private PtuContainer buildPtuContainer(LocalDate date, Integer ptuIndex) {
        PtuContainer result = new PtuContainer();
        result.setPtuDate(date);
        result.setPtuIndex(ptuIndex);
        return result;
    }

    /**
     * @return list of congestion points
     */
    private List<CongestionPointConnectionGroup> buildCongestionPointConnectionGroups() {
        List<CongestionPointConnectionGroup> result = new ArrayList<>();
        CongestionPointConnectionGroup congestionPoint = new CongestionPointConnectionGroup();
        congestionPoint.setUsefIdentifier(CONGESTION_POINT_ENTITY_ADDRESS);
        result.add(congestionPoint);
        return result;
    }

    public Map<String, Map<LocalDate, List<PlanboardMessage>>> createFlexOfferMap() {
        Map<String, Map<LocalDate, List<PlanboardMessage>>> result = new HashMap<>();
        Map<LocalDate, List<PlanboardMessage>> planboardMessageMap = new HashMap<>();
        planboardMessageMap.put(DateTimeUtil.getCurrentDate(), buildOffers());
        result.put(CONGESTION_POINT_ENTITY_ADDRESS, planboardMessageMap);

        return result;
    }

    public Map<String, Map<LocalDate, GridSafetyAnalysisDto>> createGridSafetyAnalysisMap() {
        // Map: Congestion point entity address -> Map: PTU Date -> GridSafetyAnalysisDto
        Map<String, Map<LocalDate, GridSafetyAnalysisDto>> result = new HashMap<>();
        Map<LocalDate, GridSafetyAnalysisDto> gridSafetyAnalysisMap = new HashMap<>();

        GridSafetyAnalysisDto gridSafety = new GridSafetyAnalysisDto();
        gridSafety.setEntityAddress(CONGESTION_POINT_ENTITY_ADDRESS);
        gridSafety.setPtuDate(DateTimeUtil.getCurrentDate());
        gridSafety.setPtus(getPtus());

        gridSafetyAnalysisMap.put(DateTimeUtil.getCurrentDate(), gridSafety);

        result.put(CONGESTION_POINT_ENTITY_ADDRESS, gridSafetyAnalysisMap);

        return result;
    }

    private List<PtuGridSafetyAnalysisDto> getPtus() {
        List<PtuGridSafetyAnalysisDto> ptus = new ArrayList<>();

        ptus.add(createPtuDto(1, DispositionTypeDto.AVAILABLE, 510L));
        ptus.add(createPtuDto(2, DispositionTypeDto.AVAILABLE, 5200L));
        ptus.add(createPtuDto(3, DispositionTypeDto.AVAILABLE, 5300L));
        ptus.add(createPtuDto(4, DispositionTypeDto.AVAILABLE, 5400L));

        return ptus;
    }

    private PtuGridSafetyAnalysisDto createPtuDto(Integer ptuIndex, DispositionTypeDto disposition, Long power) {
        PtuGridSafetyAnalysisDto ptuDto = new PtuGridSafetyAnalysisDto();
        ptuDto.setPtuIndex(ptuIndex);
        ptuDto.setDisposition(disposition);
        ptuDto.setPower(power);

        return ptuDto;
    }

    private WorkflowContext getWorkflowContext() {
        WorkflowContext result = new DefaultWorkflowContext();

        List<FlexOfferDto> flexOfferList = new ArrayList<>();

        FlexOfferDto flexOffer = new FlexOfferDto();
        flexOffer.setConnectionGroupEntityAddress(CONGESTION_POINT_ENTITY_ADDRESS);
        flexOffer.setPeriod(DateTimeUtil.getCurrentDate());
        flexOffer.setSequenceNumber(1L);
        flexOffer.setFlexRequestSequenceNumber(1L);

        flexOfferList.add(flexOffer);
        result.setValue(PlaceOperateFlexOrdersStepParameter.OUT.ACCEPTED_FLEX_OFFER_DTO_LIST.name(), flexOfferList);
        result.setValue(DsoLimitConnectionsStepParameter.OUT.POWER_DECREASE.name(), 1000L);

        return result;
    }

    private PtuState buildPtuState() {
        PtuState ptuState = new PtuState();
        ptuState.setRegime(RegimeType.ORANGE);
        return ptuState;
    }
}
