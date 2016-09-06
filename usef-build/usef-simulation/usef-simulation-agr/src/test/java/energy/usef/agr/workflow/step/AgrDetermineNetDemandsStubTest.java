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

import static org.junit.Assert.assertTrue;

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.dto.UdiPortfolioDto;
import energy.usef.agr.dto.device.capability.DeviceCapabilityDto;
import energy.usef.agr.dto.device.capability.IncreaseCapabilityDto;
import energy.usef.agr.dto.device.capability.ReportCapabilityDto;
import energy.usef.agr.dto.device.capability.UdiEventDto;
import energy.usef.agr.pbcfeederimpl.PbcFeederService;
import energy.usef.agr.workflow.operate.reoptimize.ReOptimizePortfolioStepParameter;
import energy.usef.agr.workflow.operate.netdemand.DetermineNetDemandStepParameter;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
 * Test class in charge of the unit tests related to the {@link AgrDetermineNetDemandsStub}.
 */
@RunWith(PowerMockRunner.class)
public class AgrDetermineNetDemandsStubTest {

    private static final int PTU_SIZE = 15;
    private static final double FORECAST_RANDOM_FACTOR = 1.06;   // forecast will randomly be increased between 0% and 6%
    private static final String[] CONNECTION_ADDRESSES = new String[] {
            "ean.100000000001",
            "ean.100000000002",
            "ean.100000000003"
    };

    private AgrDetermineNetDemandsStub agrDetermineNetDemandsStub;

    @Mock
    private PbcFeederService pbcFeederService;

    @Before
    public void setUp() throws Exception {
        agrDetermineNetDemandsStub = new AgrDetermineNetDemandsStub();
        Whitebox.setInternalState(agrDetermineNetDemandsStub, pbcFeederService);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvoke() throws Exception {
        WorkflowContext context = buildInputContext();
        List<ConnectionPortfolioDto> connectionPortfolio = context
                .get(DetermineNetDemandStepParameter.IN.CONNECTION_PORTFOLIO_DTO_LIST.name(), List.class);
        Mockito.when(pbcFeederService.retrieveUDIListWithPvLoadAveragePower(Matchers.any(LocalDate.class),
                Matchers.anyListOf(ConnectionPortfolioDto.class), Matchers.eq(PTU_SIZE))).thenReturn(connectionPortfolio);

        WorkflowContext outputContext = agrDetermineNetDemandsStub.invoke(context);
        List<ConnectionPortfolioDto> updatedConnectionPortfolio = (List<ConnectionPortfolioDto>) outputContext
                .getValue(ReOptimizePortfolioStepParameter.OUT.CONNECTION_PORTFOLIO_OUT.name());

        // verifications
        Mockito.verify(pbcFeederService, Mockito.times(1)).retrieveUDIListWithPvLoadAveragePower(Matchers.any(LocalDate.class),
                Matchers.anyListOf(ConnectionPortfolioDto.class), Matchers.eq(PTU_SIZE));
        Assert.assertNotNull(outputContext);
        List<ConnectionPortfolioDto> connectionPortfolioDTOs = outputContext.get(DetermineNetDemandStepParameter.OUT.CONNECTION_PORTFOLIO_DTO_LIST.name(),
                List.class);
        List<UdiEventDto> udiEventDtos = outputContext.get(DetermineNetDemandStepParameter.OUT.UPDATED_UDI_EVENT_DTO_LIST.name(), List.class);
        Assert.assertEquals(3, connectionPortfolioDTOs.size());
        Assert.assertEquals(3, udiEventDtos.size());
        for (ConnectionPortfolioDto connectionPortfolioDTO : connectionPortfolioDTOs) {
            for (UdiPortfolioDto udiPortfolioDto : connectionPortfolioDTO.getUdis()) {
                for (PowerContainerDto udiPowerContainerDto : udiPortfolioDto.getUdiPowerPerDTU().values()) {
                    BigInteger forecast = udiPowerContainerDto.getForecast().calculatePower();

                    assertTrue("Forecast is expected to be between 200 and 212, but is " + forecast,
                            forecast.compareTo(BigInteger.valueOf(200)) >= 0
                                    && forecast.compareTo(BigInteger.valueOf(Math.round(200 * FORECAST_RANDOM_FACTOR))) <= 0);
                }
            }
        }
        for (UdiEventDto udiEventDto : udiEventDtos) {
            for (DeviceCapabilityDto deviceCapabilityDto : udiEventDto.getDeviceCapabilities()) {
                if (deviceCapabilityDto instanceof IncreaseCapabilityDto) {
                    IncreaseCapabilityDto increaseCapabilityDto = (IncreaseCapabilityDto) deviceCapabilityDto;
                    assertTrue(increaseCapabilityDto.getMaxPower().compareTo(BigInteger.valueOf(1050)) < 1
                            && increaseCapabilityDto.getMaxPower().compareTo(BigInteger.valueOf(950)) > -1);
                }
            }
        }
    }

    private WorkflowContext buildInputContext() {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(DetermineNetDemandStepParameter.IN.PTU_DURATION.name(), PTU_SIZE);
        context.setValue(DetermineNetDemandStepParameter.IN.CONNECTION_PORTFOLIO_DTO_LIST.name(), buildConnectionPortfolioDtos());
        context.setValue(DetermineNetDemandStepParameter.IN.UDI_EVENT_DTO_MAP.name(), buildUdiEventDtoMap());
        return context;
    }

    private List<ConnectionPortfolioDto> buildConnectionPortfolioDtos() {
        List<ConnectionPortfolioDto> portfolio = new ArrayList<>();
        Stream.of(CONNECTION_ADDRESSES).map(ConnectionPortfolioDto::new).forEach(connectionPortfolioDTO -> {
            connectionPortfolioDTO.getUdis().addAll(buildUdis(connectionPortfolioDTO.getConnectionEntityAddress()));
            portfolio.add(connectionPortfolioDTO);
        });
        return portfolio;
    }

    private List<UdiPortfolioDto> buildUdis(String connectionEntityAddress) {
        List<UdiPortfolioDto> udis = new ArrayList<>();

        for (int count = 1; count <= 2; count++) {
            UdiPortfolioDto udiPortfolioDto = new UdiPortfolioDto(connectionEntityAddress + ":" + count, PTU_SIZE, "Storage");

            IntStream.rangeClosed(1, 96).forEach(dtuIndex -> {
                PowerContainerDto powerContainerDto = new PowerContainerDto(new LocalDate(), dtuIndex);
                powerContainerDto.getForecast().setUncontrolledLoad(BigInteger.valueOf(100));
                powerContainerDto.getForecast().setAverageProduction(BigInteger.ZERO);
                powerContainerDto.getForecast().setAverageConsumption(BigInteger.valueOf(100));

                udiPortfolioDto.getUdiPowerPerDTU().put(dtuIndex, powerContainerDto);
            });

            udis.add(udiPortfolioDto);
        }

        return udis;
    }

    private Map<String, Map<String, List<UdiEventDto>>> buildUdiEventDtoMap() {
        Map<String, Map<String, List<UdiEventDto>>> result = new HashMap<>();
        Stream.of(CONNECTION_ADDRESSES).forEach(connection -> result.put(connection, buildUdiMap(connection)));
        return result;
    }

    private Map<String, List<UdiEventDto>> buildUdiMap(String connectionAddress) {
        Map<String, List<UdiEventDto>> udiMap = new HashMap<>();
        UdiEventDto udiEventDto = new UdiEventDto();
        udiEventDto.getDeviceCapabilities().add(buildIncreaseCapabilityDto());
        // add only one report capability
        if (connectionAddress.equals(CONNECTION_ADDRESSES[0])) {
            udiEventDto.getDeviceCapabilities().add(new ReportCapabilityDto());
        }
        udiMap.put(connectionAddress + "1", Collections.singletonList(udiEventDto));
        return udiMap;
    }

    private IncreaseCapabilityDto buildIncreaseCapabilityDto() {
        IncreaseCapabilityDto increaseCapabilityDto = new IncreaseCapabilityDto();
        increaseCapabilityDto.setMaxPower(BigInteger.valueOf(1000));
        return increaseCapabilityDto;
    }

}
