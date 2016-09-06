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

package energy.usef.agr.transformer;

import energy.usef.agr.dto.ConnectionGroupPortfolioDto;
import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.model.ForecastPowerData;
import energy.usef.agr.model.PowerContainer;
import energy.usef.agr.model.PowerData;
import energy.usef.agr.model.Udi;
import energy.usef.agr.model.UdiPowerContainer;
import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.workflow.dto.USEFRoleDto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link ConnectionPortfolioTransformer} class.
 */
public class ConnectionPortfolioTransformerTest {

    public static final String BRP_USEF_IDENTIFIER = "brp.usef-example.com";
    public static final String CONGESTION_POINT_IDENTIFIER = "ean.123456789012345678";
    public static final int PTUS_PER_DAY = 96;

    @Test
    public void testTransformToDto() {
        // variables
        final LocalDate period = new LocalDate(2015, 8, 24);
        Map<Connection, List<PowerContainer>> powerContainersPerConnection = Collections.singletonMap(
                new Connection("ean.0000000001"),
                IntStream.rangeClosed(1, 12).mapToObj(index -> {
                    PowerContainer powerContainer = new PowerContainer(period, index);
                    ForecastPowerData forecast = new ForecastPowerData();
                    forecast.setUncontrolledLoad(BigInteger.TEN);
                    powerContainer.setForecast(forecast);
                    return powerContainer;
                }).collect(Collectors.toList()));
        Udi udi = new Udi();
        List<PowerContainer> powerContainerList = IntStream.rangeClosed(1, 12).mapToObj(index -> {
            UdiPowerContainer powerContainer = new UdiPowerContainer(udi, period, index);
            ForecastPowerData forecast = new ForecastPowerData();
            forecast.setAverageConsumption(BigInteger.ONE);
            powerContainer.setForecast(forecast);
            return powerContainer;
        }).collect(Collectors.toList());

        Map<Udi, List<PowerContainer>> udiPowerContainersMap = new HashMap<>();
        udiPowerContainersMap.put(udi, powerContainerList);

        Map<Connection, List<Udi>> udisPerConnection = Collections.singletonMap(
                new Connection("ean.0000000001"), Collections.singletonList(udi));
        // invocation
        List<ConnectionPortfolioDto> connectionPortfolioDTOs = ConnectionPortfolioTransformer
                .transformToDTO(new ArrayList(udisPerConnection.keySet()), powerContainersPerConnection, udisPerConnection, udiPowerContainersMap);
        // verifications
        Assert.assertEquals(1, connectionPortfolioDTOs.size());
        ConnectionPortfolioDto connectionPortfolioDTO = connectionPortfolioDTOs.get(0);
        Assert.assertEquals("ean.0000000001", connectionPortfolioDTO.getConnectionEntityAddress());
        Assert.assertEquals(BigInteger.TEN,
                connectionPortfolioDTO.getConnectionPowerPerPTU().get(1).getForecast().getUncontrolledLoad());
        Assert.assertEquals(12, connectionPortfolioDTO.getConnectionPowerPerPTU().size());
        Assert.assertEquals(1, connectionPortfolioDTO.getUdis().size());
        Assert.assertEquals(12, connectionPortfolioDTO.getUdis().get(0).getUdiPowerPerDTU().size());
        Assert.assertEquals(BigInteger.ONE,
                connectionPortfolioDTO.getUdis().get(0).getUdiPowerPerDTU().get(1).getForecast().calculatePower());
    }

    @Test
    public void testTransformConnectionGroupPowerContainersToDTO() {
        // variables and input
        Map<ConnectionGroup, List<PowerContainer>> connectionGroupPowerContainersMap = buildConnectionGroupPowerContainers(
                new LocalDate(2015, 12, 3));
        // actual invocation
        List<ConnectionGroupPortfolioDto> connectionGroupPortfolioDtos = ConnectionPortfolioTransformer.transformToDTO(
                connectionGroupPowerContainersMap);
        // assertions
        Assert.assertEquals(2, connectionGroupPortfolioDtos.size());
        assertCongestionPointValues(connectionGroupPortfolioDtos.stream()
                .filter(connectionGroupPortfolioDto -> connectionGroupPortfolioDto.getUsefRole() == USEFRoleDto.DSO)
                .findFirst());
        assertBrpConnectionGroupValues(connectionGroupPortfolioDtos.stream()
                .filter(connectionGroupPortfolioDto -> connectionGroupPortfolioDto.getUsefRole() == USEFRoleDto.BRP)
                .findFirst());
    }

    private void assertCongestionPointValues(Optional<ConnectionGroupPortfolioDto> connectionGroupPortfolioDto) {
        Assert.assertNotNull(connectionGroupPortfolioDto);
        ConnectionGroupPortfolioDto item = connectionGroupPortfolioDto.get();
        Assert.assertEquals(USEFRoleDto.DSO, item.getUsefRole());
        Assert.assertEquals(CONGESTION_POINT_IDENTIFIER, item.getUsefIdentifier());
        Assert.assertEquals(96, item.getConnectionGroupPowerPerPTU().size());
        item.getConnectionGroupPowerPerPTU().forEach((index, powerContainerDto) -> {
            int expectedPower = index + 100;
            Assert.assertEquals(expectedPower, powerContainerDto.getObserved().getAverageConsumption().intValue());
            Assert.assertEquals(expectedPower, powerContainerDto.getMostAccurateAverageConsumption().intValue());
        });
    }

    private void assertBrpConnectionGroupValues(Optional<ConnectionGroupPortfolioDto> connectionGroupPortfolioDto) {
        Assert.assertNotNull(connectionGroupPortfolioDto);
        ConnectionGroupPortfolioDto item = connectionGroupPortfolioDto.get();
        Assert.assertEquals(USEFRoleDto.BRP, item.getUsefRole());
        Assert.assertEquals(BRP_USEF_IDENTIFIER, item.getUsefIdentifier());
        Assert.assertEquals(96, item.getConnectionGroupPowerPerPTU().size());
        item.getConnectionGroupPowerPerPTU().forEach((index, powerContainerDto) -> {
            int expectedPower = index + 100;
            Assert.assertEquals(expectedPower, powerContainerDto.getObserved().getAverageConsumption().intValue());
            Assert.assertEquals(expectedPower, powerContainerDto.getMostAccurateAverageConsumption().intValue());
        });
    }

    private Map<ConnectionGroup, List<PowerContainer>> buildConnectionGroupPowerContainers(LocalDate period) {
        Map<ConnectionGroup, List<PowerContainer>> map = new HashMap<>();
        BrpConnectionGroup brpConnectionGroup = new BrpConnectionGroup();
        brpConnectionGroup.setUsefIdentifier(BRP_USEF_IDENTIFIER);
        brpConnectionGroup.setBrpDomain(BRP_USEF_IDENTIFIER);
        CongestionPointConnectionGroup congestionPointConnectionGroup = new CongestionPointConnectionGroup();
        congestionPointConnectionGroup.setUsefIdentifier(CONGESTION_POINT_IDENTIFIER);
        congestionPointConnectionGroup.setDsoDomain("dso.usef-example.com");
        map.put(brpConnectionGroup, IntStream.rangeClosed(1, PTUS_PER_DAY)
                .mapToObj(index -> buildPowerContainer(period, index))
                .collect(Collectors.toList()));
        map.put(congestionPointConnectionGroup, IntStream.rangeClosed(1, PTUS_PER_DAY)
                .mapToObj(index -> buildPowerContainer(period, index))
                .collect(Collectors.toList()));
        return map;
    }

    private PowerContainer buildPowerContainer(LocalDate period, int index) {
        PowerContainer powerContainer = new PowerContainer(period, index);
        PowerData observed = new PowerData();
        observed.setAverageConsumption(BigInteger.valueOf(100).add(BigInteger.valueOf(index)));
        powerContainer.setObserved(observed);
        return powerContainer;
    }

}
