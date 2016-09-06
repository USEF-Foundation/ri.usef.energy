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

package energy.usef.agr.service.business;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.DispositionAvailableRequested;
import energy.usef.core.data.xml.bean.message.FlexOrder;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.validation.CoreBusinessError;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.core.util.DateTimeUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class AgrValidationBusinessServiceTest {
    private static final LocalDate DATE = new LocalDate("2014-12-28");
    private static final long MESSAGE_SEQUENCE = 1L;
    private static final String MESSAGE_ORIGIN = "dso.usef-example.com";
    private static final String CONGESTION_POINT = "ean.123456789012345678";

    private AgrValidationBusinessService agrValidationBusinessService;

    @Mock
    private AgrPlanboardBusinessService agrPlanboardBusinessService;

    @Mock
    private CorePlanboardValidatorService corePlanboardValidatorService;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private Config config;

    @Before
    public void init() {
        agrValidationBusinessService = new AgrValidationBusinessService();
        Whitebox.setInternalState(agrValidationBusinessService, "agrPlanboardBusinessService",
                agrPlanboardBusinessService);
        Whitebox.setInternalState(agrValidationBusinessService, "corePlanboardBusinessService",
                corePlanboardBusinessService);
        Whitebox.setInternalState(agrValidationBusinessService, "corePlanboardValidatorService",
                corePlanboardValidatorService);
        Whitebox.setInternalState(agrValidationBusinessService, config);

        Mockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);

    }

    @Test
    public void testValidateCorrespondingFlexOfferWithMatchingOfferPTUs() {
        try {
            FlexOrder order = createFlexOrder();
            Map<Integer, PtuFlexOffer> ptuFlexOffer = createPtuFlexOffer(300, 0.045 / 1000);
            PTU orderPTU = createPTU(DispositionAvailableRequested.REQUESTED, 1, 1, 300, 0.045 / 1000);
            order.getPTU().clear();
            order.getPTU().add(orderPTU);

            PlanboardMessage planboardMessage = new PlanboardMessage(DocumentType.FLEX_OFFER, order.getFlexOfferSequence(),
                    DocumentStatus.ACCEPTED, order.getFlexOfferOrigin(), order.getPeriod(), order.getFlexOfferSequence(),
                    new CongestionPointConnectionGroup(order.getCongestionPoint()), DateTimeUtil.getEndOfDay(new LocalDateTime()));

            Mockito.when(corePlanboardBusinessService
                    .findPtuFlexOffer(order.getFlexOfferSequence(), order.getMessageMetadata().getSenderDomain()))
                    .thenReturn(ptuFlexOffer);
            Mockito.when(corePlanboardValidatorService
                    .validatePlanboardMessageExpirationDate(order.getFlexOfferSequence(), DocumentType.FLEX_OFFER,
                            order.getMessageMetadata().getSenderDomain())).thenReturn(planboardMessage);
            agrValidationBusinessService.validateCorrespondingFlexOffer(order);
        } catch (BusinessValidationException e) {
            Assert.fail(" do not expect exception");
        }
    }

    @Test
    public void testValidateCorrespondingFlexOfferWithDiffPriceOfferPTUs() {
        try {
            FlexOrder order = createFlexOrder();
            Map<Integer, PtuFlexOffer> ptuFlexOffer = createPtuFlexOffer(300, 0.045 / 1000);
            PTU orderPTU = createPTU(DispositionAvailableRequested.REQUESTED, 1, 1, 300, 0.040 / 1000);
            order.getPTU().clear();
            order.getPTU().add(orderPTU);
            PlanboardMessage planboardMessage = new PlanboardMessage(DocumentType.FLEX_OFFER, order.getFlexOfferSequence(),
                    DocumentStatus.ACCEPTED, order.getFlexOfferOrigin(), order.getPeriod(), order.getFlexOfferSequence(),
                    new CongestionPointConnectionGroup(order.getCongestionPoint()), DateTimeUtil.getEndOfDay(new LocalDateTime()));
            Mockito.when(corePlanboardValidatorService
                    .validatePlanboardMessageExpirationDate(order.getFlexOfferSequence(), DocumentType.FLEX_OFFER,
                            order.getMessageMetadata().getSenderDomain())).thenReturn(planboardMessage);
            agrValidationBusinessService.validateCorrespondingFlexOffer(order);
            Assert.fail(" Exception epected, price is different for order and offer.");
        } catch (BusinessValidationException e) {
        }
    }

    @Test
    public void testValidateCorrespondingFlexOfferWithDiffPowerInOfferPTUs() {
        try {
            FlexOrder order = createFlexOrder();
            Map<Integer, PtuFlexOffer> ptuFlexOffer = createPtuFlexOffer(200, 0.045 / 1000);
            PTU orderPTU = createPTU(DispositionAvailableRequested.REQUESTED, 1, 1, 300, 0.045 / 1000);
            order.getPTU().clear();
            order.getPTU().add(orderPTU);
            Mockito.when(corePlanboardBusinessService
                    .findPtuFlexOffer(order.getFlexOfferSequence(), order.getMessageMetadata().getSenderDomain()))
                    .thenReturn(ptuFlexOffer);
            PlanboardMessage planboardMessage = new PlanboardMessage(DocumentType.FLEX_OFFER, order.getFlexOfferSequence(),
                    DocumentStatus.ACCEPTED, order.getFlexOfferOrigin(), order.getPeriod(), order.getFlexOfferSequence(),
                    new CongestionPointConnectionGroup(order.getCongestionPoint()), DateTimeUtil.getEndOfDay(new LocalDateTime()));
            Mockito.when(corePlanboardValidatorService
                    .validatePlanboardMessageExpirationDate(order.getFlexOfferSequence(), DocumentType.FLEX_OFFER,
                            order.getMessageMetadata().getSenderDomain())).thenReturn(planboardMessage);
            agrValidationBusinessService.validateCorrespondingFlexOffer(order);
            Assert.fail(" Exception expected, power is different for order and offer.");
        } catch (BusinessValidationException e) {
        }
    }

    @Test(expected = BusinessValidationException.class)
    public void testValidateCorrespondingFlexOfferWithExpiredFlexOffer() throws Exception {
        FlexOrder order = createFlexOrder();
        order.setPeriod(DateTimeUtil.getCurrentDate());

        Mockito.when(corePlanboardValidatorService
                .validatePlanboardMessageExpirationDate(order.getFlexOfferSequence(), DocumentType.FLEX_OFFER,
                        order.getMessageMetadata().getSenderDomain()))
                .thenThrow(new BusinessValidationException(CoreBusinessError.DOCUMENT_EXIRED, DocumentType.FLEX_OFFER,
                        order.getFlexOfferSequence(),
                        DateTimeUtil.getStartOfDay(new LocalDateTime())));
        agrValidationBusinessService.validateCorrespondingFlexOffer(order);
    }

    @Test
    public void testValidateCorrespondingFlexOfferRevoked() {
        try {
            FlexOrder order = createFlexOrder();
            order.setPeriod(DateTimeUtil.getCurrentDate());

            Map<Integer, PtuFlexOffer> ptuFlexOffer = createPtuFlexOffer(200, 0.045 / 1000);
            PTU orderPTU = createPTU(DispositionAvailableRequested.REQUESTED, 1, 1, 200, 0.045 / 1000);
            order.getPTU().clear();
            order.getPTU().add(orderPTU);

            PlanboardMessage revokedPlanboardMessage = new PlanboardMessage(DocumentType.FLEX_OFFER, order.getFlexOfferSequence(),
                    DocumentStatus.REVOKED, order.getFlexOfferOrigin(), order.getPeriod(), order.getFlexOfferSequence(),
                    new CongestionPointConnectionGroup(order.getCongestionPoint()), DateTimeUtil.getEndOfDay(new LocalDateTime()));

            Mockito.when(corePlanboardBusinessService
                    .findPtuFlexOffer(order.getFlexOfferSequence(), order.getMessageMetadata().getSenderDomain()))
                    .thenReturn(ptuFlexOffer);

            Mockito.when(corePlanboardValidatorService
                    .validatePlanboardMessageExpirationDate(order.getFlexOfferSequence(), DocumentType.FLEX_OFFER,
                            order.getMessageMetadata().getSenderDomain())).thenReturn(revokedPlanboardMessage);

            agrValidationBusinessService.validateCorrespondingFlexOffer(order);
            Assert.fail("Exception expected, offer has already been expired!");
        } catch (BusinessValidationException e) {
        }
    }

    @Test
    public void testValidateFlexOrderTimingFuture() {
        FlexOrder order = createFlexOrder();
        order.setPeriod(DateTimeUtil.getCurrentDate().plusDays(1));
        try {
            agrValidationBusinessService.validateFlexOrderTiming(order);
        } catch (BusinessValidationException e) {
            Assert.fail("No BusinessValidationException excepted, order is in the future.");
        }
    }

    @Test
    public void testValidateFlexOrderTimingTodayAcceptable() {
        FlexOrder order = createFlexOrder();
        order.setPeriod(DateTimeUtil.getCurrentDate());
        try {
            agrValidationBusinessService.validateFlexOrderTiming(order);
        } catch (BusinessValidationException e) {
            Assert.fail("No BusinessValidationException excepted, future PTU's have non-zero power values.");
        }
    }

    @Test
    public void testValidateFlexOrderTimingTodayNotAcceptable() {
        FlexOrder order = createFlexOrder();
        order.setPeriod(DateTimeUtil.getCurrentDate());
        order.getPTU().get(0).setPower(BigInteger.ZERO);
        try {
            agrValidationBusinessService.validateFlexOrderTiming(order);
            Assert.fail("BusinessValidationException excepted, all future PTU's have zero power values.");
        } catch (BusinessValidationException e) {
        }
    }

    @Test
    public void testValidateFlexOrderTimingPast() {
        FlexOrder order = createFlexOrder();
        order.setPeriod(DateTimeUtil.getCurrentDate().minusDays(1));
        try {
            agrValidationBusinessService.validateFlexOrderTiming(order);
            Assert.fail("BusinessValidationException excepted, order is in the past.");
        } catch (BusinessValidationException e) {
        }
    }

    private Map<Integer, PtuFlexOffer> createPtuFlexOffer(int power, double price) {
        return IntStream.rangeClosed(1, 96).mapToObj(index -> {
            PtuFlexOffer ptuFlexOffer = new PtuFlexOffer();
            ptuFlexOffer.setPower(BigInteger.valueOf(power));
            ptuFlexOffer.setPrice(BigDecimal.valueOf(price));
            ptuFlexOffer.setPtuContainer(new PtuContainer(DATE, index));
            return ptuFlexOffer;
        }).collect(Collectors.toMap(fo -> fo.getPtuContainer().getPtuIndex(), Function.identity()));
    }

    private FlexOrder createFlexOrder() {
        FlexOrder flexOrder = new FlexOrder();
        flexOrder.setCongestionPoint(CONGESTION_POINT);
        flexOrder.setPeriod(DATE);
        flexOrder.setFlexOfferOrigin(MESSAGE_ORIGIN);
        flexOrder.setFlexOfferSequence(MESSAGE_SEQUENCE);
        flexOrder.setCurrency("EUR");
        flexOrder.setOrderReference("1");

        MessageMetadata messageMetadata = new MessageMetadata();
        messageMetadata.setMessageID("testId");
        messageMetadata.setConversationID("testConversation");
        messageMetadata.setSenderDomain("test.sender.domain");
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
