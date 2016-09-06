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
import energy.usef.agr.dto.ElementDto;
import energy.usef.agr.dto.ElementTypeDto;
import energy.usef.agr.dto.UdiPortfolioDto;
import energy.usef.agr.dto.device.capability.ProfileDto;
import energy.usef.agr.dto.device.capability.UdiEventDto;
import energy.usef.agr.dto.device.capability.UdiEventTypeDto;
import energy.usef.agr.repository.CapabilityProfileRepository;
import energy.usef.agr.workflow.plan.connection.profile.CreateUdiStepParameter;
import energy.usef.agr.workflow.plan.connection.profile.CreateUdiStepParameter.IN;
import energy.usef.agr.workflow.plan.connection.profile.CreateUdiStepParameter.OUT;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This PBC is in charge of filling in the Profile values for each connection of the portfolio.
 * To find the paramaters for this PBC see {@link CreateUdiStepParameter}.
 */
public class AgrCreateUdiStub implements WorkflowStep {

    public static final String UDI_PROTOCOL = "udi://";
    public static final String URL_SEPERATOR = "/";
    public static final int START_DTU = 1;
    public static final UdiEventTypeDto DEFAULT_EVENT_TYPE = UdiEventTypeDto.PRODUCTION;

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrCreateUdiStub.class);

    @Inject
    private CapabilityProfileRepository capabilityProfileRepository;

    /**
     * Executes this PBC.
     *
     * @param context Workflow context. This context provides input data for a workflow step.
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public WorkflowContext invoke(WorkflowContext context) {
        LocalDate period = context.get(IN.PERIOD.name(), LocalDate.class);
        Integer ptuDuration = context.get(IN.PTU_DURATION.name(), Integer.class);
        List<ConnectionPortfolioDto> connectionPortfolioDTOs = (List<ConnectionPortfolioDto>) context
                .get(IN.CONNECTION_PORTFOLIO_DTO_LIST.name(), List.class);
        Map<String, List<ElementDto>> elementsPerConnection = (Map<String, List<ElementDto>>) context
                .get(IN.ELEMENT_PER_CONNECTION_MAP.name(), Map.class);


        Map<String, List<UdiEventDto>> udiEvents = new HashMap<>();
        connectionPortfolioDTOs.stream()
                .filter(connectionPortfolioDTO ->
                        elementsPerConnection.containsKey(connectionPortfolioDTO.getConnectionEntityAddress()))
                .forEach(connectionPortfolioDTO -> {
                    List<ElementDto> elements = elementsPerConnection.get(connectionPortfolioDTO.getConnectionEntityAddress());
                    udiEvents.putAll(createUdiAndItsEvents(elements, connectionPortfolioDTO, period));
                });
        return buildResultContext(connectionPortfolioDTOs, udiEvents);
    }

    private Map<String, List<UdiEventDto>> createUdiAndItsEvents(List<ElementDto> elements,
            ConnectionPortfolioDto connectionPortfolioDTO, LocalDate period) {
        Map<String, List<UdiEventDto>> udiEventsPerUdi = new HashMap<>();
        for (ElementDto element : elements) {
            if (!ElementTypeDto.MANAGED_DEVICE.equals(element.getElementType())) {
                continue;
            }

            Map<String, List<UdiPortfolioDto>> collect = connectionPortfolioDTO.getUdis().stream()
                    .collect(Collectors.groupingBy(UdiPortfolioDto::getEndpoint));

            String endpoint = UDI_PROTOCOL + connectionPortfolioDTO.getConnectionEntityAddress() + URL_SEPERATOR + element.getId();
            if (!collect.containsKey(endpoint)) {
                UdiPortfolioDto udi = new UdiPortfolioDto(endpoint, element.getDtuDuration(), element.getProfile());
                connectionPortfolioDTO.getUdis().add(udi);

                List<UdiEventDto> events = createUdiEvents(udi, element, period);
                udiEventsPerUdi.put(endpoint, events);
            }
        }
        return udiEventsPerUdi;
    }

    private List<UdiEventDto> createUdiEvents(UdiPortfolioDto udi, ElementDto element, LocalDate period) {
        Map<String, ProfileDto> profiles = capabilityProfileRepository.readFromConfigFile();

        if (!profiles.containsKey(element.getProfile())) {
            LOGGER.warn("Profile {} is not known among our capability profiles!", element.getProfile());
            return new ArrayList<>();
        }
        ProfileDto profile = profiles.get(element.getProfile());
        UdiEventDto eventDto = createFulLDayEvent(udi, period);

        //make the capability Id's unique
        profile.getCapabilities().forEach(capability -> capability.setId(eventDto.getId()+"-" +capability.getId()));

        eventDto.getDeviceCapabilities().addAll(profile.getCapabilities());

        List<UdiEventDto> eventDtos = new ArrayList<>();
        eventDtos.add(eventDto);
        return eventDtos;
    }

    private UdiEventDto createFulLDayEvent(UdiPortfolioDto udi, LocalDate period) {
        UdiEventDto eventDto = new UdiEventDto();
        eventDto.setDeviceSelector(udi.getEndpoint());
        eventDto.setStartDtu(START_DTU);
        eventDto.setEndDtu(PtuUtil.getNumberOfPtusPerDay(period, udi.getDtuSize()));
        eventDto.setUdiEventType(DEFAULT_EVENT_TYPE);
        eventDto.setId(UUID.randomUUID().toString());
        eventDto.setPeriod(period);
        eventDto.setUdiEndpoint(udi.getEndpoint());
        return eventDto;
    }

    private WorkflowContext buildResultContext(List<ConnectionPortfolioDto> connectionPortfolioDTOs,
            Map<String, List<UdiEventDto>> udiEvents) {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(OUT.CONNECTION_PORTFOLIO_DTO_LIST.name(), connectionPortfolioDTOs);
        context.setValue(OUT.UDI_EVENTS_PER_UDI_MAP.name(), udiEvents);
        return context;
    }
}
