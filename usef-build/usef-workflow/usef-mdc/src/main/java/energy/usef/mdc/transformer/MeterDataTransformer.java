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

package energy.usef.mdc.transformer;

import energy.usef.core.data.xml.bean.message.ConnectionMeterData;
import energy.usef.core.data.xml.bean.message.MeterData;
import energy.usef.core.data.xml.bean.message.PTUMeterData;
import energy.usef.core.workflow.transformer.MeterEventTransformer;
import energy.usef.mdc.dto.ConnectionMeterDataDto;
import energy.usef.mdc.dto.MeterDataDto;
import energy.usef.mdc.dto.PtuMeterDataDto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transformer class for entities related to the MeterData xml entity.
 */
public class MeterDataTransformer {

    private MeterDataTransformer() {
        // do nothing, prevent instantiation.
    }

    /**
     * Transforms a list of MeterDataDtos to a List of MeterData without changing the order.
     *
     * @param meterDataDtos {@link List} of {@link MeterDataDto}.
     * @return {@link List} of {@link MeterData}.
     */
    public static List<MeterData> transformListToXml(List<MeterDataDto> meterDataDtos) {
        if (meterDataDtos == null || meterDataDtos.isEmpty()) {
            return new ArrayList<>();
        }
        return meterDataDtos.stream().map(MeterDataTransformer::transformToXml).collect(Collectors.toList());
    }

    /**
     * Transforms a MeterDataDto to its correspondent XML object.
     *
     * @param meterDataDto {@link MeterDataDto} dto.
     * @return a {@link MeterData} xml object as specified by the xsd specification.
     */
    public static MeterData transformToXml(MeterDataDto meterDataDto) {
        if (meterDataDto == null) {
            return null;
        }
        MeterData meterData = new MeterData();
        meterData.setPeriod(meterDataDto.getPeriod());
        meterDataDto.getConnectionMeterDataDtos()
                .stream()
                .map(MeterDataTransformer::transformToXml)
                .forEach(connectionMeterData -> meterData.getConnectionMeterData().add(connectionMeterData));
        meterDataDto.getConnectionMeterEventDtos()
                .stream()
                .map(MeterEventTransformer::transformToXml)
                .forEach(connectionMeterEvent -> meterData.getConnectionMeterEvent().add(connectionMeterEvent));
        return meterData;
    }

    /**
     * Transforms a ConnectionMeterDataDto to its correspondent XML object.
     *
     * @param connectionMeterDataDto {@link ConnectionMeterDataDto} dto.
     * @return a {@link ConnectionMeterData} xml object as specified by the xsd specification.
     */
    public static ConnectionMeterData transformToXml(ConnectionMeterDataDto connectionMeterDataDto) {
        if (connectionMeterDataDto == null) {
            return null;
        }
        ConnectionMeterData connectionMeterData = new ConnectionMeterData();
        connectionMeterData.setEntityAddress(connectionMeterDataDto.getEntityAddress());
        connectionMeterData.setAGRDomain(connectionMeterDataDto.getAgrDomain());
        connectionMeterData.setEntityCount(connectionMeterDataDto.getEntityCount());
        connectionMeterDataDto.getPtuMeterDataDtos()
                .stream()
                .map(MeterDataTransformer::transformToXml)
                .forEach(ptuMeterData -> connectionMeterData.getPTUMeterData().add(ptuMeterData));
        return connectionMeterData;
    }

    /**
     * Transforms a PtuMeterDataDto to its correspondent XML object.
     *
     * @param ptuMeterDataDto {@link PtuMeterDataDto} dto.
     * @return a {@link PTUMeterData} xml object as specified by the xsd specification.
     */
    public static PTUMeterData transformToXml(PtuMeterDataDto ptuMeterDataDto) {
        if (ptuMeterDataDto == null) {
            return null;
        }
        PTUMeterData ptuMeterData = new PTUMeterData();
        ptuMeterData.setDuration(ptuMeterDataDto.getDuration());
        ptuMeterData.setPower(ptuMeterDataDto.getPower());
        ptuMeterData.setStart(ptuMeterDataDto.getStart());
        return ptuMeterData;
    }
}
