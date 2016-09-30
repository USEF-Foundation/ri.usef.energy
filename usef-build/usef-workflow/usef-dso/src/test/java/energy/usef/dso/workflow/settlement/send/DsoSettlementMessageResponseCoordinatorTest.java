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

package energy.usef.dso.workflow.settlement.send;

import energy.usef.core.data.xml.bean.message.DispositionAcceptedDisputed;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.service.business.CorePlanboardBusinessService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
 * Test class in charge of the unit tests related to {@link DsoSettlementMessageResponseCoordinator}.
 */
@RunWith(PowerMockRunner.class)
public class DsoSettlementMessageResponseCoordinatorTest {

    private DsoSettlementMessageResponseCoordinator dsoSettlementMessageResponseCoordinator;
    private static final String AGGREGATOR_DOMAIN = "agr.usef-example.com";
    @Mock
    private CorePlanboardBusinessService businessService;

    @Before
    public void init() {
        dsoSettlementMessageResponseCoordinator = new DsoSettlementMessageResponseCoordinator();
        Whitebox.setInternalState(dsoSettlementMessageResponseCoordinator, businessService);
    }

    @Test
    public void testProcessPtuSettlements() {
        PowerMockito.when(
                businessService.findPlanboardMessagesWithOriginSequence(Matchers.eq(1L),
                        Matchers.eq(DocumentType.FLEX_ORDER_SETTLEMENT), Matchers.eq(AGGREGATOR_DOMAIN))).thenReturn(
                buildPlanboardMessageList());
        PowerMockito.when(
                businessService.findPlanboardMessagesWithOriginSequence(Matchers.eq(2L),
                        Matchers.eq(DocumentType.FLEX_ORDER_SETTLEMENT), Matchers.eq(AGGREGATOR_DOMAIN))).thenReturn(
                buildPlanboardMessageList());

        dsoSettlementMessageResponseCoordinator.processPtuSettlements(buildOrderReferences(), DispositionAcceptedDisputed.DISPUTED,
                AGGREGATOR_DOMAIN);
        Mockito.verify(businessService, Mockito.times(2))
                .findPlanboardMessagesWithOriginSequence(Matchers.any(Long.class), Matchers.eq(DocumentType.FLEX_ORDER_SETTLEMENT),
                        Matchers.eq(AGGREGATOR_DOMAIN));
        ArgumentCaptor<PlanboardMessage> planboardMessageCaptor = ArgumentCaptor.forClass(PlanboardMessage.class);
        Mockito.verify(businessService, Mockito.times(2)).updatePlanboardMessage(planboardMessageCaptor.capture());
        // check that we updated every planboard message with the status 'DISPUTED'
        for (PlanboardMessage pm : planboardMessageCaptor.getAllValues()) {
            Assert.assertEquals(DocumentStatus.DISPUTED, pm.getDocumentStatus());
        }
    }

    private List<PlanboardMessage> buildPlanboardMessageList() {
        PlanboardMessage planboardMessage = new PlanboardMessage();
        planboardMessage.setDocumentStatus(DocumentStatus.SENT);
        return Collections.singletonList(planboardMessage);
    }

    private List<Long> buildOrderReferences() {
        return Arrays.asList(1l, 2l);
    }

}
