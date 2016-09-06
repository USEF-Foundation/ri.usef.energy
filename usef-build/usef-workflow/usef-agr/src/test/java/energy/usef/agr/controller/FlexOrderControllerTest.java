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

package energy.usef.agr.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import energy.usef.agr.exception.AgrBusinessError;
import energy.usef.agr.service.business.AgrValidationBusinessService;
import energy.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioEvent;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.DispositionAvailableRequested;
import energy.usef.core.data.xml.bean.message.FlexOrder;
import energy.usef.core.data.xml.bean.message.FlexOrderResponse;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.PtuContainerState;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.core.util.XMLUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Event;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class with unit tests for {@link FlexOrderController }
 */
@RunWith(PowerMockRunner.class)
public class FlexOrderControllerTest {
    private static final LocalDate DATE = new LocalDate().plusDays(1);
    private static final int MESSAGE_SEQUENCE = 1;
    private static final String MESSAGE_ORIGIN = "dso.usef-example.com";
    private static final String CONGESTION_POINT = "ean.123456789012345678";
    private static final String MESSAGE_CURRENCY = "EUR";
    private static final String TIME_ZONE = "Europe/Amsterdam";

    private FlexOrderController flexOrderController;

    @Mock
    private JMSHelperService jmsHelperService;

    @Mock
    private AgrValidationBusinessService agrValidationBusinessService;
    @Mock
    private CorePlanboardValidatorService corePlanboardValidatorService;
    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Mock
    private Event<ReOptimizePortfolioEvent> eventManager;

    @Before
    public void init() {
        flexOrderController = new FlexOrderController();
        Whitebox.setInternalState(flexOrderController, "jmsService", jmsHelperService);
        Whitebox.setInternalState(flexOrderController, "agrValidationBusinessService", agrValidationBusinessService);
        Whitebox.setInternalState(flexOrderController, "corePlanboardValidatorService", corePlanboardValidatorService);
        Whitebox.setInternalState(flexOrderController, "corePlanboardBusinessService", corePlanboardBusinessService);
        Whitebox.setInternalState(flexOrderController, eventManager);
    }

    @Test
    public void flexOrderSendResponsePutsMessageInQueueTest() {
        FlexOrder order = createFlexOrder();

        try {
            flexOrderController.action(order, null);

            Mockito.verify(corePlanboardValidatorService, Mockito.times(1)).validateTimezone(Matchers.eq(order.getTimeZone()));
            Mockito.verify(corePlanboardValidatorService, Mockito.times(1))
                    .validatePTUDuration(Matchers.eq(order.getPTUDuration()));
            Mockito.verify(corePlanboardValidatorService, Mockito.times(1)).validateDomain(Matchers.eq(order.getFlexOfferOrigin()));
            Mockito.verify(corePlanboardValidatorService, Mockito.times(1)).validateCurrency(Matchers.eq(order.getCurrency()));
            Mockito.verify(agrValidationBusinessService, Mockito.times(1)).validateFlexOrderTiming(Matchers.eq(order));
            Mockito.verify(eventManager, Mockito.times(1)).fire(Mockito.any(ReOptimizePortfolioEvent.class));

            ArgumentCaptor<String> flexOrderResponseXml = ArgumentCaptor.forClass(String.class);

            Mockito.verify(corePlanboardBusinessService, Mockito.times(1)).storeFlexOrder(order.getCongestionPoint(), order,
                    DocumentStatus.ACCEPTED, order.getMessageMetadata().getSenderDomain(), AcknowledgementStatus.ACCEPTED,
                    PtuContainerState.PlanValidate);
            Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(flexOrderResponseXml.capture());
            FlexOrderResponse flexOrderResponse = XMLUtil
                    .xmlToMessage(flexOrderResponseXml.getValue(), FlexOrderResponse.class);

            assertEquals(DispositionAcceptedRejected.ACCEPTED, flexOrderResponse.getResult());
            assertNull(flexOrderResponse.getMessage());

        } catch (BusinessException e) {
            fail("Semantic validation failed " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Validation of flexOrder failed" + e.getMessage());
        }
    }

    @Test
    public void testBRPflexOrderSendResponsePutsMessageInQueueTest() {
        FlexOrder order = createFlexOrder();
        order.getMessageMetadata().setSenderDomain("brp.usef-example.com");
        order.getMessageMetadata().setSenderRole(USEFRole.BRP);
        order.setCongestionPoint(null);

        try {
            flexOrderController.action(order, null);

            Mockito.verify(corePlanboardValidatorService, Mockito.times(1)).validateTimezone(Matchers.eq(order.getTimeZone()));
            Mockito.verify(corePlanboardValidatorService, Mockito.times(1))
                    .validatePTUDuration(Matchers.eq(order.getPTUDuration()));
            Mockito.verify(corePlanboardValidatorService, Mockito.times(1)).validateDomain(Matchers.eq(order.getFlexOfferOrigin()));
            Mockito.verify(corePlanboardValidatorService, Mockito.times(1)).validateCurrency(Matchers.eq(order.getCurrency()));
            Mockito.verify(eventManager, Mockito.times(1)).fire(Mockito.any(ReOptimizePortfolioEvent.class));
            Mockito.verify(agrValidationBusinessService, Mockito.times(1)).validateFlexOrderTiming(Matchers.eq(order));

            ArgumentCaptor<String> flexOrderResponseXml = ArgumentCaptor.forClass(String.class);

            Mockito.verify(corePlanboardBusinessService, Mockito.times(1)).storeFlexOrder("brp.usef-example.com", order,
                    DocumentStatus.ACCEPTED, order.getMessageMetadata().getSenderDomain(), AcknowledgementStatus.ACCEPTED,
                    PtuContainerState.PlanValidate);
            Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(flexOrderResponseXml.capture());
            FlexOrderResponse flexOrderResponse = XMLUtil
                    .xmlToMessage(flexOrderResponseXml.getValue(), FlexOrderResponse.class);

            assertEquals(DispositionAcceptedRejected.ACCEPTED, flexOrderResponse.getResult());
            assertNull(flexOrderResponse.getMessage());

        } catch (BusinessException e) {
            fail("Semantic validation failed " + e.getMessage());
        } catch (Exception e) {
            fail("Validatino of flexOrder failed" + e.getMessage());
        }
    }

    @Test
    public void flexOrderDoesNotMatchFlexOfferValidationErrorTest() throws BusinessException {
        FlexOrder order = createFlexOrder();

        Mockito.doThrow(new BusinessValidationException(AgrBusinessError.NO_MATCHING_OFFER_FOR_ORDER, order))
                .when(agrValidationBusinessService).validateCorrespondingFlexOffer(order);

        flexOrderController.action(order, null);

        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(Matchers.contains("Result=\"Rejected\""));
        Mockito.verify(eventManager, Mockito.times(0)).fire(Mockito.any(ReOptimizePortfolioEvent.class));
    }

    private FlexOrder createFlexOrder() {
        FlexOrder flexOrder = new FlexOrder();
        flexOrder.setCongestionPoint(CONGESTION_POINT);
        flexOrder.setPeriod(DATE);
        flexOrder.setFlexOfferOrigin(MESSAGE_ORIGIN);
        flexOrder.setFlexOfferSequence(MESSAGE_SEQUENCE);
        flexOrder.setCurrency(MESSAGE_CURRENCY);
        flexOrder.setTimeZone(TIME_ZONE);
        flexOrder.setOrderReference("1");

        MessageMetadata messageMetadata = new MessageMetadata();
        messageMetadata.setMessageID("testId");
        messageMetadata.setConversationID("testConversation");
        messageMetadata.setSenderDomain("test.sender.domain");
        messageMetadata.setSenderRole(USEFRole.DSO);
        flexOrder.setMessageMetadata(messageMetadata);

        List<PTU> ptus = createPTUList(1, 96, 300, 0.045 / 1000);
        flexOrder.getPTU().addAll(ptus);

        return flexOrder;
    }

    private List<PTU> createPTUList(int start, int duration, int power, double price) {
        List<PTU> ptus = new ArrayList<>();
        ptus.add(createPTU(DispositionAvailableRequested.REQUESTED, start, duration, power, price));
        return ptus;
    }

    private PTU createPTU(DispositionAvailableRequested disposition, int start, int duration, int power, double price) {
        PTU ptu = new PTU();
        ptu.setDisposition(disposition);
        ptu.setDuration(BigInteger.valueOf(duration));
        ptu.setStart(BigInteger.valueOf(start));
        ptu.setPower(BigInteger.valueOf(power));
        ptu.setPrice(BigDecimal.valueOf(price));
        return ptu;
    }

}
