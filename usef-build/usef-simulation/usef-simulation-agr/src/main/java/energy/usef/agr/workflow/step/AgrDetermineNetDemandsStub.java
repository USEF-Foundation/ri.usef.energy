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

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.PowerDataDto;
import energy.usef.agr.dto.UdiPortfolioDto;
import energy.usef.agr.dto.device.capability.DeviceCapabilityDto;
import energy.usef.agr.dto.device.capability.IncreaseCapabilityDto;
import energy.usef.agr.dto.device.capability.ReportCapabilityDto;
import energy.usef.agr.dto.device.capability.UdiEventDto;
import energy.usef.agr.pbcfeederimpl.PbcFeederService;
import energy.usef.agr.workflow.operate.netdemand.DetermineNetDemandStepParameter.IN;
import energy.usef.agr.workflow.operate.netdemand.DetermineNetDemandStepParameter.OUT;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workflow step implementation for the Workflow 'AGR Determine Net Demands'. This implementation expects to find the following
 * parameters as input:
 * <ul>
 * <li>PERIOD: the period for which determine net demands will be executed.</li>
 * <li>PTU_DURATION: ptu duration in minutes ({@link Integer})</li>
 * <li>CONNECTION_PORTFOLIO_DTO_LIST: {@link List} of {@link ConnectionPortfolioDto} objects</li>
 * <li>UDI_EVENT_DTO_MAP: {@link Map} of {@link UdiEventDto} per Udi identifier per connection identifier.</li>
 * </ul>
 * Parameters as output:
 * <ul>
 * <li>CONNECTION_PORTFOLIO_DTO_LIST: List of {@link ConnectionPortfolioDto} containing the data of the Net Demand.</li>
 * <li>UPDATED_UDI_EVENT_DTO_LIST: {@link Map} of {@link UdiEventDto} updated with the new capabilities.</li>
 * </ul>
 */
public class AgrDetermineNetDemandsStub implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgrDetermineNetDemandsStub.class);
    private static final Random RANDOM = new Random();
    // forecast will randomly be multiplied between 0% and 6%
    private static final double FORECAST_RANDOM_FACTOR = 1.06;
    private static final int INCREASE_CAPABILITY_RANDOM_PERCENTAGE = 5;

    @Inject
    private PbcFeederService pbcFeederService;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public WorkflowContext invoke(WorkflowContext context) {

        Integer ptuDuration = context.get(IN.PTU_DURATION.name(), Integer.class);
        List<ConnectionPortfolioDto> connectionDtosToProcess = context.get(IN.CONNECTION_PORTFOLIO_DTO_LIST.name(), List.class);

        LOGGER.debug("Started Determine Net Demands for {} connections", connectionDtosToProcess.size());

        LocalDate period = context.get(IN.PERIOD.name(), LocalDate.class);

        if (connectionDtosToProcess.isEmpty()) {
            LOGGER.warn("No connection DTOs to process");
            context.setValue(OUT.CONNECTION_PORTFOLIO_DTO_LIST.name(), new ArrayList<>());
        } else {
            List<ConnectionPortfolioDto> connectionDtos = pbcFeederService.retrieveUDIListWithPvLoadAveragePower(period,
                    connectionDtosToProcess, ptuDuration);

            // set the uncontrolled load at connection level with the same value as the forecast
            setObservedLoadForConnectionLevel(connectionDtos, ptuDuration);

            randomizeForecasts(connectionDtos, context.get(IN.PTU_DURATION.name(), Integer.class));

            // Although this data will not be generated or consumed at this stage, the PBC should be designed to be able
            // to insert ADS events in the database as well. Given the complexity of the event data, this will most likely be
            // accomplished by inserting XML in the database.
            context.setValue(OUT.CONNECTION_PORTFOLIO_DTO_LIST.name(), connectionDtos);

            LOGGER.debug("Ended with {} connectionDtos", connectionDtos.size());
        }

        // update UdiEvents and capabilities
        Map<String, Map<String, List<UdiEventDto>>> udiEventDtoMap = context.get(IN.UDI_EVENT_DTO_MAP.name(), Map.class);
        updateUdiEvents(udiEventDtoMap);
        context.setValue(OUT.UPDATED_UDI_EVENT_DTO_LIST.name(), udiEventDtoMap.values().stream()
                .flatMap(map -> map.values().stream())
                .flatMap(Collection::stream).collect(Collectors.toList()));
        return context;
    }

    private void updateUdiEvents(Map<String, Map<String, List<UdiEventDto>>> udiEventsPerUdiPerConnection) {
        udiEventsPerUdiPerConnection.forEach((connection, udiMap) ->
                updateCapabilitiesForConnection(udiMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList())));
    }

    private void updateCapabilitiesForConnection(List<UdiEventDto> udiEventDtos) {
        // modify any first encountered Increase Capability in the complete list of udi events.
        udiEventDtos.stream().flatMap(udiEventDto -> udiEventDto.getDeviceCapabilities().stream())
                .filter(capability -> capability instanceof IncreaseCapabilityDto)
                .findAny()
                .ifPresent(increaseCapability -> {
                    BigInteger maxPower = ((IncreaseCapabilityDto) increaseCapability).getMaxPower();
                    maxPower = new BigDecimal(maxPower).multiply(generateRandomPercentageMultiplier()).toBigInteger();
                    ((IncreaseCapabilityDto) increaseCapability).setMaxPower(maxPower);
                });
        // creates/remove a report capability (if not/if present).
        ReportCapabilityDto reportCapabilityDto = null;
        for (UdiEventDto udiEventDto : udiEventDtos) {
            for (DeviceCapabilityDto deviceCapabilityDto : udiEventDto.getDeviceCapabilities()) {
                if (deviceCapabilityDto instanceof ReportCapabilityDto) {
                    reportCapabilityDto = (ReportCapabilityDto) deviceCapabilityDto;
                }
            }
            if (reportCapabilityDto != null) {
                udiEventDto.getDeviceCapabilities().remove(reportCapabilityDto);
                return;
            }
        }
        // report capability is null
        if (!udiEventDtos.isEmpty()) {
            reportCapabilityDto = new ReportCapabilityDto();
            reportCapabilityDto.setId(UUID.randomUUID().toString());
            udiEventDtos.get(0).getDeviceCapabilities().add(reportCapabilityDto);
        }
    }

    private void setObservedLoadForConnectionLevel(List<ConnectionPortfolioDto> connectionDtos, int ptuDuration) {
        int previousPtuIndex = PtuUtil.getPtuIndex(DateTimeUtil.getCurrentDateTime(), ptuDuration) - 1;
        connectionDtos.stream().forEach(
                connectionPortfolioDTO -> connectionPortfolioDTO.getConnectionPowerPerPTU()
                        .forEach((ptuIndex, powerContainer) -> {
                            if (previousPtuIndex == ptuIndex) {
                                powerContainer.setObserved(powerContainer.getForecast());
                            }
                        }));
    }

    // multiplies all the forecasts with a random value between 1 and FORECAST_RANDOM_FACTOR
    private void randomizeForecasts(List<ConnectionPortfolioDto> connectionDtos, Integer ptuDuration) {
        connectionDtos.forEach(connectionDto -> {
            if (connectionDto.getUdis() != null && !connectionDto.getUdis().isEmpty()) {
                // udi's available
                for (UdiPortfolioDto udiPortfolioDto : connectionDto.getUdis()) {
                    int pivotDtuIndex = determinePivotDtuIndex(udiPortfolioDto, ptuDuration);
                    udiPortfolioDto.getUdiPowerPerDTU().entrySet().stream()
                            .filter(entry -> entry.getKey() >= pivotDtuIndex)
                            .forEach(entry -> randomMultiply(entry.getValue().getForecast()));
                }

            } else {
                // no udi's available
                int nextPtuIndex = PtuUtil.getPtuIndex(DateTimeUtil.getCurrentDateTime(), ptuDuration) + 1;
                connectionDto.getConnectionPowerPerPTU().entrySet().stream()
                        .filter(entry -> entry.getKey() >= nextPtuIndex)
                        .forEach(entry -> randomMultiply(entry.getValue().getForecast()));
            }
        });
    }

    private int determinePivotDtuIndex(UdiPortfolioDto udiPortfolioDto, Integer ptuDuration) {
        // computes the DTU index of the next PTU, based on the current time.
        int currentPtuIndex = PtuUtil.getPtuIndex(DateTimeUtil.getCurrentDateTime(), ptuDuration);
        return (currentPtuIndex + 1) * udiPortfolioDto.getDtuSize() + 1;
    }

    private void randomMultiply(PowerDataDto powerDataDto) {
        // determine the random forecast multiplier
        BigDecimal multiplier = generateRandomFactor();

        powerDataDto.setUncontrolledLoad(calculateNewPower(multiplier, powerDataDto.getUncontrolledLoad()));

        powerDataDto.setAverageProduction(calculateNewPower(multiplier, powerDataDto.getAverageProduction()));

        powerDataDto.setAverageConsumption(calculateNewPower(multiplier, powerDataDto.getAverageConsumption()));

    }

    private BigInteger calculateNewPower(BigDecimal multiplier, BigInteger power) {
        if (power == null) {
            return power;
        }
        BigDecimal powerDecimal = new BigDecimal(power);
        powerDecimal = powerDecimal.multiply(multiplier).setScale(0, BigDecimal.ROUND_HALF_UP);
        return powerDecimal.toBigInteger();
    }

    // returns a random value between 1 and maxValue
    private BigDecimal generateRandomFactor() {
        double randomFactor = RANDOM.nextDouble() * (FORECAST_RANDOM_FACTOR - 1) + 1;
        return BigDecimal.valueOf(randomFactor);
    }

    /**
     * Builds a percentage to be apply by multiplication.
     * <p>
     * Example: 5% will produce a BigDecimal comprised between 0.95 and 1.05.
     */
    private BigDecimal generateRandomPercentageMultiplier() {
        return BigDecimal.ONE.add(BigDecimal
                .valueOf(RANDOM.nextInt(INCREASE_CAPABILITY_RANDOM_PERCENTAGE * 2) + 1 - INCREASE_CAPABILITY_RANDOM_PERCENTAGE)
                .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP));
    }

}
