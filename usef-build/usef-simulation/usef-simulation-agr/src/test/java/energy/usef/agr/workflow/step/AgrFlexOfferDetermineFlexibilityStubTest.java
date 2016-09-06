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

package energy.usef.agr.workflow.step;

import static java.util.stream.Collectors.toList;

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.ForecastPowerDataDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.dto.UdiPortfolioDto;
import energy.usef.agr.pbcfeederimpl.PbcFeederService;
import energy.usef.agr.workflow.validate.flexoffer.FlexOfferDetermineFlexibilityStepParameter;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.DispositionTypeDto;
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.core.workflow.dto.FlexRequestDto;
import energy.usef.core.workflow.dto.PtuFlexOfferDto;
import energy.usef.core.workflow.dto.PtuFlexRequestDto;
import energy.usef.core.workflow.dto.USEFRoleDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests related to the {@link AgrFlexOfferDetermineFlexibilityStub} PBC class.
 */
@RunWith(PowerMockRunner.class)
public class AgrFlexOfferDetermineFlexibilityStubTest {
    private static final LocalDate PERIOD = DateTimeUtil.parseDate("2015-06-17");
    private static final int PTU_DURATION = 120;
    private static final int DTU_DURATION = 40;
    private static final int PTUS_PER_DAY = 12;
    private static final int DTUS_PER_DAY = 36;
    private static final String CONGESTION_POINT_ENTITY_ADDRESS = "ean.123456789012345678";
    private final Random random = new Random();
    private AgrFlexOfferDetermineFlexibilityStub determineFlexibilityStub;

    @Mock
    private PbcFeederService pbcFeederService;

    @Before
    public void init() {
        determineFlexibilityStub = new AgrFlexOfferDetermineFlexibilityStub();
        Whitebox.setInternalState(determineFlexibilityStub, pbcFeederService);
    }

    @Test
    public void testInvoke() throws Exception {
        // initialize context
        WorkflowContext workflowContext = buildContext();
        // stubbing of the PbcFeeder
        stubPbcFeeder();

        // actual invocation
        WorkflowContext outputContext = determineFlexibilityStub.invoke(workflowContext);
        // assertions
        Assert.assertNotNull(outputContext);
        @SuppressWarnings("unchecked")
        List<FlexOfferDto> flexOfferDtos = (List<FlexOfferDto>) outputContext.get(FlexOfferDetermineFlexibilityStepParameter.OUT.FLEX_OFFER_DTO_LIST.name(), List.class);
        Assert.assertNotNull(flexOfferDtos);
        Assert.assertEquals(2, flexOfferDtos.size());

        for (FlexOfferDto flexOfferDto : flexOfferDtos) {
            for (PtuFlexOfferDto ptuFlexOfferDto : flexOfferDto.getPtus()) {
                Assert.assertEquals(-600, ptuFlexOfferDto.getPower().intValue());
            }
            Assert.assertNotNull(flexOfferDto.getFlexRequestSequenceNumber());
        }
    }

    @Test
    public void testInvokeWithoutPlacedOffers() {
        // initialize context
        WorkflowContext workflowContext = buildContext();
        workflowContext.setValue(FlexOfferDetermineFlexibilityStepParameter.IN.FLEX_OFFER_DTO_LIST.name(), new ArrayList<FlexOfferDto>());
        // stubbing of the PbcFeeder
        stubPbcFeeder();
        // actual invocation
        WorkflowContext outputContext = determineFlexibilityStub.invoke(workflowContext);
        // assertions
        Assert.assertNotNull(outputContext);
        @SuppressWarnings("unchecked")
        List<FlexOfferDto> flexOfferDtos = (List<FlexOfferDto>) outputContext.get(FlexOfferDetermineFlexibilityStepParameter.OUT.FLEX_OFFER_DTO_LIST.name(), List.class);
        Assert.assertNotNull(flexOfferDtos);
        Assert.assertEquals(2, flexOfferDtos.size());

        for (FlexOfferDto flexOfferDto : flexOfferDtos) {
            for (PtuFlexOfferDto ptuFlexOfferDto : flexOfferDto.getPtus()) {
                Assert.assertEquals(-600, ptuFlexOfferDto.getPower().intValue());
            }
            Assert.assertNotNull(flexOfferDto.getFlexRequestSequenceNumber());
        }
    }

    private WorkflowContext buildContext() {
        WorkflowContext workflowContext = new DefaultWorkflowContext();
        workflowContext.setValue(FlexOfferDetermineFlexibilityStepParameter.IN.FLEX_REQUEST_DTO_LIST.name(), buildFlexRequests());
        workflowContext.setValue(FlexOfferDetermineFlexibilityStepParameter.IN.FLEX_OFFER_DTO_LIST.name(), buildFlexOffers());
        workflowContext.setValue(FlexOfferDetermineFlexibilityStepParameter.IN.PERIOD.name(), PERIOD);
        workflowContext.setValue(FlexOfferDetermineFlexibilityStepParameter.IN.PTU_DURATION.name(), PTU_DURATION);
        workflowContext.setValue(FlexOfferDetermineFlexibilityStepParameter.IN.CONNECTION_PORTFOLIO_DTO.name(), buildConnectionPortfolio());
        workflowContext.setValue(FlexOfferDetermineFlexibilityStepParameter.IN.CONNECTION_GROUPS_TO_CONNECTIONS_MAP.name(), buildConnectionsPerConnectionGroupMap());
        return workflowContext;
    }

    private List<FlexRequestDto> buildFlexRequests() {
        return IntStream.rangeClosed(1, 2).mapToObj(i -> {
            FlexRequestDto flexRequestDto = new FlexRequestDto();
            IntStream.rangeClosed(1, PTUS_PER_DAY).mapToObj(index -> {
                PtuFlexRequestDto ptuFlexRequestDto = new PtuFlexRequestDto();
                ptuFlexRequestDto.setPtuIndex(BigInteger.valueOf(index));
                ptuFlexRequestDto.setPower(BigInteger.valueOf(-600));
                ptuFlexRequestDto.setDisposition(DispositionTypeDto.REQUESTED);
                return ptuFlexRequestDto;
            }).forEach(ptu -> flexRequestDto.getPtus().add(ptu));
            flexRequestDto.setSequenceNumber(Math.abs(random.nextLong()));
            flexRequestDto.setPrognosisSequenceNumber(Math.abs(random.nextLong()));
            flexRequestDto.setParticipantDomain("dso" + i + ".usef-example.com");
            flexRequestDto.setConnectionGroupEntityAddress(CONGESTION_POINT_ENTITY_ADDRESS);
            flexRequestDto.setPeriod(PERIOD);
            flexRequestDto.setParticipantRole(USEFRoleDto.DSO);
            return flexRequestDto;
        }).collect(toList());
    }

    /*
     * Already 2 offers, with 250
     */
    private List<FlexOfferDto> buildFlexOffers() {
        return IntStream.rangeClosed(1, 2).mapToObj(i -> {
            FlexOfferDto flexOfferDto = new FlexOfferDto();
            IntStream.rangeClosed(1, PTUS_PER_DAY).mapToObj(index -> {
                PtuFlexOfferDto ptuFlexOfferDto = new PtuFlexOfferDto();
                ptuFlexOfferDto.setPtuIndex(BigInteger.valueOf(index));
                ptuFlexOfferDto.setPower(BigInteger.valueOf(-250));
                ptuFlexOfferDto.setPrice(BigDecimal.TEN);
                return ptuFlexOfferDto;
            }).forEach(ptu -> flexOfferDto.getPtus().add(ptu));
            flexOfferDto.setSequenceNumber(Math.abs(random.nextLong()));
            flexOfferDto.setFlexRequestSequenceNumber(Math.abs(random.nextLong()));
            flexOfferDto.setParticipantDomain("dso" + i + ".usef-example.com");
            flexOfferDto.setConnectionGroupEntityAddress(CONGESTION_POINT_ENTITY_ADDRESS);
            flexOfferDto.setPeriod(PERIOD);
            return flexOfferDto;
        }).collect(toList());
    }

    private Map<String, List<String>> buildConnectionsPerConnectionGroupMap() {
        Map<String, List<String>> connectionsPerConnectionGroup = new HashMap<>();
        connectionsPerConnectionGroup.put(CONGESTION_POINT_ENTITY_ADDRESS, new ArrayList<>());
        connectionsPerConnectionGroup.get(CONGESTION_POINT_ENTITY_ADDRESS).addAll(IntStream.rangeClosed(1, 3)
                .mapToObj(i -> "ean.00000000000" + i)
                .collect(toList()));
        return connectionsPerConnectionGroup;
    }

    /*
     * 3 connections, 2 udis per connection, 3DTUs per PTU.
     * Average potential flex per udi per ptu: 200 (PRODUCTION)
     * Average potential flex per connection per ptu: 400 (2 UDIS * 200)
     * Average potentail flex per connection group per ptu: 1200 (3 Connections * 400)
     */
    private List<ConnectionPortfolioDto> buildConnectionPortfolio() {
        return IntStream.rangeClosed(1, 3)
                .mapToObj(i -> "ean.00000000000" + i)
                .map(ConnectionPortfolioDto::new)
                .peek(connectionPortfolioDTO -> {
                    IntStream.rangeClosed(1, 2).mapToObj(udiNumber -> {
                        UdiPortfolioDto udiPortfolioDto = new UdiPortfolioDto("udi" + udiNumber, DTU_DURATION, "BEMS");
                        IntStream.rangeClosed(1, DTUS_PER_DAY)
                                .forEach(index -> udiPortfolioDto.getUdiPowerPerDTU().put(index, buildUdiPowerContainerDto(index)));
                        return udiPortfolioDto;
                    }).forEach(udi -> connectionPortfolioDTO.getUdis().add(udi));
                }).collect(Collectors.toList());
    }

    /*
     * each power container for a dtu will have 200 potential PRODUCTION (-100 in consumption, 100 in production).
     */
    private PowerContainerDto buildUdiPowerContainerDto(int index) {
        PowerContainerDto powerContainerDto = new PowerContainerDto(PERIOD, index);
        ForecastPowerDataDto forecastPowerDataDto = new ForecastPowerDataDto();
        forecastPowerDataDto.setPotentialFlexConsumption(BigInteger.valueOf(-100)); // can consume less
        forecastPowerDataDto.setPotentialFlexProduction(BigInteger.valueOf(100)); // can produce more as well
        powerContainerDto.setForecast(forecastPowerDataDto);
        return powerContainerDto;
    }

    /**
     * <p> Creates stubs for the calls the PbcFeederService. </p>
     * <p> PbcFeeder will return a list of uncontrolled load per ptu (forecast average power = ptuIndex + 10) per connection (for
     * all the ptus of the period). </p>
     * <p> PbcFeeder will return a list APX prices for the period specified in the call (Price = PtuIndex * 10). </p>
     */
    @SuppressWarnings("unchecked")
    private void stubPbcFeeder() {
        Mockito.when(pbcFeederService.retrieveApxPrices(Matchers.any(LocalDate.class),
                Matchers.anyInt(),
                Matchers.anyInt())).then(invocation -> {
            Map<Integer, BigDecimal> apxPrices = new HashMap<>();
            int startPtu = (int) invocation.getArguments()[1];
            for (int i = startPtu; i < startPtu + (int) invocation.getArguments()[2]; ++i) {
                apxPrices.put(i, BigDecimal.valueOf(i).multiply(BigDecimal.TEN));
            }
            return apxPrices;
        });
    }
}
