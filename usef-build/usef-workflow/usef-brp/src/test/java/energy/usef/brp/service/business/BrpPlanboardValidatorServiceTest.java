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

package energy.usef.brp.service.business;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.AgrConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroupState;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.repository.ConnectionGroupStateRepository;
import energy.usef.core.repository.PlanboardMessageRepository;
import energy.usef.core.repository.PtuContainerRepository;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.util.DateTimeUtil;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class BrpPlanboardValidatorServiceTest {

    private BrpPlanboardValidatorService brpPlanboardValidatorService;

    @Mock
    private Config config;

    @Mock
    private PtuContainerRepository ptuContainerRepository;

    @Mock
    private ConnectionGroupStateRepository connectionGroupStateRepository;

    @Mock
    private PlanboardMessageRepository planboardMessageRepository;

    @Before
    public void init() {
        brpPlanboardValidatorService = new BrpPlanboardValidatorService();
        Whitebox.setInternalState(brpPlanboardValidatorService, "config", config);
        Whitebox.setInternalState(brpPlanboardValidatorService, "ptuContainerRepository", ptuContainerRepository);
        Whitebox.setInternalState(brpPlanboardValidatorService, "connectionGroupStateRepository", connectionGroupStateRepository);
        Whitebox.setInternalState(brpPlanboardValidatorService, "planboardMessageRepository", planboardMessageRepository);
    }

    @Test
    public void testValidatePtusSucess() throws BusinessValidationException {
        // given
        PowerMockito.when(config.getProperty(ConfigParam.PTU_DURATION)).thenReturn("15");

        List<PTU> ptuDtoList = IntStream.rangeClosed(1, 96).mapToObj(elem -> {
            PTU ptuDto = new PTU();
            ptuDto.setDuration(BigInteger.valueOf(15L));
            ptuDto.setPower(BigInteger.valueOf(1000L));
            return ptuDto;
        }).collect(Collectors.toList());

        // when
        brpPlanboardValidatorService.validatePtus(ptuDtoList);

        // then
        // no exceptions thrown
    }

    @Test(expected = BusinessValidationException.class)
    public void testValidatePtusTooBig() throws BusinessValidationException {
        // given
        PowerMockito.when(config.getProperty(ConfigParam.PTU_DURATION)).thenReturn("15");

        List<PTU> ptuDtoList = IntStream.rangeClosed(1, 96).mapToObj(elem -> {
            PTU ptuDto = new PTU();
            ptuDto.setDuration(BigInteger.valueOf(15L));
            ptuDto.setPower(BigInteger.valueOf(2000000001L));
            return ptuDto;
        }).collect(Collectors.toList());

        // when
        brpPlanboardValidatorService.validatePtus(ptuDtoList);
    }

    @Test(expected = BusinessValidationException.class)
    public void testValidatePtusWithTooManyPtus() throws Exception {
        // given
        PowerMockito.when(config.getProperty(ConfigParam.PTU_DURATION)).thenReturn("15");

        List<PTU> ptuDtoList = IntStream.rangeClosed(1, 140).mapToObj(elem -> {
            PTU ptuDto = new PTU();
            ptuDto.setDuration(BigInteger.valueOf(Long.parseLong("15")));
            ptuDto.setPower(BigInteger.valueOf(1000L));
            return ptuDto;
        }).collect(Collectors.toList());

        // when
        brpPlanboardValidatorService.validatePtus(ptuDtoList);

        // then
        // Expect exception
    }

    @Test
    public void testValidatePlanboardHasBeenInitialized() {
        final int numberOfPtus = 96;
        final LocalDate period = new LocalDate(2015, 7, 17);
        // stubbing
        Mockito.when(ptuContainerRepository.findPtuContainersMap(Matchers.any(LocalDate.class)))
                .then(call -> IntStream.rangeClosed(1, numberOfPtus)
                        .mapToObj(index -> new PtuContainer((LocalDate) call.getArguments()[0], index))
                        .collect(Collectors.toMap(PtuContainer::getPtuIndex, Function.identity())));
        List<PTU> ptus = IntStream.rangeClosed(1, numberOfPtus).mapToObj(index -> {
            PTU ptu = new PTU();
            ptu.setStart(BigInteger.valueOf(index));
            return ptu;
        }).collect(Collectors.toList());
        // actual invocation
        try {
            brpPlanboardValidatorService.validatePlanboardHasBeenInitialized(period, ptus);
        } catch (BusinessValidationException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test(expected = BusinessValidationException.class)
    public void testValidatePlanboardHasBeenInitializedThrowsException() throws BusinessValidationException {
        final int numberOfPtus = 96;
        final LocalDate period = new LocalDate(2015, 7, 17);
        // stubbing with missing PtuContainer (#96)
        Mockito.when(ptuContainerRepository.findPtuContainersMap(Matchers.any(LocalDate.class)))
                .then(call -> IntStream.rangeClosed(1, numberOfPtus - 1)
                        .mapToObj(index -> new PtuContainer((LocalDate) call.getArguments()[0], index))
                        .collect(Collectors.toMap(PtuContainer::getPtuIndex, Function.identity())));
        List<PTU> ptus = IntStream.rangeClosed(1, numberOfPtus).mapToObj(index -> {
            PTU ptu = new PTU();
            ptu.setStart(BigInteger.valueOf(index));
            return ptu;
        }).collect(Collectors.toList());
        // actual invocation
        brpPlanboardValidatorService.validatePlanboardHasBeenInitialized(period, ptus);
        Assert.fail("Exception should have been thrown.");
    }

    @Test
    public void testValidateAPlanConnectionGroup() {
        // stubbing of the ConnectionGroupStateRepository
        Mockito.when(connectionGroupStateRepository
                .findConnectionGroupStatesByUsefIdentifier(Matchers.eq("agr.usef-example.com"), Matchers.any(LocalDate.class)))
                .then(call -> {
            ConnectionGroupState cgs = new ConnectionGroupState();
            cgs.setConnection(new Connection("ean.000000000001"));
            cgs.setConnectionGroup(new AgrConnectionGroup("agr.usef-example.com"));
            return Collections.singletonList(cgs);
        });

        // actual invocation
        try {
            brpPlanboardValidatorService.validateAPlanConnectionGroup(new LocalDate(2015, 7, 17), "agr.usef-example.com");
        } catch (BusinessValidationException e) {
            Assert.fail(e.getMessage());
        }
        // verifications
        Mockito.verify(connectionGroupStateRepository, Mockito.times(1))
                .findConnectionGroupStatesByUsefIdentifier(Matchers.eq("agr.usef-example.com"),
                        Matchers.eq(new LocalDate(2015, 7, 17)));

    }

    @Test(expected = BusinessValidationException.class)
    public void testValidateAPlanConnectionGroupThrowsException() throws BusinessValidationException {

        // actual invocation
        brpPlanboardValidatorService.validateAPlanConnectionGroup(new LocalDate(2015, 7, 17), "none.agr.usef-example.com");
        // verifications
        Mockito.verify(connectionGroupStateRepository, Mockito.times(1))
                .findActiveConnectionGroupStatesOfType(Matchers.eq(new LocalDate(2015, 7, 17)),
                        Matchers.eq(AgrConnectionGroup.class));

    }

    @Test(expected = BusinessValidationException.class)
    public void testValidatePeriodPast() throws BusinessValidationException {
        brpPlanboardValidatorService.validatePeriod(DateTimeUtil.getCurrentDate().minusDays(1));
    }

    @Test
    public void testValidatePeriodFuture() {
        try {
            brpPlanboardValidatorService.validatePeriod(DateTimeUtil.getCurrentDate().plusDays(1));
        } catch (BusinessValidationException e) {
            Assert.fail("No BusinessValidationException expected");
        }
    }

    @Test(expected = BusinessValidationException.class)
    public void testValidateAPlanSequenceNumberFail() throws BusinessValidationException {
        Mockito.when(planboardMessageRepository
                .findMaxPlanboardMessageSequence(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
                .thenReturn(2l);

        Prognosis prognosis = new Prognosis();
        prognosis.setSequence(1l);
        prognosis.setPeriod(DateTimeUtil.getCurrentDate());
        prognosis.setCongestionPoint("ean.1");
        prognosis.setMessageMetadata(MessageMetadataBuilder.buildDefault());

        brpPlanboardValidatorService.validateAPlanSequenceNumber(prognosis);
    }

    @Test
    public void testValidateAPlanSequenceNumberSuccess() {
        Mockito.when(planboardMessageRepository
                .findMaxPlanboardMessageSequence(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
                .thenReturn(2l);

        Prognosis prognosis = new Prognosis();
        prognosis.setSequence(2l);
        prognosis.setPeriod(DateTimeUtil.getCurrentDate());
        prognosis.setCongestionPoint("ean.1");
        prognosis.setMessageMetadata(MessageMetadataBuilder.buildDefault());

        try {
            brpPlanboardValidatorService.validateAPlanSequenceNumber(prognosis);
        } catch (BusinessValidationException e) {
            Assert.fail("No BusinessValidationException expected");
        }
    }
}
