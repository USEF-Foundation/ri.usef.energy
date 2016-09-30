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

package energy.usef.mdc.workflow.altstep;

import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;
import energy.usef.core.workflow.dto.ConnectionMeterEventDto;
import energy.usef.core.workflow.dto.MeterDataQueryTypeDto;
import energy.usef.core.workflow.dto.MeterEventTypeDto;
import energy.usef.mdc.dto.ConnectionMeterDataDto;
import energy.usef.mdc.dto.MeterDataDto;
import energy.usef.mdc.dto.PtuMeterDataDto;
import energy.usef.mdc.pbcfeederimpl.PbcFeederService;
import energy.usef.mdc.workflow.meterdata.MeterDataQueryStepParameter;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This step generates data for all connections for all the requested days. Power values between -500 and 500 are generated for each
 * ptu. INPUT: - PTU_DURATION The duration of a PTU. - DATE_RANGE_START The start date of the requested days - DATE_RANGE_END The
 * end date of the requested days - CONNECTIONS The connections for which data should be generated. - META_DATA_QUERY_TYPE meta data
 * query type {@link MeterDataQueryTypeDto}
 * <p>
 * OUTPUT: - METER_DATA A List of {@link MeterDataDto} which contains all the generated data.
 */
public class MdcMeterDataQueryStubSpecificActual implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(MdcMeterDataQueryStubSpecificActual.class);
    private static final Random RANDOM = new Random();
    private static final int MINUTES_PER_HOUR = 60;
    private static final int MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR;
    private static final int FLIP_THE_COIN_SIDES = 5;

    @Inject
    private PbcFeederService pbcFeederService;

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        Integer ptuDuration = (Integer) context.getValue(MeterDataQueryStepParameter.IN.PTU_DURATION.name());
        @SuppressWarnings("unchecked")
        List<String> stateData = (List<String>) context.getValue(MeterDataQueryStepParameter.IN.CONNECTIONS.name());
        MeterDataQueryTypeDto meterDataQueryTypeDto = (MeterDataQueryTypeDto) context
                .getValue(MeterDataQueryStepParameter.IN.META_DATA_QUERY_TYPE.name());

        LocalDate startDate = (LocalDate) context.getValue(MeterDataQueryStepParameter.IN.DATE_RANGE_START.name());
        LocalDate endDate = (LocalDate) context.getValue(MeterDataQueryStepParameter.IN.DATE_RANGE_END.name());

        LOGGER.info(
                "PBC invoked with the connection list size {}, the start range day {}, the end range date {}, meter data query "
                        + "type {}", stateData.size(), startDate, endDate, meterDataQueryTypeDto);

        int days = Days.daysBetween(startDate, endDate).getDays();
        // Map days to MeterData objects
        List<MeterDataDto> meterDatas = IntStream
                .rangeClosed(0, days)
                .mapToObj(startDate::plusDays)
                .map(day -> fetchMeterDataDtoForDay(day, ptuDuration, stateData, meterDataQueryTypeDto))
                .filter(meterData -> meterData != null).collect(Collectors.toList());

        context.setValue(MeterDataQueryStepParameter.OUT.METER_DATA.name(), meterDatas);
        LOGGER.debug("Meter data DTO list with the size {} is generated", meterDatas.size());
        return context;
    }

    private MeterDataDto fetchMeterDataDtoForDay(LocalDate day, Integer ptuDuration, List<String> stateData,
            MeterDataQueryTypeDto meterDataQueryTypeDto) {

        MeterDataDto meterDataDto = new MeterDataDto();
        meterDataDto.setPeriod(day);

        LOGGER.debug("Calling PbcFeederService to retrieve uncontrolled load for day [{}]", day);
        Map<String, ConnectionMeterDataDto> uncontrolledLoadConnectionMeterDataDto = pbcFeederService
                .fetchUncontrolledLoad(day,
                        1, MINUTES_PER_DAY / ptuDuration, stateData)
                .stream()
                .collect(Collectors.toMap(ConnectionMeterDataDto::getEntityAddress, cmdto -> cmdto));

        if (meterDataQueryTypeDto == MeterDataQueryTypeDto.EVENTS) {
            // PBC takes a random decision whether to create connection meter events
            if (RANDOM.nextBoolean()) {
                stateData.stream()
                        .collect(Collectors.toMap(Function.identity(),
                                entityAddress -> generateConnectionMeterEventDto(entityAddress, day)))
                        .values()
                        .stream()
                        .forEach(list -> meterDataDto.getConnectionMeterEventDtos().addAll(list));
            }

            if (meterDataDto.getConnectionMeterEventDtos().isEmpty()) {
                return null;
            }
        } else {
            meterDataDto.getConnectionMeterDataDtos().addAll(
                    // get the connection state for that day and map it
                    stateData
                            .stream()
                            .map(entityAddress -> {
                                ConnectionMeterDataDto uncontrolledLoadData = uncontrolledLoadConnectionMeterDataDto
                                        .get(entityAddress);
                                ConnectionMeterDataDto connectionMeterDataDto = new ConnectionMeterDataDto();
                                connectionMeterDataDto.setEntityAddress(entityAddress);
                                fillPTUData(
                                        connectionMeterDataDto,
                                        PtuUtil.getNumberOfPtusPerDay(day, ptuDuration),
                                        uncontrolledLoadData == null ? new ArrayList<>() : uncontrolledLoadData
                                                .getPtuMeterDataDtos());
                                return connectionMeterDataDto;
                            }).collect(Collectors.toList()));
            if (meterDataDto.getConnectionMeterDataDtos().isEmpty()) {
                return null;
            }
        }

        return meterDataDto;
    }

    private List<ConnectionMeterEventDto> generateConnectionMeterEventDto(String entityAddress, LocalDate day) {
        List<ConnectionMeterEventDto> result = new ArrayList<>();

        ConnectionMeterEventDto connectionMeterEventDto1 = new ConnectionMeterEventDto();
        connectionMeterEventDto1.setEntityAddress(entityAddress);
        LocalDateTime eventDateTime1 = new LocalDateTime(day.toDateMidnight().toDate()).plusMinutes(RANDOM
                .nextInt(MINUTES_PER_HOUR));
        connectionMeterEventDto1.setEventDateTime(eventDateTime1);
        result.add(connectionMeterEventDto1);

        ConnectionMeterEventDto connectionMeterEventDto2 = new ConnectionMeterEventDto();
        connectionMeterEventDto2.setEntityAddress(entityAddress);
        LocalDateTime eventDateTime2 = new LocalDateTime(day.toDateMidnight().toDate()).plusMinutes(MINUTES_PER_HOUR + 1 + RANDOM
                .nextInt(MINUTES_PER_HOUR));
        connectionMeterEventDto2.setEventDateTime(eventDateTime2);

        int randomInt = RANDOM.nextInt(FLIP_THE_COIN_SIDES);
        if (randomInt == 0) {
            connectionMeterEventDto1.setEventType(MeterEventTypeDto.CAPACITY_MANAGEMENT);
            BigInteger eventData = BigInteger.valueOf(generateRandomNumber(-500, 500));
            connectionMeterEventDto1.setEventData(eventData);

            connectionMeterEventDto2.setEventType(MeterEventTypeDto.CAPACITY_MANAGEMENT);
            result.add(connectionMeterEventDto2);
        } else if (randomInt == 1) {
            connectionMeterEventDto1.setEventType(MeterEventTypeDto.CONNECTION_INTERRUPTION);

            connectionMeterEventDto2.setEventType(MeterEventTypeDto.CONNECTION_RESUMPTION);
            result.add(connectionMeterEventDto2);
        } else if (randomInt == 2) {
            connectionMeterEventDto1.setEventType(MeterEventTypeDto.CAPACITY_MANAGEMENT);
            if (RANDOM.nextBoolean()) {
                BigInteger eventData = BigInteger.valueOf(generateRandomNumber(-500, 500));
                connectionMeterEventDto1.setEventData(eventData);
            }
        } else if (randomInt == 3) {
            connectionMeterEventDto1.setEventType(MeterEventTypeDto.CONNECTION_INTERRUPTION);
        } else {
            connectionMeterEventDto1.setEventType(MeterEventTypeDto.CONNECTION_RESUMPTION);
        }

        return result;
    }

    private int generateRandomNumber(int minValue, int maxValue) {
        return RANDOM.nextInt((maxValue - minValue) + 1) + minValue;
    }

    private void fillPTUData(ConnectionMeterDataDto connectionMeterDataDto, Integer ptuCount,
            List<PtuMeterDataDto> uncontrolledLoadPtus) {
        List<Integer> actualPowerValues = Arrays.asList(15000, 17000, 19000, -15000, -17000, -19000, 1000, 3000, 5000,
                -5000, -3000, -1000, 5000, 7000, 9000, 11000, 13000, 15000, -11000, -13000, -15000, -5000, -7000, -9000,
                15000, 11000, 9000);
        Map<Integer, PtuMeterDataDto> ptuToUncontrolledLoad = uncontrolledLoadPtus.stream()
                .collect(Collectors.toMap(ptu -> ptu.getStart().intValue(), ptu -> ptu));
        IntStream.rangeClosed(1, ptuCount).forEach(index -> {
            PtuMeterDataDto ptuMeterDataDto = new PtuMeterDataDto();
            ptuMeterDataDto.setStart(BigInteger.valueOf(index));
            ptuMeterDataDto.setDuration(BigInteger.ONE);
            ptuMeterDataDto.setPower(BigInteger.ZERO);
            if (index <= actualPowerValues.size()) {
                ptuMeterDataDto.setPower(BigInteger.valueOf(actualPowerValues.get(index - 1)));
            } else {
                ptuMeterDataDto.setPower(BigInteger.valueOf(17000));
            }
            connectionMeterDataDto.getPtuMeterDataDtos().add(ptuMeterDataDto);
        });
    }
}
