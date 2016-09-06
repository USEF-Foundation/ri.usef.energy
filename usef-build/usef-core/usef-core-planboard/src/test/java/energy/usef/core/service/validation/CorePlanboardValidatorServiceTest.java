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

package energy.usef.core.service.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.FlexOffer;
import energy.usef.core.data.xml.bean.message.FlexOfferRevocation;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.Document;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuContainerState;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.model.PtuState;
import energy.usef.core.repository.ConnectionGroupRepository;
import energy.usef.core.repository.PlanboardMessageRepository;
import energy.usef.core.repository.PtuContainerRepository;
import energy.usef.core.repository.PtuStateRepository;
import energy.usef.core.util.DateTimeUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.junit.Assert;
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
 * Test class in charge of the unit tests related to the {@link CorePlanboardValidatorService} class.
 */
@RunWith(PowerMockRunner.class)
public class CorePlanboardValidatorServiceTest {

    @Mock
    private Config config;
    @Mock
    private PtuContainerRepository ptuContainerRepository;

    @Mock
    private PlanboardMessageRepository planboardMessageRepository;

    @Mock
    private PtuStateRepository ptuStateRepository;

    @Mock
    private ConnectionGroupRepository connectionGroupRepository;

    private CorePlanboardValidatorService validator;

    @Before
    public void init() {
        validator = new CorePlanboardValidatorService();
        Whitebox.setInternalState(validator, config);
        Whitebox.setInternalState(validator, ptuContainerRepository);
        Whitebox.setInternalState(validator, planboardMessageRepository);
        Whitebox.setInternalState(validator, ptuStateRepository);
        Whitebox.setInternalState(validator, connectionGroupRepository);
    }

    @Test
    public void testIsPlanboardItemWithingIntradayGateClosureTime() {
        PowerMockito.when(config.getIntegerProperty(ConfigParam.INTRADAY_GATE_CLOSURE_PTUS)).thenReturn(8);
        PowerMockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);

        LocalDateTime now = new LocalDateTime();
        LocalDateTime acceptableDateTime = now.plusMinutes(8 * 15 + 15 + 1);
        LocalDateTime rejectableDateTime = now.plusMinutes(8 * 15 - 15 - 1);

        PtuContainer acceptablePtuContainer = new PtuContainer();
        acceptablePtuContainer.setPtuDate(acceptableDateTime.toLocalDate());
        acceptablePtuContainer.setPtuIndex(1 + acceptableDateTime.get(DateTimeFieldType.minuteOfDay()) / 15);
        PtuContainer rejectablePtuContainer = new PtuContainer();
        rejectablePtuContainer.setPtuDate(rejectableDateTime.toLocalDate());
        rejectablePtuContainer.setPtuIndex(1 + rejectableDateTime.get(DateTimeFieldType.minuteOfDay()) / 15);
        Document document = new Document();
        document.setPtuContainer(rejectablePtuContainer);
        Assert.assertTrue(validator.isPlanboardItemWithingIntradayGateClosureTime(document));
        document.setPtuContainer(acceptablePtuContainer);
        Assert.assertFalse(validator.isPlanboardItemWithingIntradayGateClosureTime(document));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHasPlanboardItemPtusInOperatePhase() {
        PowerMockito.when(
                ptuContainerRepository.findPtuContainersForDocumentSequence(Matchers.any(Long.class), Matchers.any(Class.class)))
                .thenReturn(buildPtuContainers(true));

        PtuState ptuState = new PtuState();
        ptuState.setState(PtuContainerState.Operate);
        Mockito.when(ptuStateRepository.findOrCreatePtuState(Mockito.any(PtuContainer.class), Mockito.any(ConnectionGroup.class)))
                .thenReturn(ptuState);
        Assert.assertTrue(validator.hasPlanboardItemPtusInOperatePhase(new Document()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHasPlanboardItemPtusInOperatePhaseWithout() {
        PowerMockito.when(
                ptuContainerRepository.findPtuContainersForDocumentSequence(Matchers.any(Long.class), Matchers.any(Class.class)))
                .thenReturn(buildPtuContainers(false));
        PtuState ptuState = new PtuState();
        ptuState.setState(PtuContainerState.Settled);
        Mockito.when(ptuStateRepository.findOrCreatePtuState(Mockito.any(PtuContainer.class), Mockito.any(ConnectionGroup.class)))
                .thenReturn(ptuState);
        Assert.assertFalse(validator.hasPlanboardItemPtusInOperatePhase(new Document()));
    }

    @Test
    public void testValidateTimezone() {
        Mockito.when(config.getProperty(Matchers.eq(ConfigParam.TIME_ZONE))).thenReturn("Europe/Amsterdam");
        // geldig
        try {
            validator.validateTimezone("Europe/Amsterdam");
        } catch (BusinessValidationException e1) {
            fail("Should not throw exception");
        }
        // ongeldig
        try {
            validator.validateTimezone("Europe/Onzin");
            fail("expected BusinessException");
        } catch (BusinessValidationException e) {
            assertEquals(CoreBusinessError.INVALID_TIMEZONE, e.getBusinessError());
        }
    }

    @Test
    public void testValidateCurrency() {
        Mockito.when(config.getProperty(Matchers.eq(ConfigParam.CURRENCY))).thenReturn("EUR");
        // geldig
        try {
            validator.validateCurrency("EUR");
        } catch (BusinessValidationException e1) {
            fail("Should not throw exception");
        }
        // ongeldig
        try {
            validator.validateCurrency("DOLLAR");
            fail("expected BusinessException");
        } catch (BusinessValidationException e) {
            assertEquals(CoreBusinessError.INVALID_CURRENCY, e.getBusinessError());
        }
    }

    @Test
    public void testValidatePTUDuration() {
        Mockito.when(config.getIntegerProperty(Matchers.eq(ConfigParam.PTU_DURATION))).thenReturn(15);
        // geldig
        try {
            validator.validatePTUDuration(Period.minutes(15));
        } catch (BusinessValidationException e1) {
            fail("Should not throw exception");
        }
        // ongeldig
        try {
            validator.validatePTUDuration(Period.minutes(10));
            fail("expected BusinessException");
        } catch (BusinessValidationException e) {
            assertEquals(CoreBusinessError.INVALID_PTU_DURATION, e.getBusinessError());
        }
    }

    @Test
    public void testValidateDomain() {
        Mockito.when(config.getProperty(Matchers.eq(ConfigParam.HOST_DOMAIN))).thenReturn("usef-example.com");
        // geldig
        try {
            validator.validateDomain("usef-example.com");
        } catch (BusinessValidationException e1) {
            fail("Should not throw exception");
        }
        // ongeldig
        try {
            validator.validateDomain("agr1.usef-example.com");
            fail("expected BusinessException");
        } catch (BusinessValidationException e) {
            assertEquals(CoreBusinessError.INVALID_DOMAIN, e.getBusinessError());
        }
    }

    @Test
    public void testValidatePTUsForPeriod() {
        // ptu 0-9
        PTU ptu1 = new PTU();
        ptu1.setStart(BigInteger.ONE);
        ptu1.setDuration(BigInteger.TEN);
        // ptu 10-19
        PTU ptu2 = new PTU();
        ptu2.setStart(new BigInteger("11"));
        ptu2.setDuration(BigInteger.TEN);
        // ptu 20 - 96
        PTU ptu3 = new PTU();
        ptu3.setStart(new BigInteger("21"));
        ptu3.setDuration(new BigInteger("76"));

        // check a normal day 96
        Mockito.when(config.getIntegerProperty(Matchers.eq(ConfigParam.PTU_DURATION))).thenReturn(15);

        // incorrect amount
        List<PTU> ptus = new ArrayList<>();
        ptus.add(ptu2);
        try {
            validator.validatePTUsForPeriod(ptus, new LocalDate("2014-12-16"), false);
            fail("Should throw exception");
        } catch (BusinessValidationException e) {
            assertEquals(CoreBusinessError.WRONG_NUMBER_OF_PTUS, e.getBusinessError());
        }

        // incorrect values
        ptus.clear();
        ptus.add(ptu2);
        ptus.add(ptu2);
        ptus.add(ptu3);
        try {
            validator.validatePTUsForPeriod(ptus, DateTimeUtil.parseDate("2014-12-16"), false);
            fail("Should throw exception");
        } catch (BusinessValidationException e) {
            assertEquals(CoreBusinessError.INCOMPLETE_PTUS, e.getBusinessError());
        }

        // wrong order but correct values
        ptus.clear();
        ptus.add(ptu2);
        ptus.add(ptu1);
        ptus.add(ptu3);
        try {
            validator.validatePTUsForPeriod(ptus, DateTimeUtil.parseDate("2014-12-16"), false);
        } catch (BusinessValidationException e) {
            fail("Should not throw exception");
        }

    }

    @Test
    public void testValidateNonexistentFlexRequest() {
        DocumentType documentType = DocumentType.FLEX_REQUEST;
        Long sequence = 123123L;
        String participantDomain = "usef-example.com";
        PlanboardMessage flexRequest = new PlanboardMessage();

        flexRequest.setExpirationDate(new LocalDateTime().plusDays(1));

        Mockito.when(planboardMessageRepository.findSinglePlanboardMessage(Matchers.eq(sequence), Matchers.eq(documentType),
                Matchers.eq(participantDomain))).thenReturn(null);
        try {
            MessageMetadata metaData = new MessageMetadata();
            metaData.setSenderDomain(participantDomain);
            FlexOffer flexOffer = new FlexOffer();
            flexOffer.setFlexRequestSequence(sequence);
            flexOffer.setMessageMetadata(metaData);
            validator.validatePlanboardMessageExpirationDate(sequence, DocumentType.FLEX_REQUEST, participantDomain);
            fail("expected BusinessException");
        } catch (BusinessValidationException e) {
            assertEquals(CoreBusinessError.RELATED_MESSAGE_NOT_FOUND, e.getBusinessError());
        }
    }

    @Test
    public void testValidateExistentFlexRequest() {
        DocumentType documentType = DocumentType.FLEX_REQUEST;
        Long sequence = 123123L;
        String participantDomain = "usef-example.com";
        PlanboardMessage flexRequest = new PlanboardMessage();

        flexRequest.setExpirationDate(new LocalDateTime().plusDays(1));

        Mockito.when(planboardMessageRepository.findSinglePlanboardMessage(Matchers.eq(sequence), Matchers.eq(documentType),
                Matchers.eq(participantDomain))).thenReturn(flexRequest);
        // geldig
        try {
            MessageMetadata metaData = new MessageMetadata();
            metaData.setSenderDomain(participantDomain);
            FlexOffer flexOffer = new FlexOffer();
            flexOffer.setFlexRequestSequence(sequence);
            flexOffer.setMessageMetadata(metaData);
            PlanboardMessage document = validator.validatePlanboardMessageExpirationDate(sequence, DocumentType.FLEX_REQUEST, participantDomain);
            assertNotNull(document);
        } catch (BusinessValidationException e1) {
            fail("Should not throw exception");
        }
    }

    @Test
    public void testValidateExpiredFlexRequest() {
        DocumentType documentType = DocumentType.FLEX_REQUEST;
        Long sequence = 123123L;
        String participantDomain = "usef-example.com";
        PlanboardMessage flexRequest = new PlanboardMessage();

        flexRequest.setExpirationDate(new LocalDateTime().minusDays(1));

        Mockito.when(planboardMessageRepository.findSinglePlanboardMessage(Matchers.eq(sequence), Matchers.eq(documentType),
                Matchers.eq(participantDomain))).thenReturn(flexRequest);
        try {
            validator.validatePlanboardMessageExpirationDate(sequence, DocumentType.FLEX_REQUEST, participantDomain);
            fail("expected BusinessException");
        } catch (BusinessValidationException e) {
            assertEquals(CoreBusinessError.DOCUMENT_EXIRED, e.getBusinessError());
        }
    }

    @Test
    public void testValidateIfPTUForPeriodIsNotInPhase() {
        String entityAddress = "ea.234j124i90p12j4p194";
        LocalDate period = DateTimeUtil.parseDate("2014-12-17");

        Map<Integer, PtuContainer> ptuContainers = new HashMap<>();
        PtuContainer ptuc1 = new PtuContainer();
        ptuContainers.put(1, ptuc1);

        Mockito.when(connectionGroupRepository.find(entityAddress)).thenReturn(new CongestionPointConnectionGroup());

        PtuState ptuState = new PtuState();
        ptuState.setState(PtuContainerState.PendingSettlement);
        Mockito.when(ptuStateRepository.findOrCreatePtuState(Mockito.any(PtuContainer.class), Mockito.any(ConnectionGroup.class)))
                .thenReturn(ptuState);

        Mockito.when(ptuContainerRepository.findPtuContainersMap(period)).thenReturn(ptuContainers);
        // invalid
        try {
            validator.validateIfPTUForPeriodIsNotInPhase(entityAddress, period, PtuContainerState.PendingSettlement,
                    PtuContainerState.Settled);
            fail("Should throw exception");
        } catch (BusinessValidationException e) {
            assertEquals(CoreBusinessError.PTUS_IN_WRONG_PHASE, e.getBusinessError());
            assertEquals(
                    "PTU's for gridpoint ea.234j124i90p12j4p194 and period 2014-12-17 are all in one of the following phases [PendingSettlement, Settled].",
                    e.getMessage());
        }
        // valid
        try {
            validator.validateIfPTUForPeriodIsNotInPhase(entityAddress, period, PtuContainerState.Settled);
        } catch (BusinessValidationException e) {
            fail("Should not throw exception");
        }
    }

    @Test
    public void testValidateIfAllPTUForPeriodAreNotInPhase() {
        String usefIdentifier = "ea.234j124i90p12j4p194";
        LocalDate period = DateTimeUtil.parseDate("2014-12-17");

        List<PtuState> ptuStates = new ArrayList<>();

        PtuState ptuState1 = new PtuState();
        ptuState1.setState(PtuContainerState.PlanValidate);
        ptuStates.add(ptuState1);

        PtuState ptuState2 = new PtuState();
        ptuState2.setState(PtuContainerState.DayAheadClosedValidate);
        ptuStates.add(ptuState2);

        Mockito.when(ptuStateRepository.findPtuStates(period, usefIdentifier)).thenReturn(ptuStates);

        // invalid
        try {
            validator.validateIfAllPTUForPeriodAreNotInPhase(usefIdentifier, period, PtuContainerState.DayAheadClosedValidate,
                    PtuContainerState.Operate);
            fail("Should throw exception");
        } catch (BusinessValidationException e) {
            assertEquals(CoreBusinessError.PTUS_IN_WRONG_PHASE, e.getBusinessError());
        }

        // valid
        try {
            validator.validateIfAllPTUForPeriodAreNotInPhase(usefIdentifier, period, PtuContainerState.Operate,
                    PtuContainerState.Settled);
        } catch (BusinessValidationException e) {
            fail("Should not throw exception");
        }
    }

    @Test(expected = BusinessValidationException.class)
    public void testPtuPhaseWithOperatePhase() throws BusinessValidationException {
        FlexOfferRevocation flexOfferRevocation = buildFlexOfferRevocation();
        Map<Integer,PtuFlexOffer> flexOffers = buildFlexOffers().stream().collect(
                Collectors.toMap(fo -> fo.getPtuContainer().getPtuIndex(), Function.identity()));


        PtuState ptuState = new PtuState();
        ptuState.setState(PtuContainerState.Operate);

        Mockito.when(ptuStateRepository.findOrCreatePtuState(Matchers.any(PtuContainer.class), Matchers.any(ConnectionGroup.class)))
                .thenReturn(ptuState);

        validator.checkPtuPhase(flexOfferRevocation, flexOffers);
    }

    @Test
    public void testPtuPhaseWithoutOperatePhase() {
        FlexOfferRevocation flexOfferRevocation = buildFlexOfferRevocation();
        Map<Integer,PtuFlexOffer> flexOffers = buildFlexOffers().stream().collect(
                Collectors.toMap(fo -> fo.getPtuContainer().getPtuIndex(), Function.identity()));

        PtuState ptuState = new PtuState();
        ptuState.setState(PtuContainerState.PlanValidate);

        Mockito.when(ptuStateRepository.findOrCreatePtuState(Matchers.any(PtuContainer.class), Matchers.any(ConnectionGroup.class)))
                .thenReturn(ptuState);

        try {
            validator.checkPtuPhase(flexOfferRevocation, flexOffers);
        } catch (BusinessValidationException e) {
            fail("No BusinessValidationException expected");
        }
    }

    @Test(expected = BusinessValidationException.class)
    public void testCheckRelatedPlanboardMessagesExistWithEmptyPlanboardList() throws BusinessValidationException {
        FlexOfferRevocation flexOfferRevocation = buildFlexOfferRevocation();
        List<PlanboardMessage> planboardMessages = new ArrayList<>();

        PtuState ptuState = new PtuState();
        ptuState.setState(PtuContainerState.PlanValidate);

        Mockito.when(ptuStateRepository.findOrCreatePtuState(Matchers.any(PtuContainer.class), Matchers.any(ConnectionGroup.class)))
                .thenReturn(ptuState);

        validator.checkRelatedPlanboardMessagesExist(flexOfferRevocation, planboardMessages);
    }

    @Test
    public void testCheckRelatedPlanboardMessagesExistWithValidPlanboardList() {
        FlexOfferRevocation flexOfferRevocation = buildFlexOfferRevocation();

        PlanboardMessage planboardMessage = new PlanboardMessage();
        List<PlanboardMessage> planboardMessages = Collections.singletonList(planboardMessage);

        PtuState ptuState = new PtuState();
        ptuState.setState(PtuContainerState.PlanValidate);

        Mockito.when(ptuStateRepository.findOrCreatePtuState(Matchers.any(PtuContainer.class), Matchers.any(ConnectionGroup.class)))
                .thenReturn(ptuState);

        try {
            validator.checkRelatedPlanboardMessagesExist(flexOfferRevocation, planboardMessages);
        } catch (BusinessValidationException e) {
            fail("No BusinessValidationException expected");
        }
    }

    private FlexOfferRevocation buildFlexOfferRevocation() {
        FlexOfferRevocation flexOfferRevocation = new FlexOfferRevocation();
        MessageMetadata messageMetaData = new MessageMetadata();

        messageMetaData.setRecipientDomain("agr.usef-example.com");
        messageMetaData.setSenderDomain("brp.usef-example.com");
        flexOfferRevocation.setSequence(12345L);
        flexOfferRevocation.setMessageMetadata(messageMetaData);

        return flexOfferRevocation;
    }

    private List<PtuFlexOffer> buildFlexOffers() {
        PtuFlexOffer pfo1 = new PtuFlexOffer();
        pfo1.setPtuContainer(buildPtuContainer());

        return Collections.singletonList(pfo1);
    }

    private PtuContainer buildPtuContainer() {
        PtuContainer ptuc1 = new PtuContainer();
        ptuc1.setPtuIndex(1);

        return ptuc1;
    }

    private List<PtuContainer> buildPtuContainers(boolean hasPtuInOperatePhase) {
        PtuContainer ptuc1 = new PtuContainer();
        ptuc1.setPtuIndex(1);
        PtuContainer ptuc2 = new PtuContainer();
        ptuc2.setPtuIndex(2);
        PtuContainer ptuc3 = new PtuContainer();
        ptuc3.setPtuIndex(3);
        return Arrays.asList(ptuc1, ptuc2, ptuc3);
    }
}
