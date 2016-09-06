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

package energy.usef.agr;

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.dto.UdiPortfolioDto;
import energy.usef.core.util.PtuUtil;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;

/**
 *
 */
public class PortfolioBuilder {

    private final int ptuSize;
    private final int dtuSize;
    private final LocalDate period;

    Set<ConnectionPortfolioDto> connectionPortfolioDtos;

    public PortfolioBuilder(LocalDate period, int ptuSize, int dtuSize) {
        // empty constructor, do nothing
        this.ptuSize = ptuSize;
        this.dtuSize = dtuSize;
        connectionPortfolioDtos = new HashSet<>();
        this.period = period;
    }

    public Set<ConnectionPortfolioDto> build() {
        return connectionPortfolioDtos;
    }

    public PortfolioBuilder withConnection(String entityAddress) {
        ConnectionPortfolioDto connectionPortfolioDto = new ConnectionPortfolioDto(entityAddress);
        connectionPortfolioDtos.add(connectionPortfolioDto);
        return this;
    }

    public PortfolioBuilder uncontrolledLoadForConnection(String entityAddress, long uncontrolledLoad) {
        Optional<ConnectionPortfolioDto> first = connectionPortfolioDtos.stream()
                .filter(connectionPortfolioDto -> connectionPortfolioDto.getConnectionEntityAddress().equals(entityAddress))
                .findFirst();
        ConnectionPortfolioDto connectionPortfolioDto = first.orElseGet(
                generateConnection.apply(entityAddress, connectionPortfolioDtos));
        IntStream.rangeClosed(1, getPtusPerDay()).forEach(index -> {
            connectionPortfolioDto.getConnectionPowerPerPTU()
                    .put(index, buildForecastUncontrolledLoad(index, BigInteger.valueOf(uncontrolledLoad)));
        });
        return this;
    }

    public PortfolioBuilder udisForConnections(String entityAddress, int udiAmount, String endpointPrefix, long powerPerDtu) {
        Optional<ConnectionPortfolioDto> first = connectionPortfolioDtos.stream()
                .filter(connectionPortfolioDto -> connectionPortfolioDto.getConnectionEntityAddress().equals(entityAddress))
                .findFirst();
        ConnectionPortfolioDto connectionPortfolioDto = first.orElseGet(
                generateConnection.apply(entityAddress, connectionPortfolioDtos));
        IntStream.rangeClosed(1, udiAmount)
                .mapToObj(index -> new UdiPortfolioDto(endpointPrefix + "000" + index, dtuSize, null))
                .peek(udi -> IntStream.rangeClosed(1, getDtusPerDay())
                        .forEach(index -> udi.getUdiPowerPerDTU()
                                .put(index, buildForecastAverageConsumption(index, BigInteger.valueOf(powerPerDtu))))).

                forEach(udi -> connectionPortfolioDto.getUdis().add(udi)
                );
        return this;
    }

    private PowerContainerDto buildForecastAverageConsumption(int index, BigInteger averageConsumption) {
        PowerContainerDto powerContainerDto = new PowerContainerDto(period, index);
        powerContainerDto.getForecast().setAverageConsumption(averageConsumption);
        return powerContainerDto;
    }

    private PowerContainerDto buildForecastUncontrolledLoad(int index, BigInteger uncontrolledLoad) {
        PowerContainerDto powerContainerDto = new PowerContainerDto(period, index);
        powerContainerDto.getForecast().setUncontrolledLoad(uncontrolledLoad);
        return powerContainerDto;
    }

    private int getDtusPerDay() {
        return getPtusPerDay() * getDtusPerPtu();
    }

    private int getDtusPerPtu() {
        return ptuSize / dtuSize;
    }

    private int getPtusPerDay() {
        return PtuUtil.getNumberOfPtusPerDay(period, ptuSize);
    }

    private static BiFunction<String, Set<ConnectionPortfolioDto>, Supplier<ConnectionPortfolioDto>> generateConnection =
            (entityAddress, existingConnections) -> {
                ConnectionPortfolioDto connectionPortfolioDto = new ConnectionPortfolioDto(entityAddress);
                if (!existingConnections.contains(connectionPortfolioDto)) {
                    existingConnections.add(connectionPortfolioDto);
                }
                return (Supplier<ConnectionPortfolioDto>) () -> connectionPortfolioDto;
            };
}
