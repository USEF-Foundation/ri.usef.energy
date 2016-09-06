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

package energy.usef.core.workflow.transformer;

import energy.usef.core.data.xml.bean.message.ConnectionMeterData;
import energy.usef.core.data.xml.bean.message.MeterData;
import energy.usef.core.data.xml.bean.message.MeterDataSet;
import energy.usef.core.data.xml.bean.message.PTUMeterData;
import energy.usef.core.workflow.dto.ConnectionMeterDataDto;
import energy.usef.core.workflow.dto.MeterDataDto;
import energy.usef.core.workflow.dto.MeterDataSetDto;
import energy.usef.core.workflow.dto.PtuMeterDataDto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transformer class to map XML generated Meter Data object to DTOs.
 */
public class MeterDataTransformer {

    private MeterDataTransformer() {
        // prevent instantiations
    }

    /**
     * Transforms a list of MeterDataSets.
     *
     * @param meterDataSets {@link List} of {@link MeterDataSet}.
     * @return a {@link List} of {@link MeterDataSetDto}.
     */
    public static List<MeterDataSetDto> transform(List<MeterDataSet> meterDataSets) {
        if (meterDataSets == null || meterDataSets.isEmpty()) {
            return new ArrayList<>();
        }
        return meterDataSets.stream().map(MeterDataTransformer::transform).collect(Collectors.toList());
    }

    /**
     * Transforms a MeterDataSet.
     *
     * @param meterDataSet a {@link MeterDataSet}.
     * @return a {@link MeterDataSetDto}.
     */
    public static MeterDataSetDto transform(MeterDataSet meterDataSet) {
        MeterDataSetDto meterDataSetDto = new MeterDataSetDto(meterDataSet.getEntityAddress());
        meterDataSet.getMeterData().stream().forEach(meterData -> meterDataSetDto.getMeterDataDtos().add(transform(meterData)));
        return meterDataSetDto;
    }

    /**
     * Transforms a MeterData.
     *
     * @param meterData a {@link MeterData}.
     * @return a {@link MeterDataDto}.
     */
    public static MeterDataDto transform(MeterData meterData) {
        MeterDataDto meterDataDto = new MeterDataDto();
        meterDataDto.setPeriod(meterData.getPeriod());
        meterData.getConnectionMeterData().stream()
                .forEach(connectionMeterData -> meterDataDto.getConnectionMeterDataDtos().add(transform(connectionMeterData)));
        return meterDataDto;
    }

    /**
     * Transforms a ConnectionMeterData.
     *
     * @param connectionMeterData a {@link ConnectionMeterData}.
     * @return a {@link ConnectionMeterDataDto}.
     */
    public static ConnectionMeterDataDto transform(ConnectionMeterData connectionMeterData) {
        ConnectionMeterDataDto connectionMeterDataDto = new ConnectionMeterDataDto();
        connectionMeterDataDto.setAgrDomain(connectionMeterData.getAGRDomain());
        connectionMeterDataDto.setEntityAddress(connectionMeterData.getEntityAddress());
        connectionMeterDataDto.setEntityCount(connectionMeterData.getEntityCount());
        connectionMeterData.getPTUMeterData().stream()
                .forEach(ptuMeterData -> connectionMeterDataDto.getPtuMeterDataDtos().addAll(transform(ptuMeterData)));
        return connectionMeterDataDto;
    }

    /**
     * Transforms a PTUMeterData.
     *
     * @param ptuMeterData a {@link PTUMeterData}.
     * @return a {@link PtuMeterDataDto}.
     */
    public static List<PtuMeterDataDto> transform(PTUMeterData ptuMeterData) {
        List<PtuMeterDataDto> ptuMeterDataDtos = new ArrayList<>();
        for (int index = ptuMeterData.getStart().intValue();
             index < ptuMeterData.getStart().add(ptuMeterData.getDuration()).intValue();
             ++index) {
            PtuMeterDataDto ptuMeterDataDto = new PtuMeterDataDto(index);
            ptuMeterDataDto.setPower(ptuMeterData.getPower());
            ptuMeterDataDtos.add(ptuMeterDataDto);
        }
        return ptuMeterDataDtos;
    }

}
