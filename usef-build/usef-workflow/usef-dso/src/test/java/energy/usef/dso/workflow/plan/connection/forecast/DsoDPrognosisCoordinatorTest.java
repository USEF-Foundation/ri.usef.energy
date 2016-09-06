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

package energy.usef.dso.workflow.plan.connection.forecast;

import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.Message;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.dso.controller.PrognosisController;
import energy.usef.dso.model.Aggregator;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;
import energy.usef.dso.workflow.validate.gridsafetyanalysis.GridSafetyAnalysisEvent;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.enterprise.event.Event;

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

/**
 * Test class in charge of the unit tests related to the {@link PrognosisController} class.
 */
@RunWith(PowerMockRunner.class)
public class DsoDPrognosisCoordinatorTest {

    private DsoDPrognosisCoordinator coordinator;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private Event<GridSafetyAnalysisEvent> eventManager;

    @Mock
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    @Before
    public void init() {
        coordinator = new DsoDPrognosisCoordinator();
        Whitebox.setInternalState(coordinator, eventManager);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, dsoPlanboardBusinessService);
    }

    @Test
    public void testInvokeForNewPrognosis() throws BusinessException {
        PowerMockito.when(corePlanboardBusinessService
                .findLastPrognoses(Matchers.any(LocalDate.class), Matchers.any(PrognosisType.class), Matchers.any(String.class)))
                .thenReturn(new ArrayList<>());
        PowerMockito.when(corePlanboardBusinessService.findAcceptedPrognosisMessages(Matchers.any(DocumentType.class),
                Matchers.any(LocalDate.class), Matchers.any(String.class))).thenReturn(
                Collections.singletonList(new PlanboardMessage()));
        PowerMockito.when(dsoPlanboardBusinessService.getAggregatorsByEntityAddress(Matchers.any(String.class),
                        Matchers.any(LocalDate.class))).thenReturn(Arrays.asList(new Aggregator(), new Aggregator()));
        Prognosis prognosis = buildPrognosis();
        coordinator.invokeWorkflow(prognosis, null);

        Mockito.verify(corePlanboardBusinessService, Mockito.times(1))
                .findLastPrognoses(Matchers.any(LocalDate.class), Matchers.eq(PrognosisType.D_PROGNOSIS),
                        Matchers.eq(prognosis.getCongestionPoint()));
        Mockito.verify(corePlanboardBusinessService, Mockito.times(1)).storePrognosis(Mockito.anyString(),
                Matchers.eq(prognosis),
                Matchers.eq(DocumentType.D_PROGNOSIS),
                Matchers.eq(DocumentStatus.ACCEPTED),
                Matchers.eq(prognosis.getMessageMetadata().getSenderDomain()),
                (Message) Matchers.isNull(),
                Matchers.eq(false));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvokeForUpdatedPrognosis() throws BusinessException {
        PowerMockito.when(corePlanboardBusinessService
                .findLastPrognoses(Matchers.any(LocalDate.class), Matchers.any(PrognosisType.class), Matchers.any(String.class)))
                .thenReturn(buildPtuPrognosisList());
        PowerMockito.when(corePlanboardBusinessService.findAcceptedPrognosisMessages(Matchers.any(DocumentType.class),
                Matchers.any(LocalDate.class), Matchers.any(String.class)))
                .thenReturn(Arrays.asList(new PlanboardMessage(), new PlanboardMessage()));
        PowerMockito.when(dsoPlanboardBusinessService.getAggregatorsByEntityAddress(Matchers.any(String.class),
                        Matchers.any(LocalDate.class))).thenReturn(Arrays.asList(new Aggregator(), new Aggregator()));
        Prognosis prognosis = buildPrognosis();
        coordinator.invokeWorkflow(prognosis, null);
        Mockito.verify(dsoPlanboardBusinessService, Mockito.times(1)).handleUpdatedPrognosis(
                Matchers.eq(prognosis),
                Matchers.any(List.class));
    }

    private Prognosis buildPrognosis() {
        Prognosis dprognosis = new Prognosis();
        dprognosis.setMessageMetadata(new MessageMetadataBuilder()
                .senderDomain("agr.usef-example.com").senderRole(USEFRole.AGR)
                .recipientDomain("dso.usef-example.com").recipientRole(USEFRole.DSO)
                .messageID().conversationID().timeStamp()
                .build());
        dprognosis.setSequence(2l);
        dprognosis.setCongestionPoint("ean.0123456789012345678");
        dprognosis.setPeriod(new LocalDate());

        PTU ptu1 = new PTU();
        ptu1.setStart(BigInteger.valueOf(1));
        ptu1.setPower(BigInteger.valueOf(900));

        PTU ptu2 = new PTU();
        ptu2.setStart(BigInteger.valueOf(2));
        ptu2.setPower(BigInteger.valueOf(900));
        dprognosis.getPTU().add(ptu1);
        dprognosis.getPTU().add(ptu2);
        return dprognosis;
    }

    private List<PtuPrognosis> buildPtuPrognosisList() {
        PtuContainer ptu1 = new PtuContainer();
        ptu1.setPtuIndex(1);
        PtuContainer ptu2 = new PtuContainer();
        ptu2.setPtuIndex(2);

        PtuPrognosis prognosis1 = new PtuPrognosis();
        prognosis1.setParticipantDomain("agr.usef-example.com");
        prognosis1.setSequence(1l);
        prognosis1.setPtuContainer(ptu1);
        prognosis1.setPower(BigInteger.valueOf(1000));

        PtuPrognosis prognosis2 = new PtuPrognosis();
        prognosis2.setParticipantDomain("agr.usef-example.com");
        prognosis2.setSequence(1l);
        prognosis2.setPtuContainer(ptu2);
        prognosis2.setPower(BigInteger.valueOf(1000));
        return Arrays.asList(prognosis1, prognosis2);
    }

}
