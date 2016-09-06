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

import energy.usef.agr.dto.device.request.ConsumptionProductionTypeDto;
import energy.usef.agr.dto.device.request.DeviceMessageDto;
import energy.usef.agr.dto.device.request.IncreaseRequestDto;
import energy.usef.agr.dto.device.request.InterruptRequestDto;
import energy.usef.agr.dto.device.request.ReduceRequestDto;
import energy.usef.agr.dto.device.request.ReportRequestDto;
import energy.usef.agr.dto.device.request.ShiftRequestDto;
import energy.usef.agr.model.ConsumptionProductionType;
import energy.usef.agr.model.DeviceMessage;
import energy.usef.agr.model.DeviceMessageStatus;
import energy.usef.agr.model.DeviceRequest;
import energy.usef.agr.model.IncreaseRequest;
import energy.usef.agr.model.InterruptRequest;
import energy.usef.agr.model.ReduceRequest;
import energy.usef.agr.model.ReportRequest;
import energy.usef.agr.model.ShiftRequest;
import energy.usef.agr.model.Udi;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Transformer class to convert DeviceMessage from the xml format to the DTO format.
 */
public class DeviceMessageTransformer {

    public static final String DTU_INDEX_SEPERATOR = ",";

    private DeviceMessageTransformer() {
        // do nothing, just to prevent instantiation.
    }

    /**
     * Transforms a {@link DeviceMessageDto} to its related {@link DeviceMessage} format.
     *
     * @param deviceMessageDto {@link DeviceMessageDto} device message dto.
     * @return a {@link DeviceMessage} with the same data.
     */
    public static DeviceMessage transform(DeviceMessageDto deviceMessageDto, Udi udi) {
        DeviceMessage deviceMessage = new DeviceMessage();
        deviceMessage.setEndpoint(deviceMessageDto.getEndpoint());
        deviceMessage.setDeviceMessageStatus(DeviceMessageStatus.NEW);
        deviceMessage.setUdi(udi);
        mapDeviceRequests(deviceMessageDto, deviceMessage);
        return deviceMessage;
    }

    /**
     * Transforms a {@link DeviceMessageDto} format to its related {@link DeviceMessage} model format.
     *
     * @param deviceMessageDtos List of {@link DeviceMessageDto} device messages.
     * @param udis Map with Udis mapped per udi endpoint.
     * @return a List of {@link DeviceMessage} with the same data.
     */
    public static List<DeviceMessage> transform(List<DeviceMessageDto> deviceMessageDtos,
            Map<String, Udi> udis) {
        List<DeviceMessage> deviceMessageList = new ArrayList<>();

        deviceMessageDtos.forEach(
                deviceMessageDto -> deviceMessageList.add(transform(deviceMessageDto, udis.get(deviceMessageDto.getEndpoint()))));

        return deviceMessageList;
    }

    /**
     * Transforms a Device Message to its DTO format.
     *
     * @param deviceMessage a {@link DeviceMessage}.
     * @return a {@link DeviceMessageDto}.
     */
    public static DeviceMessageDto transformToDto(DeviceMessage deviceMessage) {
        if (deviceMessage == null) {
            return null;
        }
        DeviceMessageDto deviceMessageDto = new DeviceMessageDto();
        deviceMessageDto.setEndpoint(deviceMessage.getEndpoint());
        deviceMessage.getDeviceRequests().forEach(deviceRequest -> mapDeviceRequestsToDto(deviceMessageDto, deviceRequest));
        return deviceMessageDto;
    }

    private static void mapDeviceRequests(DeviceMessageDto deviceMessageDto, DeviceMessage deviceMessage) {
        deviceMessageDto.getShiftRequestDtos().stream()
                .forEach(o -> deviceMessage.getDeviceRequests().add(DeviceMessageTransformer.transformShiftRequest(o)));
        deviceMessageDto.getReduceRequestDtos().stream()
                .forEach(o -> deviceMessage.getDeviceRequests().add(DeviceMessageTransformer.transformReduceRequest(o)));
        deviceMessageDto.getIncreaseRequestDtos().stream()
                .forEach(o -> deviceMessage.getDeviceRequests().add(DeviceMessageTransformer.transformIncreaseRequest(o)));
        deviceMessageDto.getInterruptRequestDtos().stream()
                .forEach(o -> deviceMessage.getDeviceRequests().add(DeviceMessageTransformer.transformInterruptRequest(o)));
        deviceMessageDto.getReportRequestDtos().stream()
                .forEach(o -> deviceMessage.getDeviceRequests().add(DeviceMessageTransformer.transformReportRequest(o)));
    }

    private static void mapDeviceRequestsToDto(DeviceMessageDto deviceMessageDto, DeviceRequest deviceRequest) {
        if (deviceRequest instanceof ShiftRequest) {
            deviceMessageDto.getShiftRequestDtos().add(transformShiftRequestToDto((ShiftRequest) deviceRequest));
        }
        if (deviceRequest instanceof ReportRequest) {
            deviceMessageDto.getReportRequestDtos().add(transformReportRequestToDto((ReportRequest) deviceRequest));
        }
        if (deviceRequest instanceof InterruptRequest) {
            deviceMessageDto.getInterruptRequestDtos().add(transformInterruptRequestToDto((InterruptRequest) deviceRequest));
        }
        if (deviceRequest instanceof ReduceRequest) {
            deviceMessageDto.getReduceRequestDtos().add(transformReduceRequestToDto((ReduceRequest) deviceRequest));
        }
        if (deviceRequest instanceof IncreaseRequest) {
            deviceMessageDto.getIncreaseRequestDtos().add(transformIncreaseRequestToDto((IncreaseRequest) deviceRequest));
        }
    }

    private static ShiftRequest transformShiftRequest(ShiftRequestDto shiftRequestDto) {
        if (shiftRequestDto == null) {
            return null;
        }
        ShiftRequest shiftRequest = new ShiftRequest();
        shiftRequest.setId(shiftRequestDto.getId());
        shiftRequest.setPeriod(shiftRequestDto.getDate());
        shiftRequest.setEventId(shiftRequestDto.getEventID());
        shiftRequest.setStartDtu(shiftRequestDto.getStartDTU().intValue());
        return shiftRequest;
    }

    private static ReduceRequest transformReduceRequest(ReduceRequestDto reduceRequestDto) {
        if (reduceRequestDto == null) {
            return null;
        }
        ReduceRequest reduceRequest = new ReduceRequest();
        reduceRequest.setId(reduceRequestDto.getId());
        reduceRequest.setEventId(reduceRequestDto.getEventID());
        reduceRequest.setPeriod(reduceRequestDto.getDate());
        reduceRequest.setStartDtu(reduceRequestDto.getStartDTU().intValue());
        reduceRequest.setEndDtu(reduceRequestDto.getEndDTU().intValue());
        reduceRequest.setPower(reduceRequestDto.getPower());
        reduceRequest
                .setConsumptionProductionType(transformConsumptionProductionType(reduceRequestDto.getConsumptionProductionType()));
        return reduceRequest;
    }

    private static ConsumptionProductionType transformConsumptionProductionType(
            ConsumptionProductionTypeDto consumptionProductionType) {
        if (consumptionProductionType == ConsumptionProductionTypeDto.CONSUMPTION) {
            return ConsumptionProductionType.CONSUMPTION;
        } else if (consumptionProductionType == ConsumptionProductionTypeDto.PRODUCTION) {
            return ConsumptionProductionType.PRODUCTION;
        } else {
            return null;
        }
    }

    private static ConsumptionProductionTypeDto transformConsumptionProductionType(
            ConsumptionProductionType consumptionProductionType) {
        if (consumptionProductionType == ConsumptionProductionType.CONSUMPTION) {
            return ConsumptionProductionTypeDto.CONSUMPTION;
        } else if (consumptionProductionType == ConsumptionProductionType.PRODUCTION) {
            return ConsumptionProductionTypeDto.PRODUCTION;
        } else {
            return null;
        }
    }

    private static IncreaseRequest transformIncreaseRequest(IncreaseRequestDto increaseRequestDto) {
        if (increaseRequestDto == null) {
            return null;
        }
        IncreaseRequest increaseRequest = new IncreaseRequest();
        increaseRequest.setId(increaseRequestDto.getId());
        increaseRequest.setEventId(increaseRequestDto.getEventID());
        increaseRequest.setPeriod(increaseRequestDto.getDate());
        increaseRequest.setStartDtu(increaseRequestDto.getStartDTU().intValue());
        increaseRequest.setEndDtu(increaseRequestDto.getEndDTU().intValue());
        increaseRequest.setPower(increaseRequestDto.getPower());
        increaseRequest.setConsumptionProductionType(
                transformConsumptionProductionType(increaseRequestDto.getConsumptionProductionType()));
        return increaseRequest;
    }

    private static InterruptRequest transformInterruptRequest(InterruptRequestDto interruptRequestDto) {
        if (interruptRequestDto == null) {
            return null;
        }
        InterruptRequest interruptRequest = new InterruptRequest();
        interruptRequest.setId(interruptRequestDto.getId());
        interruptRequest.setPeriod(interruptRequestDto.getDate());
        interruptRequest.setEventId(interruptRequestDto.getEventID());
        interruptRequest.setDtuIndexes(
                Arrays.asList(interruptRequestDto.getDtus().split(DTU_INDEX_SEPERATOR))
                        .stream()
                        .map(String::trim)
                        .mapToInt(Integer::new)
                        .boxed()
                        .collect(Collectors.toList()));
        return interruptRequest;
    }

    private static ReportRequest transformReportRequest(ReportRequestDto reportRequestDto) {
        if (reportRequestDto == null) {
            return null;
        }
        ReportRequest reportRequest = new ReportRequest();
        reportRequest.setId(reportRequestDto.getId());
        reportRequest.setPeriod(reportRequestDto.getDate());
        reportRequest.setDtuIndexes(
                Arrays.asList(reportRequestDto.getDtus().split(DTU_INDEX_SEPERATOR))
                        .stream()
                        .map(String::trim)
                        .mapToInt(Integer::new)
                        .boxed()
                        .collect(Collectors.toList()));
        return reportRequest;
    }

    private static ShiftRequestDto transformShiftRequestToDto(ShiftRequest shiftRequest) {
        if (shiftRequest == null) {
            return null;
        }
        ShiftRequestDto shiftRequestDto = new ShiftRequestDto();
        shiftRequestDto.setId(shiftRequest.getId());
        shiftRequestDto.setDate(shiftRequest.getPeriod());
        shiftRequestDto.setEventID(shiftRequest.getEventId());
        shiftRequestDto.setStartDTU(BigInteger.valueOf(shiftRequest.getStartDtu()));
        return shiftRequestDto;
    }

    private static ReportRequestDto transformReportRequestToDto(ReportRequest reportRequest) {
        if (reportRequest == null) {
            return null;
        }
        ReportRequestDto reportRequestDto = new ReportRequestDto();
        reportRequestDto.setId(reportRequest.getId());
        reportRequestDto.setDate(reportRequest.getPeriod());
        reportRequestDto.setDtus(
                reportRequest.getDtuIndexes().stream().map(String::valueOf).collect(Collectors.joining(DTU_INDEX_SEPERATOR)));
        return reportRequestDto;
    }

    private static InterruptRequestDto transformInterruptRequestToDto(InterruptRequest interruptRequest) {
        if (interruptRequest == null) {
            return null;
        }
        InterruptRequestDto interruptRequestDto = new InterruptRequestDto();
        interruptRequestDto.setId(interruptRequest.getId());
        interruptRequestDto.setDate(interruptRequest.getPeriod());
        interruptRequestDto.setEventID(interruptRequest.getEventId());
        interruptRequestDto.setDtus(
                interruptRequest.getDtuIndexes().stream().map(String::valueOf).collect(Collectors.joining(DTU_INDEX_SEPERATOR)));
        return interruptRequestDto;
    }

    private static ReduceRequestDto transformReduceRequestToDto(ReduceRequest reduceRequest) {
        if (reduceRequest == null) {
            return null;
        }
        ReduceRequestDto reduceRequestDto = new ReduceRequestDto();
        reduceRequestDto.setId(reduceRequest.getId());
        reduceRequestDto.setEventID(reduceRequest.getEventId());
        reduceRequestDto.setDate(reduceRequest.getPeriod());
        reduceRequestDto.setStartDTU(BigInteger.valueOf(reduceRequest.getStartDtu()));
        reduceRequestDto.setEndDTU(BigInteger.valueOf(reduceRequest.getEndDtu()));
        reduceRequestDto.setPower(reduceRequest.getPower());
        reduceRequestDto
                .setConsumptionProductionType(transformConsumptionProductionType(reduceRequest.getConsumptionProductionType()));
        return reduceRequestDto;
    }

    private static IncreaseRequestDto transformIncreaseRequestToDto(IncreaseRequest increaseRequest) {
        if (increaseRequest == null) {
            return null;
        }
        IncreaseRequestDto increaseRequestDto = new IncreaseRequestDto();
        increaseRequestDto.setId(increaseRequest.getId());
        increaseRequestDto.setEventID(increaseRequest.getEventId());
        increaseRequestDto.setDate(increaseRequest.getPeriod());
        increaseRequestDto.setStartDTU(BigInteger.valueOf(increaseRequest.getStartDtu()));
        increaseRequestDto.setEndDTU(BigInteger.valueOf(increaseRequest.getEndDtu()));
        increaseRequestDto.setPower(increaseRequest.getPower());
        increaseRequestDto
                .setConsumptionProductionType(transformConsumptionProductionType(increaseRequest.getConsumptionProductionType()));
        return increaseRequestDto;
    }
}
