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
import info.usef.agr.dto.ElementTypeDto;
import info.usef.agr.dto.UdiPortfolioDto;
import info.usef.agr.dto.device.capability.ProfileDto;
import info.usef.agr.dto.device.capability.ShiftCapabilityDto;
import info.usef.agr.dto.device.capability.UdiEventDto;
import info.usef.agr.workflow.plan.connection.profile.CreateUdiStepParameter.IN;
import info.usef.agr.workflow.plan.connection.profile.CreateUdiStepParameter.OUT;
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import nl.energieprojecthoogdalem.agr.udis.UdiConfiguration;
import nl.energieprojecthoogdalem.forecastservice.element.ElementType;
import nl.energieprojecthoogdalem.util.EANUtil;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
public class CreateUdiTest
{
    private static int CONNECTION_COUNT = 3;

    private static String DEFINED_ENDPOINT_PREFIX = "MZEBX00"
                        , UNDEFINED_ENDPOINT_PREFIX = "NODEVICE"
                        , PV_ENDPOINT_SUFFIX = "/usef/PV"
                        , BATTERY_ENDPOINT_SUFFIX = "/usef/BATTERY"
                        , EAN_PREFIX = "ean.10000000000"
            ;

    private LocalDate PERIOD = new LocalDate();

    private CreateUdi createUdi;

    @Mock
    private UdiConfiguration udiConfiguration;

    @Before
    public void init()
    {
        createUdi = new CreateUdi();

        Mockito.when(udiConfiguration.getCapabilities(Matchers.anyString())).thenReturn(buildProfile(), buildProfile(), buildProfile());
        Mockito.doReturn(buildEndpoints()).when(udiConfiguration).getEndpoints();

        Whitebox.setInternalState(createUdi, "udiConfiguration", udiConfiguration);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInvoke() throws Exception
    {
        Map<String, String> endpoints = buildEndpoints();
        WorkflowContext result = createUdi.invoke(buildInputContext());
        validateEvents(result);
        validateConnections(endpoints, result.get(OUT.CONNECTION_PORTFOLIO_DTO_LIST.name(), List.class));

        result.setValue(IN.PERIOD.name(), PERIOD.plusDays(1));
        result.remove(OUT.UDI_EVENTS_PER_UDI_MAP.name());

        result = createUdi.invoke(result);
        validateEvents(result);
        validateConnections(endpoints, result.get(OUT.CONNECTION_PORTFOLIO_DTO_LIST.name(), List.class));
    }

    private ProfileDto buildProfile()
    {
        ProfileDto profileDto = new ProfileDto();
        profileDto.getCapabilities().add(new ShiftCapabilityDto());
        return profileDto;
    }

    private WorkflowContext buildInputContext()
    {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(IN.PERIOD.name(), PERIOD);
        context.setValue(IN.PTU_DURATION.name(), 15);
        context.setValue(IN.CONNECTION_PORTFOLIO_DTO_LIST.name(),  buildConnectionPortfolioDtos());
        context.setValue(IN.ELEMENT_PER_CONNECTION_MAP.name(), buildElementMap());
        return context;
    }

    private Map<String, String> buildEndpoints()
    {
        return IntStream.rangeClosed(1, CONNECTION_COUNT -1)
                        .boxed()
                        .collect(Collectors.toMap(Object::toString, idx -> DEFINED_ENDPOINT_PREFIX+idx));
    }

    private Map<String, List<ElementDto>> buildElementMap()
    {
        return IntStream.rangeClosed(1, CONNECTION_COUNT)
                        .boxed()
                        .collect(Collectors.toMap(idx -> EAN_PREFIX + idx, this::buildElements));
    }

    private List<ElementDto> buildElements(int idx)
    {
        List<ElementDto> list = new ArrayList<>();

        list.add(createElement(DEFINED_ENDPOINT_PREFIX + idx, ElementType.HOME, ElementTypeDto.SYNTHETIC_DATA));

        if(idx > CONNECTION_COUNT /2)
            list.add(createElement(DEFINED_ENDPOINT_PREFIX + idx, ElementType.BATTERY_ZIH, ElementTypeDto.MANAGED_DEVICE));

        else
            list.add(createElement(DEFINED_ENDPOINT_PREFIX + idx, ElementType.BATTERY_NOD, ElementTypeDto.MANAGED_DEVICE));

        return list;
    }

    private ElementDto createElement(String ean, String profile, ElementTypeDto elementTypeDto)
    {
        ElementDto dto = new ElementDto();
        dto.setConnectionEntityAddress(ean);
        dto.setDtuDuration(15);
        dto.setProfile(profile);
        dto.setElementType(elementTypeDto);
        dto.setId(profile +1);
        return dto;
    }

    private List<ConnectionPortfolioDto> buildConnectionPortfolioDtos()
    {
        return IntStream.rangeClosed(1, CONNECTION_COUNT)
                        .mapToObj(index -> new ConnectionPortfolioDto(EAN_PREFIX + index))
                        .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private void validateEvents(WorkflowContext context)
    {
        Map<String, List<UdiEventDto>> udiEvents = context.get(OUT.UDI_EVENTS_PER_UDI_MAP.name(), Map.class);
        List<UdiEventDto> events = udiEvents.values().stream().flatMap(Collection::stream).collect(Collectors.toList());

        assertNotNull(events);
        assertEquals(3, events.size());
        events.forEach(event -> assertEquals(1, event.getDeviceCapabilities().size()));
    }

    private void validateConnections(Map<String, String> endpoints, List<ConnectionPortfolioDto> connections)
    {
        assertNotNull(connections);
        connections.forEach(connection ->
        {
            int idx = EANUtil.toHomeInt(connection.getConnectionEntityAddress());
            String homeId = ""+idx;
            List<UdiPortfolioDto> udis = connection.getUdis();

            if( idx > CONNECTION_COUNT /2)
                assertEquals(2, udis.size() );

            else
                assertEquals(1, udis.size() );

            udis.forEach(udi ->
            {
                String expectedEndpoint = endpoints.containsKey( homeId ) ? endpoints.get( homeId ) : UNDEFINED_ENDPOINT_PREFIX + homeId;

                switch (udi.getProfile())
                {
                    case ElementType.PV:
                        expectedEndpoint += PV_ENDPOINT_SUFFIX;
                        assertEquals(expectedEndpoint, udi.getEndpoint());
                        break;

                    case ElementType.BATTERY_NOD:
                    case ElementType.BATTERY_ZIH:
                        expectedEndpoint += BATTERY_ENDPOINT_SUFFIX;
                        assertEquals(expectedEndpoint, udi.getEndpoint());
                        break;
                }
            });

        });
    }

}
