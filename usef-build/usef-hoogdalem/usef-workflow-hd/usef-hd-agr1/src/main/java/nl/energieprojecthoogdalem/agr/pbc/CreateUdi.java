/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.agr.pbc;

import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.dto.ElementDto;
import info.usef.agr.dto.UdiPortfolioDto;
import info.usef.agr.dto.device.capability.ProfileDto;
import info.usef.agr.dto.device.capability.UdiEventDto;
import info.usef.agr.dto.device.capability.UdiEventTypeDto;
import info.usef.agr.workflow.plan.connection.profile.CreateUdiStepParameter;
import info.usef.agr.workflow.plan.connection.profile.CreateUdiStepParameter.IN;
import info.usef.agr.workflow.plan.connection.profile.CreateUdiStepParameter.OUT;
import info.usef.core.util.PtuUtil;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import nl.energieprojecthoogdalem.agr.udis.Counter;
import nl.energieprojecthoogdalem.agr.udis.UdiConfiguration;
import nl.energieprojecthoogdalem.forecastservice.element.ElementType;
import nl.energieprojecthoogdalem.util.EANUtil;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * This PBC is in charge of filling in the Profile values for each connection of the portfolio.
 * To find the parameters for this PBC see {@link CreateUdiStepParameter}.
 */
public class CreateUdi implements WorkflowStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateUdi.class);

    private static final int START_DTU = 1;

    private static final String UNDEFINED_ENDPOINT = "NODEVICE"
                                , USEF_PATH = "/usef/"
                                , PV_ENDPOINT_SUFFIX = USEF_PATH + ElementType.PV
                                , BATTERY_ENDPOINT_SUFFIX = USEF_PATH + ElementType.BATTERY
                        ;

    private static final UdiEventTypeDto DEFAULT_EVENT_TYPE = UdiEventTypeDto.CONSUMPTION;

    @Inject
    private UdiConfiguration udiConfiguration;

    /**
     * reads the capabilities from a json file using the {@link UdiConfiguration} getCapabilities(),
     * reads the endpoint devices from a json file using the {@link UdiConfiguration} getEndpoints(),
     * creates a PV udi for the BATTERY_ZIH element profile, creates a
     *
     * @param context Workflow context. This context provides the connection portfolio list filled with elements per connection (and optional filled with udis).
     * @return a connection portfolio list filled with UDIs and a list with UDI events per
     */
    @Override
    @SuppressWarnings("unchecked")
    public WorkflowContext invoke(WorkflowContext context)
    {
        //usef input
        LocalDate period = context.get(IN.PERIOD.name(), LocalDate.class);

        List<ConnectionPortfolioDto> connectionPortfolioDTOs = context.get(IN.CONNECTION_PORTFOLIO_DTO_LIST.name(), List.class);
        Map<String, List<ElementDto>> elementsPerConnection = context.get(IN.ELEMENT_PER_CONNECTION_MAP.name(), Map.class);

        //local input
        Map<String, String> endpoints = udiConfiguration.getEndpoints();

        Counter idCounter = new Counter();

        //output
        Map<String, List<UdiEventDto>> udiEvents = new HashMap<>();

        connectionPortfolioDTOs.stream()
                .filter(connectionPortfolioDTO ->
                        elementsPerConnection.containsKey(connectionPortfolioDTO.getConnectionEntityAddress()))
                .forEach(connectionPortfolioDTO ->
                {
                    List<ElementDto> elements = elementsPerConnection.get(connectionPortfolioDTO.getConnectionEntityAddress());
                    udiEvents.putAll(createUdiAndItsEvents(period, connectionPortfolioDTO, elements, endpoints, idCounter));
                });

        context.setValue(OUT.CONNECTION_PORTFOLIO_DTO_LIST.name(), connectionPortfolioDTOs);
        context.setValue(OUT.UDI_EVENTS_PER_UDI_MAP.name(), udiEvents);

        return context;
    }

    /**
     * creates a list of udi events, and udis for a list of elements
     * fills the portfolios with udis
     * @param period the date to create udi events for
     * @param connection a single portfolio to create udis for
     * @param elements a list of elements to create udis
     * @param endpoints a map of endpoint devices (cg serials) linked to home id keys
     * @param idCounter a {@link Counter} to make ids between udi events unique
     * @return a map with a list of {@link UdiEventDto} containing the udi events for each endpoint
     * */
    private Map<String, List<UdiEventDto>> createUdiAndItsEvents(LocalDate period, ConnectionPortfolioDto connection, List<ElementDto> elements
            ,Map<String, String> endpoints, Counter idCounter
    )
    {
        Map<String, List<UdiEventDto>> udiEventsPerUdi = new HashMap<>();

        List<UdiPortfolioDto> udis = connection.getUdis();

        //convert EAN -> H ID (no formatting) -> SERIAL
        String homeId = ""+EANUtil.toHomeInt(connection.getConnectionEntityAddress());
        String endpointDevice = endpoints.containsKey(homeId) ? endpoints.get(homeId) : UNDEFINED_ENDPOINT + homeId;

        for(ElementDto element : elements)
        {
            String elementProfile = element.getProfile();
            int ptuDuration = element.getDtuDuration();

            switch( elementProfile )
            {
                case ElementType.BATTERY_ZIH:
                    createZIHUdis(udis, udiEventsPerUdi, period, elementProfile, endpointDevice, ptuDuration, idCounter);
                break;

                case ElementType.BATTERY_NOD:
                    createNODUdis(udis, udiEventsPerUdi, period, elementProfile, endpointDevice, ptuDuration, idCounter);
                break;
            }
        }

        return udiEventsPerUdi;
    }

    /**
     * creates a list of udi events using the capabilities json for a single element
     * @param udi the udi to create events for
     * @param elementProfile the element profile to read capabilities from
     * @param period the date to create udi events for
     * @param idCounter a {@link Counter} to make ids between udi events unique
     * @return a List of {@link UdiEventDto} containing the udi events dto
     * */
    private UdiEventDto createUdiEvent(LocalDate period, UdiPortfolioDto udi, String elementProfile, Counter idCounter)
    {
        ProfileDto profile = udiConfiguration.getCapabilities(elementProfile);

        if( profile == null)
            return null;

        UdiEventDto eventDto = createFullDayEvent(udi, period, idCounter);

        profile.getCapabilities().forEach(capability ->
        {
            capability.setId(eventDto.getId());
            eventDto.getDeviceCapabilities().add(capability);
        });

        return eventDto;
    }

    /**
     * creates an udi event and sets the date and time for the given udi
     * @param udi the udi to create a event for
     * @param period the date to create a udi event for
     * @param idCounter a {@link Counter} to make ids between udi events unique
     * @return {@link UdiEventDto} a udi event dto with date and time set
     * */
    private UdiEventDto createFullDayEvent(UdiPortfolioDto udi, LocalDate period, Counter idCounter)
    {
        UdiEventDto eventDto = new UdiEventDto();
        eventDto.setDeviceSelector(udi.getEndpoint());
        eventDto.setStartDtu(START_DTU);
        eventDto.setEndDtu(PtuUtil.getNumberOfPtusPerDay(period, udi.getDtuSize()));
        eventDto.setUdiEventType(DEFAULT_EVENT_TYPE);
        eventDto.setId(period.toString("yyyy-MM-dd") + '-' + idCounter.value());
        eventDto.setPeriod(period);
        eventDto.setUdiEndpoint(udi.getEndpoint());

        idCounter.increment();

        return eventDto;
    }

    /**
     * creates a PV and a BATTERY udi for a ZIH {@link ConnectionPortfolioDto}
     * @param udis a reference to the udis list of a {@link ConnectionPortfolioDto}
     * @param udiEventsPerUdi a map containing a list of {@link UdiEventDto} per udi
     * @param period the forecast {@link LocalDate}
     * @param elementProfile the element profile name
     * @param endpointDevice a cloudgate serial
     * @param ptuDuration the duration of one PTU
     * @param idCounter a {@link Counter} to make ids between udi events unique
     * */
    private void createZIHUdis( List<UdiPortfolioDto> udis, Map<String, List<UdiEventDto>> udiEventsPerUdi
                                , LocalDate period, String elementProfile, String endpointDevice, int ptuDuration, Counter idCounter)
    {
        String batteryEndpoint = endpointDevice + BATTERY_ENDPOINT_SUFFIX;
        UdiPortfolioDto batteryUdi = findUdiForEndpoint(udis, batteryEndpoint);
        List<UdiEventDto> udiEvents = new ArrayList<>();

        if( batteryUdi == null || udis.isEmpty() )
        {
            LOGGER.warn("Found new endpoint device {} clearing previous udis.", endpointDevice);
            udis.clear();
            udis.add(new UdiPortfolioDto(endpointDevice+ PV_ENDPOINT_SUFFIX, ptuDuration, ElementType.PV));
            batteryUdi = new UdiPortfolioDto(batteryEndpoint, ptuDuration, elementProfile);
            udis.add(batteryUdi);
        }

        udiEvents.add(createUdiEvent(period, batteryUdi, elementProfile, idCounter));
        udiEventsPerUdi.put(batteryEndpoint, udiEvents);
    }

    /**
     * creates a BATTERY udi for a NOD {@link ConnectionPortfolioDto}
     * @param udis a reference to the udis list of a {@link ConnectionPortfolioDto}
     * @param udiEventsPerUdi a map containing a list of {@link UdiEventDto} per udi
     * @param period the forecast {@link LocalDate}
     * @param elementProfile the element profile name
     * @param endpointDevice a cloudgate serial
     * @param ptuDuration the duration of one PTU
     * @param idCounter a {@link Counter} to make ids between udi events unique
     * */
    private void createNODUdis(List<UdiPortfolioDto> udis, Map<String, List<UdiEventDto>> udiEventsPerUdi
                                , LocalDate period, String elementProfile, String endpointDevice, int ptuDuration, Counter idCounter)
    {
        String batteryEndpoint = endpointDevice + BATTERY_ENDPOINT_SUFFIX;
        UdiPortfolioDto batteryUdi = findUdiForEndpoint(udis, batteryEndpoint);
        List<UdiEventDto> udiEvents = new ArrayList<>();

        if( batteryUdi == null || udis.isEmpty() )
        {
            LOGGER.warn("Found new endpoint {} clearing previous udis.", endpointDevice);
            udis.clear();
            batteryUdi = new UdiPortfolioDto(batteryEndpoint, ptuDuration, elementProfile);
            udis.add(batteryUdi);
        }

        udiEvents.add(createUdiEvent(period, batteryUdi, elementProfile, idCounter));
        udiEventsPerUdi.put(batteryEndpoint, udiEvents);
    }

    /**
     * searches in a list of {@link UdiPortfolioDto} if it contains the endpoint
     * @param endpoint the endpoint to check
     * @param udis the list of udis to search through
     * @return true if endpoint equals the {@link UdiPortfolioDto} getEndpoint()
     * */
    private UdiPortfolioDto findUdiForEndpoint(List<UdiPortfolioDto> udis, String endpoint)
    {
        for(UdiPortfolioDto udi : udis)
        {
            if(endpoint.equals(udi.getEndpoint()))
                return udi;
        }
        return null;
    }
}
