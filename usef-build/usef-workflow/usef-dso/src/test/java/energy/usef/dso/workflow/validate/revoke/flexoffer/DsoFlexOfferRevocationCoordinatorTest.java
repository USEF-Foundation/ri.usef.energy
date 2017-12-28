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

package energy.usef.dso.workflow.validate.revoke.flexoffer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.FlexOfferRevocation;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuContainerState;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.model.PtuState;
import energy.usef.core.repository.PtuStateRepository;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.service.validation.CorePlanboardValidatorService;

import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.dso.workflow.DsoWorkflowStep;
import energy.usef.dso.workflow.validate.revoke.flexoffer.RevokeFlexOfferStepParameter.IN;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Test class in charge of the unit tests related to the {@link DsoFlexOfferRevocationCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class DsoFlexOfferRevocationCoordinatorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DsoFlexOfferRevocationCoordinatorTest.class);

    private DsoFlexOfferRevocationCoordinator coordinator;

    @Mock
    private JMSHelperService jmsHelperService;
    @Mock
    private CorePlanboardBusinessService planboardBusinessService;
    @Spy
    private CorePlanboardValidatorService planboardValidatorService = new CorePlanboardValidatorService();
    @Mock
    private PtuStateRepository ptuStateRepository;
    @Mock
    private Config config;
    @Mock
    private WorkflowStepExecuter workflowStepExecuter;

    @Before
    public void init() {
        coordinator = new DsoFlexOfferRevocationCoordinator();

        MockitoAnnotations.initMocks(this);

        Whitebox.setInternalState(coordinator, jmsHelperService);
        Whitebox.setInternalState(coordinator, planboardBusinessService);
        Whitebox.setInternalState(coordinator, planboardValidatorService);
        Whitebox.setInternalState(planboardValidatorService, ptuStateRepository);
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, workflowStepExecuter);

        PowerMockito.when(config.getProperty(Matchers.eq(ConfigParam.HOST_DOMAIN))).thenReturn("usef-example.com");
    }

    @Test
    public void testInvokeWorkflowSucceeds() throws XpathException, SAXException, IOException {
        PowerMockito.when(planboardBusinessService.findPtuFlexOffer(any(Long.class), any(String.class)))
                .thenReturn(buildFlexOffers().stream().collect(Collectors.toMap(fo -> fo.getPtuContainer().getPtuIndex(),
                        Function.identity())));

        PtuState ptuState = new PtuState();
        ptuState.setState(PtuContainerState.PlanValidate);

        PowerMockito.when(
                ptuStateRepository.findOrCreatePtuState(any(PtuContainer.class), any(ConnectionGroup.class)))
                .thenReturn(ptuState);

        PowerMockito.when(planboardBusinessService.findPlanboardMessages(any(Long.class), any(DocumentType.class),
                        Matchers.anyString())).
                thenReturn(buildPlanboardMessageList());

        coordinator.handleEvent(new FlexOfferRevocationEvent(buildContext()));

        try {
            Mockito.verify(planboardValidatorService, Mockito.times(1)).checkPtuPhase(any(FlexOfferRevocation.class),
                    Matchers.anyMapOf(Integer.class, PtuFlexOffer.class));
        } catch (BusinessValidationException e) {
            fail("No BusinessValidationException expected");
        }

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(messageCaptor.capture());
        String responseMessage = messageCaptor.getValue();
        Assert.assertNotNull(responseMessage);
        XMLAssert.assertXpathEvaluatesTo("Accepted", "/FlexOfferRevocationResponse/@Result", responseMessage);

        ArgumentCaptor<WorkflowContext> workflowContext = ArgumentCaptor.forClass(WorkflowContext.class);
        Mockito.verify(workflowStepExecuter, Mockito.timeout(1000).times(1)).invoke(Mockito.eq(DsoWorkflowStep.DSO_FLEX_OFFER_REVOCATION.name()),
                workflowContext.capture());
        WorkflowContext context = workflowContext.getValue();
        assertNotNull(context.getValue(IN.FLEX_OFFER_DTO.name()));
        assertNotNull(context.getValue(IN.AGGREGATOR.name()));

        LOGGER.info("Response message:\n{}", responseMessage);
    }

    @Test
    public void testInvokeWorkflowOrdered() throws XpathException, SAXException, IOException {
        PowerMockito.when(planboardBusinessService.findPtuFlexOffer(any(Long.class), any(String.class)))
                .thenReturn(buildFlexOffers().stream().collect(Collectors.toMap(fo -> fo.getPtuContainer().getPtuIndex(),
                        Function.identity())));

        PtuState ptuState = new PtuState();
        ptuState.setState(PtuContainerState.PlanValidate);

        PowerMockito.when(
                ptuStateRepository.findOrCreatePtuState(any(PtuContainer.class), any(ConnectionGroup.class)))
                .thenReturn(ptuState);

        PowerMockito.when(planboardBusinessService.findPlanboardMessages(any(Long.class), any(DocumentType.class),
                Matchers.anyString())).
                thenReturn(buildPlanboardMessageList());
        //used to find orders
        PowerMockito.when(planboardBusinessService.findPlanboardMessagesWithOriginSequence(any(Long.class), any(DocumentType.class),
                Matchers.anyString())).
                thenReturn(buildPlanboardMessageList());

        coordinator.handleEvent(new FlexOfferRevocationEvent(buildContext()));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(messageCaptor.capture());
        String responseMessage = messageCaptor.getValue();
        Assert.assertNotNull(responseMessage);
        XMLAssert.assertXpathEvaluatesTo("Rejected", "/FlexOfferRevocationResponse/@Result", responseMessage);

        LOGGER.info("Response message:\n{}", responseMessage);
    }


    @Test
    public void testInvokeWorkflowFailsForNoPlanboardMessage() throws XpathException, SAXException, IOException {
        PowerMockito.when(planboardBusinessService.findPtuFlexOffer(any(Long.class), any(String.class)))
                .thenReturn(buildFlexOffers().stream().collect(Collectors.toMap(fo -> fo.getPtuContainer().getPtuIndex(),
                        Function.identity())));

        PowerMockito.when(planboardBusinessService.findPlanboardMessages(any(Long.class), any(DocumentType.class),
                        Matchers.anyString())).
                thenReturn(new ArrayList<>());

        coordinator.handleEvent(new FlexOfferRevocationEvent(buildContext()));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(messageCaptor.capture());
        String responseMessage = messageCaptor.getValue();
        Assert.assertNotNull(responseMessage);
        XMLAssert.assertXpathEvaluatesTo("Rejected", "/FlexOfferRevocationResponse/@Result", responseMessage);

        LOGGER.info("Response message:\n{}", responseMessage);
    }

    @Test
    public void testInvokeWorkflowFailsForFlexOfferInOperatePhase() throws XpathException, SAXException, IOException {
        List<PlanboardMessage> planboardMessages = buildPlanboardMessageList();

        PtuState ptuState = new PtuState();
        ptuState.setState(PtuContainerState.Operate);

        PowerMockito.when(planboardBusinessService.findPtuFlexOffer(any(Long.class), any(String.class)))
                .thenReturn(buildFlexOffers().stream().collect(Collectors.toMap(fo -> fo.getPtuContainer().getPtuIndex(),
                        Function.identity())));

        PowerMockito.when(
                ptuStateRepository.findOrCreatePtuState(any(PtuContainer.class), any(ConnectionGroup.class)))
                .thenReturn(ptuState);

        PowerMockito.when(planboardBusinessService.findPlanboardMessages(any(Long.class), any(DocumentType.class),
                        Matchers.anyString())).
                thenReturn(planboardMessages);

        coordinator.handleEvent(new FlexOfferRevocationEvent(buildContext()));

        try {
            Mockito.verify(planboardValidatorService, Mockito.times(1)).checkPtuPhase(any(FlexOfferRevocation.class),
                    Matchers.anyMapOf(Integer.class, PtuFlexOffer.class));
        } catch (BusinessValidationException e) {
            fail("No BusinessValidationException expected");
        }

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(messageCaptor.capture());
        String responseMessage = messageCaptor.getValue();
        Assert.assertNotNull(responseMessage);
        XMLAssert.assertXpathEvaluatesTo("Rejected", "/FlexOfferRevocationResponse/@Result", responseMessage);

        LOGGER.info("Response message:\n{}", responseMessage);
    }

    @Test
    public void testInvokeWorkflowFailsForNoFlexOffer() throws XpathException, SAXException, IOException {
        PowerMockito.when(planboardBusinessService.findPtuFlexOffer(any(Long.class), any(String.class)))
                .thenReturn(new HashMap<Integer, PtuFlexOffer>());

        coordinator.handleEvent(new FlexOfferRevocationEvent(buildContext()));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(messageCaptor.capture());
        String responseMessage = messageCaptor.getValue();
        Assert.assertNotNull(responseMessage);
        XMLAssert.assertXpathEvaluatesTo("Rejected", "/FlexOfferRevocationResponse/@Result", responseMessage);

        LOGGER.info("Response message:\n{}", responseMessage);
    }

    private FlexOfferRevocation buildContext() {
        FlexOfferRevocation flexOfferRevocation = new FlexOfferRevocation();
        flexOfferRevocation.setMessageMetadata(new MessageMetadataBuilder().messageID().conversationID().timeStamp()
                .senderDomain("sender-usef-example.com").senderRole(USEFRole.AGR)
                .recipientDomain("recipient-usef-example.com").recipientRole(USEFRole.DSO).build());
        return flexOfferRevocation;
    }

    private List<PtuFlexOffer> buildFlexOffers() {
        PtuFlexOffer fo1 = new PtuFlexOffer();
        fo1.setPower(BigInteger.TEN);
        fo1.setPrice(BigDecimal.ZERO);
        CongestionPointConnectionGroup cg = new CongestionPointConnectionGroup();
        cg.setUsefIdentifier("senderd");
        fo1.setConnectionGroup(cg);
        PtuContainer ptuc1 = new PtuContainer();
        ptuc1.setPtuIndex(1);
        fo1.setPtuContainer(ptuc1);
        return Collections.singletonList(fo1);
    }

    private List<PlanboardMessage> buildPlanboardMessageList() {
        List<PlanboardMessage> planboardMessages = new ArrayList<>();
        PlanboardMessage planboardMessage = new PlanboardMessage();
        planboardMessage.setDocumentStatus(DocumentStatus.ACCEPTED);
        planboardMessages.add(planboardMessage);

        return planboardMessages;
    }

}
