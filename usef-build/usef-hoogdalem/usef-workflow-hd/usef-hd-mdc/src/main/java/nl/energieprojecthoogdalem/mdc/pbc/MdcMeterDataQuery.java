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

package nl.energieprojecthoogdalem.mdc.pbc;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import info.usef.core.workflow.dto.MeterDataQueryTypeDto;
import info.usef.mdc.dto.ConnectionMeterDataDto;
import info.usef.mdc.dto.MeterDataDto;
import info.usef.mdc.dto.PtuMeterDataDto;
import info.usef.mdc.workflow.meterdata.MeterDataQueryStepParameter;
import info.usef.mdc.workflow.meterdata.MeterDataQueryStepParameter.IN;

/**
 * This step generates data for all connections for all the requested days. Power values between -500 and 500 are generated for each
 * ptu. INPUT: - PTU_DURATION The duration of a PTU. - DATE_RANGE_START The start date of the requested days - DATE_RANGE_END The
 * end date of the requested days - CONNECTIONS The connections for which data should be generated. - META_DATA_QUERY_TYPE meta data
 * query type {@link MeterDataQueryTypeDto}
 * <p>
 * OUTPUT: - METER_DATA A List of {@link MeterDataDto} which contains all the generated data.
 */
public class MdcMeterDataQuery implements WorkflowStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(MdcMeterDataQuery.class);
    private static final int MINUTES_PER_HOUR = 60;
    private static final int MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR;

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        Integer ptuDuration = (Integer) context.getValue(IN.PTU_DURATION.name());
        @SuppressWarnings("unchecked")
        List<String> stateData = (List<String>) context.getValue(IN.CONNECTIONS.name());
        MeterDataQueryTypeDto meterDataQueryTypeDto = (MeterDataQueryTypeDto) context
                .getValue(MeterDataQueryStepParameter.IN.META_DATA_QUERY_TYPE.name());

        LocalDate startDate = (LocalDate) context.getValue(IN.DATE_RANGE_START.name());
        LocalDate endDate = (LocalDate) context.getValue(IN.DATE_RANGE_END.name());

        LOGGER.info(
                "PBC invoked with the connection list size {}, the start range day {}, the end range date {}, meter data query "
                        + "type {}", stateData.size(), startDate, endDate, meterDataQueryTypeDto);

        int days = Days.daysBetween(startDate, endDate).getDays();
        // Map days to MeterData objects
        List<MeterDataDto> meterDatas = IntStream
                .rangeClosed(0, days)
                .mapToObj(startDate::plusDays)
                .map(day -> fetchMeterDataDtoForDay(day, ptuDuration, stateData))
                .filter(meterData -> meterData != null).collect(Collectors.toList());

        context.setValue(MeterDataQueryStepParameter.OUT.METER_DATA.name(), meterDatas);
        LOGGER.debug("Meter data DTO list with the size {} is generated", meterDatas.size());
        return context;
    }

    private MeterDataDto fetchMeterDataDtoForDay(LocalDate day, Integer ptuDuration, List<String> stateData) {

        MeterDataDto meterDataDto = new MeterDataDto();
        meterDataDto.setPeriod(day);

        meterDataDto.getConnectionMeterDataDtos().addAll(
                // get the connection state for that day and map it
                stateData
                        .stream()
                        .map(entityAddress -> {
                            ConnectionMeterDataDto connectionMeterDataDto = new ConnectionMeterDataDto();
                            connectionMeterDataDto.setEntityAddress(entityAddress);

                            PtuMeterDataDto ptuMeterDataDto = new PtuMeterDataDto();
                            ptuMeterDataDto.setStart(BigInteger.ONE);
                            ptuMeterDataDto.setPower(BigInteger.ZERO);
                            ptuMeterDataDto.setDuration(new BigInteger(String.valueOf(MINUTES_PER_DAY / ptuDuration)));

                            connectionMeterDataDto.getPtuMeterDataDtos().add(ptuMeterDataDto);

                            return connectionMeterDataDto;
                        }).collect(Collectors.toList()));
        if (meterDataDto.getConnectionMeterDataDtos().isEmpty()) {
            return null;
        }

        return meterDataDto;
    }
}
