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

package energy.usef.agr.service.business;

import energy.usef.agr.dto.device.capability.DeviceCapabilityDto;
import energy.usef.agr.dto.device.capability.IncreaseCapabilityDto;
import energy.usef.agr.dto.device.capability.InterruptCapabilityDto;
import energy.usef.agr.dto.device.capability.ReduceCapabilityDto;
import energy.usef.agr.dto.device.capability.ReportCapabilityDto;
import energy.usef.agr.dto.device.capability.ShiftCapabilityDto;
import energy.usef.agr.dto.device.capability.UdiEventDto;
import energy.usef.agr.model.Udi;
import energy.usef.agr.model.UdiEvent;
import energy.usef.agr.model.device.capability.DeviceCapability;
import energy.usef.agr.model.device.capability.IncreaseCapability;
import energy.usef.agr.model.device.capability.InterruptCapability;
import energy.usef.agr.model.device.capability.ReduceCapability;
import energy.usef.agr.model.device.capability.ReportCapability;
import energy.usef.agr.model.device.capability.ShiftCapability;
import energy.usef.agr.repository.UdiRepository;
import energy.usef.agr.repository.UdiEventRepository;
import energy.usef.agr.repository.device.capability.IncreaseCapabilityRepository;
import energy.usef.agr.repository.device.capability.InterruptCapabilityRepository;
import energy.usef.agr.repository.device.capability.ReduceCapabilityRepository;
import energy.usef.agr.repository.device.capability.ReportCapabilityRepository;
import energy.usef.agr.repository.device.capability.ShiftCapabilityRepository;
import energy.usef.agr.transformer.UdiEventTransformer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Business service handling operations related to the device capabilities.
 */
@Stateless
public class AgrDeviceCapabilityBusinessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrDeviceCapabilityBusinessService.class);

    @Inject
    private UdiRepository udiRepository;
    @Inject
    private UdiEventRepository udiEventRepository;
    @Inject
    private ShiftCapabilityRepository shiftCapabilityRepository;
    @Inject
    private InterruptCapabilityRepository interruptCapabilityRepository;
    @Inject
    private IncreaseCapabilityRepository increaseCapabilityRepository;
    @Inject
    private ReduceCapabilityRepository reduceCapabilityRepository;
    @Inject
    private ReportCapabilityRepository reportCapabilityRepository;

    /**
     * Updates, creates or deletes UDI events using a given list of UDI Event DTOs.
     *
     * @param period {@link LocalDate} period for which UDI Events will be created/updated/deleted.
     * @param updatedUdiEvents {@link List} of {@link UdiEventDto}.
     */
    public void updateUdiEvents(LocalDate period, List<UdiEventDto> updatedUdiEvents) {
        LOGGER.debug("Updating udi events for period [{}]", period);
        // fetch existing udi events
        Map<String, UdiEvent> udiEventsPerId = udiEventRepository.findUdiEventsForPeriod(period)
                .stream()
                .collect(Collectors.toMap(UdiEvent::getId, Function.identity()));
        // 1. Update or create UdiEvents
        for (UdiEventDto udiEventDto : updatedUdiEvents) {
            if (!udiEventsPerId.containsKey(udiEventDto.getId())) {
                createUdiEventFromDto(udiEventDto, period);
            } else {
                updateUdiEventFromDto(udiEventsPerId.get(udiEventDto.getId()), udiEventDto);
            }
        }
        // 2. Delete udi events not present in the list anymore
        Set<String> updatedIds = updatedUdiEvents.stream().map(UdiEventDto::getId).collect(Collectors.toSet());
        udiEventsPerId.entrySet().stream()
                .filter(udiEventPerId -> !updatedIds.contains(udiEventPerId.getKey()))
                .forEach(udiEventPerId -> {
                    LOGGER.debug("Deleted udi event with id [{}]", udiEventPerId.getKey());
                    udiEventRepository.delete(udiEventPerId.getValue());
                });
    }

    private void createUdiEventFromDto(UdiEventDto udiEventDto,LocalDate period) {
        Udi udi = udiRepository.findByEndpoint(udiEventDto.getUdiEndpoint(), period);
        UdiEvent udiEvent = UdiEventTransformer.transformToModel(udiEventDto, udi);
        udiEventRepository.persist(udiEvent);
        LOGGER.debug("Created new udi event with id [{}]", udiEvent.getId());
        udiEventDto.getDeviceCapabilities().forEach(deviceCapabilityDto -> createDeviceCapability(deviceCapabilityDto, udiEvent));
    }

    private void updateUdiEventFromDto(UdiEvent originalUdiEvent, UdiEventDto udiEventDto) {
        // should not change but you never know, let's allow it for now
        originalUdiEvent.setPeriod(udiEventDto.getPeriod());
        originalUdiEvent.setUdiEventType(UdiEventTransformer.transformToModel(udiEventDto.getUdiEventType()));
        // may change
        originalUdiEvent.setStartDtu(udiEventDto.getStartDtu());
        originalUdiEvent.setEndDtu(udiEventDto.getEndDtu());
        originalUdiEvent.setStartAfterDtu(udiEventDto.getStartAfterDtu());
        originalUdiEvent.setFinishBeforeDtu(udiEventDto.getFinishBeforeDtu());
        originalUdiEvent.setDeviceSelector(udiEventDto.getDeviceSelector());
        Map<String, DeviceCapability> dbCapabilitiesPerId = originalUdiEvent.getDeviceCapabilities()
                .stream()
                .collect(Collectors.toMap(DeviceCapability::getId, Function.identity()));
        LOGGER.debug("Updated udi event with id [{}]", originalUdiEvent.getId());
        // update of create
        udiEventDto.getDeviceCapabilities().stream().forEach(deviceCapabilityDto -> {
            if (!dbCapabilitiesPerId.containsKey(deviceCapabilityDto.getId())) {
                createDeviceCapability(deviceCapabilityDto, originalUdiEvent);
            } else {
                updateDeviceCapability(dbCapabilitiesPerId.get(deviceCapabilityDto.getId()), deviceCapabilityDto);
            }
        });
        // remove capabilities that are not given back in the updated id list
        Set<String> updatedCapabilitiesId = udiEventDto.getDeviceCapabilities()
                .stream()
                .map(DeviceCapabilityDto::getId)
                .collect(Collectors.toSet());
        dbCapabilitiesPerId.values()
                .stream()
                .filter(deviceCapability -> !updatedCapabilitiesId.contains(deviceCapability.getId()))
                .forEach(deviceCapability -> {
                    LOGGER.debug("Deleted device capability with id [{}] for udi event with id [{}]", deviceCapability.getId(),
                            deviceCapability.getUdiEvent().getId());
                    originalUdiEvent.getDeviceCapabilities().remove(deviceCapability);
                });
    }

    private void createDeviceCapability(DeviceCapabilityDto deviceCapabilityDto, UdiEvent udiEvent) {
        if (deviceCapabilityDto == null) {
            return;
        }
        if (deviceCapabilityDto instanceof ShiftCapabilityDto) {
            createShiftCapability((ShiftCapabilityDto) deviceCapabilityDto, udiEvent);
        } else if (deviceCapabilityDto instanceof InterruptCapabilityDto) {
            createInterruptCapability((InterruptCapabilityDto) deviceCapabilityDto, udiEvent);
        } else if (deviceCapabilityDto instanceof IncreaseCapabilityDto) {
            createIncreaseCapability((IncreaseCapabilityDto) deviceCapabilityDto, udiEvent);
        } else if (deviceCapabilityDto instanceof ReduceCapabilityDto) {
            createReduceCapability((ReduceCapabilityDto) deviceCapabilityDto, udiEvent);
        } else if (deviceCapabilityDto instanceof ReportCapabilityDto) {
            createReportCapability((ReportCapabilityDto) deviceCapabilityDto, udiEvent);
        }
    }

    private ShiftCapability createShiftCapability(ShiftCapabilityDto shiftCapabilityDto, UdiEvent udiEvent) {
        ShiftCapability shiftCapability = new ShiftCapability();
        shiftCapability.setId(getDeviceCapabilityId(shiftCapabilityDto));
        shiftCapability.setUdiEvent(udiEvent);
        shiftCapabilityRepository.persist(shiftCapability);
        LOGGER.debug("Created new shift-capability with id [{}] for event with id [{}]", shiftCapabilityDto.getId(),
                udiEvent.getId());
        return shiftCapability;
    }

    private InterruptCapability createInterruptCapability(InterruptCapabilityDto interruptCapabilityDto, UdiEvent udiEvent) {
        InterruptCapability interruptCapability = new InterruptCapability();
        interruptCapability.setId(getDeviceCapabilityId(interruptCapabilityDto));
        interruptCapability.setMaxDtus(interruptCapabilityDto.getMaxDtus());
        interruptCapability.setType(UdiEventTransformer.transformToModel(interruptCapabilityDto.getType()));
        interruptCapability.setUdiEvent(udiEvent);
        interruptCapabilityRepository.persist(interruptCapability);
        LOGGER.debug("Created new interrupt-capability with id [{}] for event with id [{}]", interruptCapabilityDto.getId(),
                udiEvent.getId());
        return interruptCapability;
    }

    private IncreaseCapability createIncreaseCapability(IncreaseCapabilityDto increaseCapabilityDto, UdiEvent udiEvent) {
        IncreaseCapability increaseCapability = new IncreaseCapability();
        increaseCapability.setId(getDeviceCapabilityId(increaseCapabilityDto));
        increaseCapability.setMaxDtus(increaseCapabilityDto.getMaxDtus());
        increaseCapability.setPowerStep(increaseCapabilityDto.getPowerStep());
        increaseCapability.setMaxPower(increaseCapabilityDto.getMaxPower());
        increaseCapability.setDurationMultiplier(increaseCapabilityDto.getDurationMultiplier());
        increaseCapability.setConsumptionProductionType(
                UdiEventTransformer.transformToModel(increaseCapabilityDto.getConsumptionProductionType()));
        increaseCapability.setUdiEvent(udiEvent);
        increaseCapabilityRepository.persist(increaseCapability);
        LOGGER.debug("Created new increase-capability with id [{}] for event with id [{}]", increaseCapabilityDto.getId(),
                udiEvent.getId());
        return increaseCapability;
    }

    private ReduceCapability createReduceCapability(ReduceCapabilityDto reduceCapabilityDto, UdiEvent udiEvent) {
        ReduceCapability reduceCapability = new ReduceCapability();
        reduceCapability.setId(getDeviceCapabilityId(reduceCapabilityDto));
        reduceCapability.setMaxDtus(reduceCapabilityDto.getMaxDtus());
        reduceCapability.setPowerStep(reduceCapabilityDto.getPowerStep());
        reduceCapability.setMinPower(reduceCapabilityDto.getMinPower());
        reduceCapability.setDurationMultiplier(reduceCapabilityDto.getDurationMultiplier());
        reduceCapability.setConsumptionProductionType(
                UdiEventTransformer.transformToModel(reduceCapabilityDto.getConsumptionProductionType()));
        reduceCapability.setUdiEvent(udiEvent);
        reduceCapabilityRepository.persist(reduceCapability);
        LOGGER.debug("Created new reduce-capability with id [{}] for event with id [{}]", reduceCapabilityDto.getId(),
                udiEvent.getId());
        return reduceCapability;
    }

    private ReportCapability createReportCapability(ReportCapabilityDto reportCapabilityDto, UdiEvent udiEvent) {
        ReportCapability reportCapability = new ReportCapability();
        reportCapability.setId(getDeviceCapabilityId(reportCapabilityDto));
        reportCapability.setUdiEvent(udiEvent);
        reportCapabilityRepository.persist(reportCapability);
        LOGGER.debug("Created new report-capability with id [{}] for event with id [{}]", reportCapabilityDto.getId(),
                udiEvent.getId());
        return reportCapability;
    }

    private String getDeviceCapabilityId(DeviceCapabilityDto deviceCapabilityDto) {
        return deviceCapabilityDto.getId() == null ? UUID.randomUUID().toString() : deviceCapabilityDto.getId();
    }

    private void updateDeviceCapability(DeviceCapability deviceCapability, DeviceCapabilityDto deviceCapabilityDto) {
        if (deviceCapability instanceof InterruptCapability) {
            updateInterruptCapability((InterruptCapability) deviceCapability, (InterruptCapabilityDto) deviceCapabilityDto);
        } else if (deviceCapability instanceof IncreaseCapability) {
            updateIncreaseCapability((IncreaseCapability) deviceCapability, (IncreaseCapabilityDto) deviceCapabilityDto);
        } else if (deviceCapability instanceof ReduceCapability) {
            updateReduceCapability((ReduceCapability) deviceCapability, (ReduceCapabilityDto) deviceCapabilityDto);
        }
    }

    private void updateInterruptCapability(InterruptCapability interruptCapability, InterruptCapabilityDto interruptCapabilityDto) {
        interruptCapability.setMaxDtus(interruptCapabilityDto.getMaxDtus());
        interruptCapability.setType(UdiEventTransformer.transformToModel(interruptCapabilityDto.getType()));
        LOGGER.debug("Updated interrupt-capability with id [{}] ", interruptCapabilityDto.getId());
    }

    private void updateIncreaseCapability(IncreaseCapability increaseCapability, IncreaseCapabilityDto increaseCapabilityDto) {
        increaseCapability.setMaxDtus(increaseCapabilityDto.getMaxDtus());
        increaseCapability.setPowerStep(increaseCapabilityDto.getPowerStep());
        increaseCapability.setMaxPower(increaseCapabilityDto.getMaxPower());
        increaseCapability.setDurationMultiplier(increaseCapabilityDto.getDurationMultiplier());
        LOGGER.debug("Updated increase-capability with id [{}] ", increaseCapabilityDto.getId());
    }

    private void updateReduceCapability(ReduceCapability reduceCapability, ReduceCapabilityDto reduceCapabilityDto) {
        reduceCapability.setMaxDtus(reduceCapabilityDto.getMaxDtus());
        reduceCapability.setPowerStep(reduceCapabilityDto.getPowerStep());
        reduceCapability.setMinPower(reduceCapabilityDto.getMinPower());
        reduceCapability.setDurationMultiplier(reduceCapabilityDto.getDurationMultiplier());
        LOGGER.debug("Updated reduce-capability with id [{}] ", reduceCapabilityDto.getId());
    }
}