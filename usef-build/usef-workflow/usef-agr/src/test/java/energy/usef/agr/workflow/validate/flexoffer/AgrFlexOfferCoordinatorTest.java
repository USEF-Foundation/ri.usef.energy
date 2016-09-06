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

package energy.usef.agr.workflow.validate.flexoffer;

import static energy.usef.agr.workflow.AgrWorkflowStep.AGR_FLEX_OFFER_DETERMINE_FLEXIBILITY;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.service.business.AgrPlanboardBusinessService;
import energy.usef.agr.service.business.AgrPortfolioBusinessService;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DispositionAvailableRequested;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.model.PtuFlexRequest;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.core.workflow.dto.PtuFlexOfferDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
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

import energy.usef.core.data.xml.bean.message.FlexOffer;

/**
 * Unit test for {@link AgrFlexOfferCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class AgrFlexOfferCoordinatorTest {

    private AgrFlexOfferCoordinator agrFlexOfferCoordinator;

    @Mock
    private WorkflowStepExecuter workflowStepExecuter;
    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Mock
    private AgrPlanboardBusinessService agrPlanboardBusinessService;
    @Mock
    private AgrPortfolioBusinessService agrPortfolioBusinessService;
    @Mock
    private JMSHelperService jmsHelperService;
    @Mock
    private Config config;
    @Mock
    private ConfigAgr configAgr;
    @Mock
    private CorePlanboardValidatorService planboardValidatorService;

    @Before
    public void prepareTest() {
        agrFlexOfferCoordinator = new AgrFlexOfferCoordinator();
        SequenceGeneratorService sequenceGeneratorService = new SequenceGeneratorService();
        Whitebox.setInternalState(agrFlexOfferCoordinator, workflowStepExecuter);
        Whitebox.setInternalState(agrFlexOfferCoordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(agrFlexOfferCoordinator, agrPlanboardBusinessService);
        Whitebox.setInternalState(agrFlexOfferCoordinator, agrPortfolioBusinessService);
        Whitebox.setInternalState(agrFlexOfferCoordinator, jmsHelperService);
        Whitebox.setInternalState(agrFlexOfferCoordinator, config);
        Whitebox.setInternalState(agrFlexOfferCoordinator, configAgr);
        Whitebox.setInternalState(agrFlexOfferCoordinator, planboardValidatorService);
        Whitebox.setInternalState(agrFlexOfferCoordinator, sequenceGeneratorService);
        PowerMockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);

        Mockito.when(corePlanboardBusinessService.findSinglePlanboardMessage(Mockito.anyLong(), Mockito.any(), Mockito.any()))
                .thenReturn(new PlanboardMessage());
    }

    @Test
    public void testInvokeWorkflowWithDso() throws BusinessException {
        List<PlanboardMessage> requestList = new ArrayList<>();
        PlanboardMessage request = buildDsoFlexRequest();
        requestList.add(request);
        // outcontext of workflow
        WorkflowContext outContext = new DefaultWorkflowContext();
        List<FlexOfferDto> flexOffers = buildResultingFlexOffers(USEFRole.DSO);
        outContext.setValue(FlexOfferDetermineFlexibilityStepParameter.OUT.FLEX_OFFER_DTO_LIST.name(), flexOffers);

        Mockito.when(agrPlanboardBusinessService.findLastFlexRequestDocumentWithDispositionRequested(Matchers.any(String.class),
                Matchers.any(LocalDate.class), Matchers.any(Long.class))).thenReturn(new PtuFlexRequest());
        Mockito.when(agrPlanboardBusinessService.findAcceptedRequests(Matchers.eq(DocumentType.FLEX_REQUEST)))
                .thenReturn(requestList);
        Mockito.when(
                agrPlanboardBusinessService.findPtuFlexRequestWithSequence(Matchers.any(String.class), Matchers.any(Long.class),
                Matchers.any(String.class))).then(invocation -> {
            CongestionPointConnectionGroup congestionPoint = new CongestionPointConnectionGroup();
            congestionPoint.setUsefIdentifier("ean.123456789012345678");
            return IntStream.rangeClosed(1, 96).mapToObj(index -> {
                PtuContainer ptuContainer = new PtuContainer();
                ptuContainer.setPtuIndex(index);
                ptuContainer.setPtuDate(DateTimeUtil.getCurrentDate());
                PtuFlexRequest ptuFlexRequest = new PtuFlexRequest();
                ptuFlexRequest.setPower(BigInteger.TEN);
                ptuFlexRequest.setDisposition(DispositionAvailableRequested.REQUESTED);
                ptuFlexRequest.setSequence((Long) invocation.getArguments()[1]);
                ptuFlexRequest.setParticipantDomain((String) invocation.getArguments()[2]);
                ptuFlexRequest.setConnectionGroup(congestionPoint);
                ptuFlexRequest.setPtuContainer(ptuContainer);
                return ptuFlexRequest;
            }).collect(Collectors.toList());
        });

        ArgumentCaptor<WorkflowContext> workflowIn = ArgumentCaptor.forClass(WorkflowContext.class);
        Mockito.when(workflowStepExecuter.invoke(Mockito.eq(AGR_FLEX_OFFER_DETERMINE_FLEXIBILITY.name()), Mockito.any()))
                .thenReturn(outContext);

        // execute test
        agrFlexOfferCoordinator.handleEvent(new FlexOfferEvent());

        Mockito.verify(agrPlanboardBusinessService, Mockito.times(1)).findAcceptedRequests(Matchers.eq(DocumentType.FLEX_REQUEST));
        Mockito.verify(corePlanboardBusinessService, Mockito.times(1))
                .findLastPrognoses(Matchers.eq(request.getPeriod()), Matchers.eq(PrognosisType.A_PLAN));

        // verify workflowstep
        Mockito.verify(workflowStepExecuter, Mockito.times(1)).invoke(
                Mockito.eq(AGR_FLEX_OFFER_DETERMINE_FLEXIBILITY.name()), workflowIn.capture());
        Assert.assertNotNull(
                workflowIn.getValue().getValue(FlexOfferDetermineFlexibilityStepParameter.IN.FLEX_REQUEST_DTO_LIST.name()));

        // verify execution of processing results
        Mockito.verify(planboardValidatorService, Mockito.times(1))
                .isPlanboardItemWithingIntradayGateClosureTime(Matchers.any(PtuFlexOffer.class));
        Mockito.verify(corePlanboardBusinessService, Mockito.times(1))
                .storeFlexOffer(Mockito.anyString(), Matchers.any(FlexOffer.class),
                        Matchers.eq(DocumentStatus.SENT), Matchers.anyString());
        Mockito.verify(jmsHelperService, Mockito.times(1))
                .sendMessageToOutQueue(Matchers.contains(request.getConnectionGroup().getUsefIdentifier()));

    }

    @Test
    public void testInvokeWorkflowWithBrp() throws BusinessException {
        List<PlanboardMessage> requestList = new ArrayList<>();
        PlanboardMessage request = buildBrpFlexRequest();
        requestList.add(request);
        // outcontext of workflow
        WorkflowContext outContext = new DefaultWorkflowContext();
        List<FlexOfferDto> flexOffers = buildResultingFlexOffers(USEFRole.BRP);
        outContext.setValue(FlexOfferDetermineFlexibilityStepParameter.OUT.FLEX_OFFER_DTO_LIST.name(), flexOffers);

        Mockito.when(agrPlanboardBusinessService.findLastFlexRequestDocumentWithDispositionRequested(Matchers.any(String.class),
                Matchers.any(LocalDate.class), Matchers.any(Long.class))).thenReturn(new PtuFlexRequest());
        Mockito.when(agrPlanboardBusinessService.findAcceptedRequests(Matchers.eq(DocumentType.FLEX_REQUEST)))
                .thenReturn(requestList);

        ArgumentCaptor<WorkflowContext> workflowIn = ArgumentCaptor.forClass(WorkflowContext.class);
        Mockito.when(workflowStepExecuter.invoke(Mockito.eq(AGR_FLEX_OFFER_DETERMINE_FLEXIBILITY.name()), Mockito.any()))
                .thenReturn(outContext);

        // execute test
        agrFlexOfferCoordinator.handleEvent(new FlexOfferEvent());

        Mockito.verify(agrPlanboardBusinessService, Mockito.times(1)).findAcceptedRequests(Matchers.eq(DocumentType.FLEX_REQUEST));
        Mockito.verify(corePlanboardBusinessService, Mockito.times(1))
                .findLastPrognoses(Matchers.eq(request.getPeriod()), Matchers.eq(PrognosisType.A_PLAN));

        // verify workflowstep
        Mockito.verify(workflowStepExecuter, Mockito.times(1)).invoke(
                Mockito.eq(AGR_FLEX_OFFER_DETERMINE_FLEXIBILITY.name()), workflowIn.capture());
        Assert.assertNotNull(
                workflowIn.getValue().getValue(FlexOfferDetermineFlexibilityStepParameter.IN.FLEX_REQUEST_DTO_LIST.name()));
        // verify execution of processing results
        Mockito.verify(planboardValidatorService, Mockito.times(1))
                .isPlanboardItemWithingIntradayGateClosureTime(Matchers.any(PtuFlexOffer.class));
        Mockito.verify(corePlanboardBusinessService, Mockito.times(1))
                .storeFlexOffer(Mockito.anyString(), Matchers.any(FlexOffer.class),
                        Matchers.eq(DocumentStatus.SENT), Matchers.anyString());
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(Matchers.any(String.class));

    }

    private PlanboardMessage buildDsoFlexRequest() {
        // build data
        ConnectionGroup connectionGroup = new CongestionPointConnectionGroup();
        connectionGroup.setUsefIdentifier("ean.123456789012345678");

        PlanboardMessage request = new PlanboardMessage();
        request.setConnectionGroup(connectionGroup);
        request.setPeriod(DateTimeUtil.getCurrentDate());
        request.setDocumentStatus(DocumentStatus.ACCEPTED);
        request.setSequence(2l);
        request.setExpirationDate(DateTimeUtil.getCurrentDateTime().plusDays(4));
        return request;
    }

    private PlanboardMessage buildBrpFlexRequest() {
        // build data
        ConnectionGroup connectionGroup = new BrpConnectionGroup();
        connectionGroup.setUsefIdentifier("test.brp.usef-example.com");

        PlanboardMessage request = new PlanboardMessage();
        request.setParticipantDomain(connectionGroup.getUsefIdentifier());
        request.setConnectionGroup(connectionGroup);
        request.setPeriod(DateTimeUtil.getCurrentDate());
        request.setDocumentStatus(DocumentStatus.ACCEPTED);
        request.setSequence(2l);
        request.setExpirationDate(DateTimeUtil.getCurrentDateTime().plusDays(4));
        return request;
    }

    private List<FlexOfferDto> buildResultingFlexOffers(USEFRole usefRole) {
        Random random = new Random();
        List<FlexOfferDto> flexOffers = new ArrayList<>();
        FlexOfferDto flexOffer = new FlexOfferDto();
        flexOffer.setFlexRequestSequenceNumber(2l);
        flexOffer.setPeriod(DateTimeUtil.getCurrentDate());
        flexOffer.setExpirationDateTime(DateTimeUtil.getCurrentDateTime().plusHours(4));
        flexOffer.setConnectionGroupEntityAddress(
                usefRole == USEFRole.DSO ? "ean.123456789012345678" : "test.brp.usef-example.com");
        flexOffer.setSequenceNumber(3l);
        IntStream.rangeClosed(1, 96).mapToObj(index -> {
            PtuFlexOfferDto ptuFlexOfferDto = new PtuFlexOfferDto();
            ptuFlexOfferDto.setPtuIndex(BigInteger.valueOf(index));
            ptuFlexOfferDto.setPower(BigInteger.valueOf(random.nextInt(1000)));
            ptuFlexOfferDto.setPrice(BigDecimal.valueOf(random.nextDouble()).multiply(new BigDecimal(ptuFlexOfferDto.getPower())));
            return ptuFlexOfferDto;
        }).forEach(ptu -> flexOffer.getPtus().add(ptu));
        flexOffers.add(flexOffer);
        return flexOffers;
    }

}
