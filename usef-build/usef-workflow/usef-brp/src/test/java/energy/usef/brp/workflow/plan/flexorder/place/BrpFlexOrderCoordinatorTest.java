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

package energy.usef.brp.workflow.plan.flexorder.place;

import energy.usef.brp.config.ConfigBrp;
import energy.usef.brp.service.business.BrpPlanboardBusinessService;
import energy.usef.brp.workflow.BrpWorkflowStep;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.model.AgrConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.step.WorkflowStepExecuter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class in charge of the unit tests related to the {@link BrpFlexOrderCoordinator} class.
 */
@RunWith(PowerMockRunner.class)
public class BrpFlexOrderCoordinatorTest {

    private static final String AGR1_DOMAIN = "agr1.usef-example.com";
    private static final String AGR2_DOMAIN = "agr2.usef-example.com";

    @Mock
    private Config config;
    @Mock
    private ConfigBrp configBrp;
    @Mock
    private BrpPlanboardBusinessService brpPlanboardBusinessService;
    @Mock
    private JMSHelperService jmsHelperService;
    @Mock
    private WorkflowStepExecuter workflowStubLoader;
    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    private BrpFlexOrderCoordinator coordinator;

    private static final Logger LOGGER = LoggerFactory.getLogger(BrpFlexOrderCoordinatorTest.class);

    @Before
    public void init() {
        coordinator = new BrpFlexOrderCoordinator();
        SequenceGeneratorService sequenceGeneratorService = new SequenceGeneratorService();
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, configBrp);
        Whitebox.setInternalState(coordinator, brpPlanboardBusinessService);
        Whitebox.setInternalState(coordinator, jmsHelperService);
        Whitebox.setInternalState(coordinator, workflowStubLoader);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, sequenceGeneratorService);

        PowerMockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);
    }

    @Test
    public void testHandleEventPlacesOneOrder() {
        // stubbing of the brpPlanboardBusinessService
        PowerMockito.when(brpPlanboardBusinessService.findOrderableFlexOffers()).thenReturn(buildProcessableFlexOffers());
        PowerMockito.when(corePlanboardBusinessService
                .findLastPrognoses(Matchers.any(LocalDate.class), Matchers.eq(PrognosisType.A_PLAN), Matchers.eq(AGR1_DOMAIN)))
                .thenReturn(buildAggregator1Aplans());
        // stubbing of the workflow step loader
        PowerMockito.when(
                workflowStubLoader.invoke(Mockito.eq(BrpWorkflowStep.BRP_PLACE_FLEX_ORDERS.name()), Mockito.any()))
                .then(call -> mockPlaceFlexOrdersWorkflowContext((WorkflowContext) call.getArguments()[1]));
        PowerMockito.when(
                workflowStubLoader.invoke(Mockito.eq(BrpWorkflowStep.BRP_GET_NOT_DESIRABLE_FLEX_OFFERS.name()), Mockito.any())).then(call -> {
            WorkflowContext context = (WorkflowContext) call.getArguments()[1];

            if (context.get(GetNotDesirableFlexOffersParameter.IN.CONNECTION_GROUP_IDENTIFIER.name(), String.class)
                    .equals(AGR2_DOMAIN)) {
                return mockNotDesirableFlexOffersWorkflowContext(context, Arrays.asList(2l));
            } else {
                return mockNotDesirableFlexOffersWorkflowContext(context, new ArrayList<>());
            }
        });
        PowerMockito.when(corePlanboardBusinessService.findAPlanRelatedToFlexOffer(Matchers.any(), Matchers.anyString()))
                .then(call -> buildAplan((Long) call.getArguments()[0], (String) call.getArguments()[1]));

        PowerMockito.when(corePlanboardBusinessService.findPtuFlexOffer(Matchers.eq(1L), Matchers.eq(AGR1_DOMAIN))).thenReturn(
                buildAggregatorPtuFlexOffers(1l, AGR1_DOMAIN));
        PowerMockito.when(corePlanboardBusinessService.findPtuFlexOffer(Matchers.eq(2L), Matchers.eq(AGR1_DOMAIN)))
                .thenReturn(buildAggregatorPtuFlexOffers(2l, AGR1_DOMAIN));
        PowerMockito.when(corePlanboardBusinessService.findPtuFlexOffer(Matchers.eq(2L), Matchers.eq(AGR2_DOMAIN)))
                .thenReturn(buildAggregatorPtuFlexOffers(2l, AGR2_DOMAIN));

        coordinator.handleEvent(new FlexOrderEvent());
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsHelperService, Mockito.times(2)).sendMessageToOutQueue(messageCaptor.capture());

        String xmlMessage = messageCaptor.getValue();
        Assert.assertNotNull(xmlMessage);
        LOGGER.debug(xmlMessage);
    }

    private PlanboardMessage buildAplan(long sequence, String agr) {
        PlanboardMessage aPlan = new PlanboardMessage();
        aPlan.setSequence(sequence);
        aPlan.setDocumentType(DocumentType.A_PLAN);
        aPlan.setConnectionGroup(new AgrConnectionGroup(agr));
        aPlan.setDocumentStatus(DocumentStatus.RECEIVED);
        aPlan.setMessage(new Message());
        return aPlan;
    }

    /**
     * This simple implementation will accept only the offer from Aggregator 1
     *
     * @return a {@link WorkflowStep}.
     */
    private WorkflowContext mockPlaceFlexOrdersWorkflowContext(WorkflowContext context) {
        if (AGR1_DOMAIN.equals(context.getValue(PlaceFlexOrdersStepParameter.IN.CONNECTION_GROUP_IDENTIFIER.name()))) {
            context.setValue(PlaceFlexOrdersStepParameter.OUT.ACCEPTED_FLEX_OFFER_SEQUENCE_LIST.name(),
                    Collections.singletonList(1l));
        } else {
            context.setValue(PlaceFlexOrdersStepParameter.OUT.ACCEPTED_FLEX_OFFER_SEQUENCE_LIST.name(), new ArrayList<Long>());
        }
        return context;
    }

    private WorkflowContext mockNotDesirableFlexOffersWorkflowContext(WorkflowContext context, List<Long> sequenceList) {
        context.setValue(GetNotDesirableFlexOffersParameter.OUT.NOT_DESIRABLE_FLEX_OFFER_SEQUENCE_LIST.name(), sequenceList);
        return context;
    }

    private List<PlanboardMessage> buildProcessableFlexOffers() {
        ConnectionGroup cg1 = new AgrConnectionGroup();
        cg1.setUsefIdentifier(AGR1_DOMAIN);
        ConnectionGroup cg2 = new AgrConnectionGroup();
        cg2.setUsefIdentifier(AGR2_DOMAIN);

        PlanboardMessage offer1 = new PlanboardMessage();
        offer1.setSequence(1l);
        offer1.setParticipantDomain(AGR1_DOMAIN);
        offer1.setDocumentStatus(DocumentStatus.PROCESSED);
        offer1.setConnectionGroup(cg1);
        PlanboardMessage offer2 = new PlanboardMessage();
        offer2.setSequence(2l);
        offer2.setParticipantDomain(AGR2_DOMAIN);
        offer2.setDocumentStatus(DocumentStatus.PROCESSED);
        offer2.setConnectionGroup(cg2);
        return Arrays.asList(offer1, offer2);
    }

    private List<PtuPrognosis> buildAggregator1Aplans() {
        List<PtuPrognosis> aplanPtus = new ArrayList<>();
        ConnectionGroup cg = new AgrConnectionGroup();
        cg.setUsefIdentifier(AGR1_DOMAIN);
        for (int i = 1; i <= 6; ++i) {
            PtuContainer ptuContainer = new PtuContainer();
            ptuContainer.setPtuDate(DateTimeUtil.getCurrentDate());
            ptuContainer.setPtuIndex(i);
            PtuPrognosis aplanPtu = new PtuPrognosis();
            aplanPtu.setConnectionGroup(cg);
            aplanPtu.setPtuContainer(ptuContainer);
            aplanPtu.setPower(BigInteger.valueOf(1000l));
            aplanPtu.setSequence(1l);
            aplanPtu.setType(PrognosisType.A_PLAN);
            aplanPtus.add(aplanPtu);
        }
        return aplanPtus;
    }

    private Map<Integer, PtuFlexOffer> buildAggregatorPtuFlexOffers(long sequence, String agrDomain) {
        List<PtuFlexOffer> ptuFlexOffers = new ArrayList<>();
        ConnectionGroup cg = new AgrConnectionGroup();
        cg.setUsefIdentifier(agrDomain);
        for (int i = 1; i <= 6; ++i) {
            PtuContainer ptuContainer = new PtuContainer();
            ptuContainer.setPtuDate(DateTimeUtil.getCurrentDate());
            ptuContainer.setPtuIndex(i);
            PtuFlexOffer ptuFlexOffer = new PtuFlexOffer();
            ptuFlexOffer.setConnectionGroup(cg);
            ptuFlexOffer.setPtuContainer(ptuContainer);
            ptuFlexOffer.setPower(BigInteger.valueOf(800l));
            ptuFlexOffer.setSequence(sequence);
            ptuFlexOffer.setPrice(BigDecimal.valueOf(100d));
            ptuFlexOffers.add(ptuFlexOffer);
        }
        return ptuFlexOffers.stream().collect(Collectors.toMap(fo -> fo.getPtuContainer().getPtuIndex(), Function.identity()));
    }
}
