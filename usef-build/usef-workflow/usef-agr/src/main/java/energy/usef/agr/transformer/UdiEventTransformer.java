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

import energy.usef.agr.dto.device.capability.DeviceCapabilityDto;
import energy.usef.agr.dto.device.capability.IncreaseCapabilityDto;
import energy.usef.agr.dto.device.capability.InterruptCapabilityDto;
import energy.usef.agr.dto.device.capability.InterruptCapabilityTypeDto;
import energy.usef.agr.dto.device.capability.ReduceCapabilityDto;
import energy.usef.agr.dto.device.capability.ReportCapabilityDto;
import energy.usef.agr.dto.device.capability.ShiftCapabilityDto;
import energy.usef.agr.dto.device.capability.UdiEventDto;
import energy.usef.agr.dto.device.capability.UdiEventTypeDto;
import energy.usef.agr.dto.device.request.ConsumptionProductionTypeDto;
import energy.usef.agr.model.ConsumptionProductionType;
import energy.usef.agr.model.Udi;
import energy.usef.agr.model.UdiEvent;
import energy.usef.agr.model.UdiEventType;
import energy.usef.agr.model.device.capability.DeviceCapability;
import energy.usef.agr.model.device.capability.IncreaseCapability;
import energy.usef.agr.model.device.capability.InterruptCapability;
import energy.usef.agr.model.device.capability.InterruptCapabilityType;
import energy.usef.agr.model.device.capability.ReduceCapability;
import energy.usef.agr.model.device.capability.ReportCapability;
import energy.usef.agr.model.device.capability.ShiftCapability;

import java.util.UUID;

/**
 * Transformer class for the Udi Events related objects.
 */
public class UdiEventTransformer {

    private UdiEventTransformer() {

    }

    /**
     * Transforms a UdiEvent to its related DTO format.
     *
     * @param udiEvent {@link UdiEvent} entity.
     * @return a {@link UdiEventDto} object.
     */
    public static UdiEventDto transformToDto(UdiEvent udiEvent) {
        UdiEventDto udiEventDto = new UdiEventDto();
        udiEventDto.setId(udiEvent.getId());
        udiEventDto.setPeriod(udiEvent.getPeriod());
        udiEventDto.setStartDtu(udiEvent.getStartDtu());
        udiEventDto.setEndDtu(udiEvent.getEndDtu());
        udiEventDto.setStartAfterDtu(udiEvent.getStartAfterDtu());
        udiEventDto.setFinishBeforeDtu(udiEvent.getFinishBeforeDtu());
        udiEventDto.setDeviceSelector(udiEvent.getDeviceSelector());
        udiEventDto.setUdiEndpoint(udiEvent.getUdi().getEndpoint());
        udiEventDto.setUdiEventType(transformToDto(udiEvent.getUdiEventType()));
        udiEvent.getDeviceCapabilities().stream().map(UdiEventTransformer::transformToDto)
                .forEach(deviceCapabilityDto -> udiEventDto.getDeviceCapabilities().add(deviceCapabilityDto));
        return udiEventDto;
    }

    /**
     * Transforms a UdiEventType to its related DTO format.
     *
     * @param udiEventType {@link UdiEventType} entity.
     * @return a {@link UdiEventTypeDto} object.
     */
    public static UdiEventTypeDto transformToDto(UdiEventType udiEventType) {
        if (udiEventType == null) {
            return null;
        }
        switch (udiEventType) {
        case CONSUMPTION:
            return UdiEventTypeDto.CONSUMPTION;
        case PRODUCTION:
            return UdiEventTypeDto.PRODUCTION;
        case ON_DEMAND_CONSUMPTION:
            return UdiEventTypeDto.ON_DEMAND_CONSUMPTION;
        case ON_DEMAND_PRODUCTION:
            return UdiEventTypeDto.ON_DEMAND_PRODUCTION;
        default:
            return null;
        }
    }

    /**
     * Transforms a UdiEventDto to its related model format (WITHOUT its capabilities).
     *
     * @param udiEventDto {@link UdiEventDto}.
     * @param udi @{@link Udi} the UDI related to the UDI event, already present in the database.
     * @return a {@link UdiEvent} unmanaged entity.
     */
    public static UdiEvent transformToModel(UdiEventDto udiEventDto, Udi udi) {
        UdiEvent udiEvent = new UdiEvent();
        udiEvent.setId(udiEventDto.getId() == null ? UUID.randomUUID().toString() : udiEventDto.getId());
        udiEvent.setDeviceSelector(udiEventDto.getDeviceSelector());
        udiEvent.setStartDtu(udiEventDto.getStartDtu());
        udiEvent.setEndDtu(udiEventDto.getEndDtu());
        udiEvent.setStartAfterDtu(udiEventDto.getStartAfterDtu());
        udiEvent.setFinishBeforeDtu(udiEventDto.getFinishBeforeDtu());
        udiEvent.setPeriod(udiEventDto.getPeriod());
        udiEvent.setUdiEventType(UdiEventTransformer.transformToModel(udiEventDto.getUdiEventType()));
        udiEvent.setUdi(udi);
        return udiEvent;
    }

    /**
     * Transforms a UdiEventTypeDto to its related model format.
     *
     * @param udiEventTypeDto {@link UdiEventTypeDto}.
     * @return a {@link UdiEventType} enumerated object.
     */
    public static UdiEventType transformToModel(UdiEventTypeDto udiEventTypeDto) {
        if (udiEventTypeDto == null) {
            return null;
        }
        switch (udiEventTypeDto) {
        case CONSUMPTION:
            return UdiEventType.CONSUMPTION;
        case PRODUCTION:
            return UdiEventType.PRODUCTION;
        case ON_DEMAND_CONSUMPTION:
            return UdiEventType.ON_DEMAND_CONSUMPTION;
        case ON_DEMAND_PRODUCTION:
            return UdiEventType.ON_DEMAND_PRODUCTION;
        default:
            return null;
        }
    }

    /**
     * Transforms a DeviceCapability to its related DTO format.
     *
     * @param deviceCapability {@link DeviceCapability} entity.
     * @return a {@link DeviceCapabilityDto} object.
     */
    public static DeviceCapabilityDto transformToDto(DeviceCapability deviceCapability) {
        if (deviceCapability == null) {
            return null;
        }
        if (deviceCapability instanceof ReduceCapability) {
            return transformCapabilityToDto((ReduceCapability) deviceCapability);
        } else if (deviceCapability instanceof IncreaseCapability) {
            return transformCapabilityToDto((IncreaseCapability) deviceCapability);
        } else if (deviceCapability instanceof InterruptCapability) {
            return transformCapabilityToDto((InterruptCapability) deviceCapability);
        } else if (deviceCapability instanceof ShiftCapability) {
            return transformCapabilityToDto((ShiftCapability) deviceCapability);
        } else if (deviceCapability instanceof ReportCapability) {
            return transformCapabilityToDto((ReportCapability) deviceCapability);
        }
        return null;
    }

    /**
     * Transforms a ReduceCapability to its related DTO format.
     *
     * @param reduceCapability {@link ReduceCapability} entity.
     * @return a {@link ReduceCapabilityDto} object.
     */
    public static ReduceCapabilityDto transformCapabilityToDto(ReduceCapability reduceCapability) {
        ReduceCapabilityDto reduceCapabilityDto = new ReduceCapabilityDto();
        reduceCapabilityDto.setId(reduceCapability.getId());
        reduceCapabilityDto.setMinPower(reduceCapability.getMinPower());
        reduceCapabilityDto.setPowerStep(reduceCapability.getPowerStep());
        reduceCapabilityDto.setMaxDtus(reduceCapability.getMaxDtus());
        reduceCapabilityDto.setDurationMultiplier(reduceCapability.getDurationMultiplier());
        reduceCapabilityDto.setConsumptionProductionType(transformToDto(reduceCapability.getConsumptionProductionType()));
        return reduceCapabilityDto;
    }

    /**
     * Transforms a IncreaseCapability to its related DTO format.
     *
     * @param increaseCapability {@link IncreaseCapability} entity.
     * @return a {@link IncreaseCapabilityDto} object.
     */
    public static IncreaseCapabilityDto transformCapabilityToDto(IncreaseCapability increaseCapability) {
        IncreaseCapabilityDto increaseCapabilityDto = new IncreaseCapabilityDto();
        increaseCapabilityDto.setId(increaseCapability.getId());
        increaseCapabilityDto.setMaxPower(increaseCapability.getMaxPower());
        increaseCapabilityDto.setPowerStep(increaseCapability.getPowerStep());
        increaseCapabilityDto.setMaxDtus(increaseCapability.getMaxDtus());
        increaseCapabilityDto.setDurationMultiplier(increaseCapability.getDurationMultiplier());
        increaseCapabilityDto.setConsumptionProductionType(transformToDto(increaseCapability.getConsumptionProductionType()));
        return increaseCapabilityDto;
    }

    /**
     * Transforms a InterruptCapability to its related DTO format.
     *
     * @param interruptCapability {@link InterruptCapability} entity.
     * @return a {@link InterruptCapabilityDto} object.
     */
    public static InterruptCapabilityDto transformCapabilityToDto(InterruptCapability interruptCapability) {
        InterruptCapabilityDto interruptCapabilityDto = new InterruptCapabilityDto();
        interruptCapabilityDto.setId(interruptCapability.getId());
        interruptCapabilityDto.setMaxDtus(interruptCapability.getMaxDtus());
        interruptCapabilityDto.setType(transformToDto(interruptCapability.getType()));
        return interruptCapabilityDto;
    }

    /**
     * Transforms a ConsumptionProductionType to its related DTO format.
     *
     * @param type {@link ConsumptionProductionType}.
     * @return a {@link ConsumptionProductionTypeDto}.
     */
    public static ConsumptionProductionTypeDto transformToDto(ConsumptionProductionType type) {
        if (type == null) {
            return null;
        }
        switch (type) {
        case CONSUMPTION:
            return ConsumptionProductionTypeDto.CONSUMPTION;
        case PRODUCTION:
            return ConsumptionProductionTypeDto.PRODUCTION;
        default:
            return null;
        }
    }

    /**
     * Transforms a InterruptCapabilityType to its related DTO format.
     *
     * @param type {@link InterruptCapabilityType}.
     * @return a {@link InterruptCapabilityTypeDto}.
     */
    public static InterruptCapabilityTypeDto transformToDto(InterruptCapabilityType type) {
        if (type == null) {
            return null;
        }
        switch (type) {
        case FULL:
            return InterruptCapabilityTypeDto.FULL;
        case NONE:
            return InterruptCapabilityTypeDto.NONE;
        case PER_DTU:
            return InterruptCapabilityTypeDto.PER_DTU;
        default:
            return null;
        }
    }

    /**
     * Transforms a ConsumptionProductionTypeDto to its related model format.
     *
     * @param type {@link ConsumptionProductionTypeDto}.
     * @return a {@link ConsumptionProductionType}.
     */
    public static ConsumptionProductionType transformToModel(ConsumptionProductionTypeDto type) {
        if (type == null) {
            return null;
        }
        switch (type) {
        case CONSUMPTION:
            return ConsumptionProductionType.CONSUMPTION;
        case PRODUCTION:
            return ConsumptionProductionType.PRODUCTION;
        default:
            return null;
        }
    }

    /**
     * Transforms a InterruptCapabilityTypeDto to its related model format.
     *
     * @param type {@link InterruptCapabilityTypeDto}.
     * @return a {@link InterruptCapabilityType}.
     */
    public static InterruptCapabilityType transformToModel(InterruptCapabilityTypeDto type) {
        if (type == null) {
            return null;
        }
        switch (type) {
        case FULL:
            return InterruptCapabilityType.FULL;
        case NONE:
            return InterruptCapabilityType.NONE;
        case PER_DTU:
            return InterruptCapabilityType.PER_DTU;
        default:
            return null;
        }
    }

    /**
     * Transforms a ShiftCapability to its related DTO format.
     *
     * @param shiftCapability {@link ShiftCapability} entity.
     * @return a {@link ShiftCapabilityDto} object.
     */
    public static ShiftCapabilityDto transformCapabilityToDto(ShiftCapability shiftCapability) {
        ShiftCapabilityDto shiftCapabilityDto = new ShiftCapabilityDto();
        shiftCapabilityDto.setId(shiftCapability.getId());
        return shiftCapabilityDto;
    }

    /**
     * Transforms a ReportCapability to its related DTO format.
     *
     * @param reportCapability {@link ReportCapability} entity.
     * @return a {@link ReportCapabilityDto} object.
     */
    public static ReportCapabilityDto transformCapabilityToDto(ReportCapability reportCapability) {
        ReportCapabilityDto reportCapabilityDto = new ReportCapabilityDto();
        reportCapabilityDto.setId(reportCapability.getId());
        return reportCapabilityDto;
    }

}
