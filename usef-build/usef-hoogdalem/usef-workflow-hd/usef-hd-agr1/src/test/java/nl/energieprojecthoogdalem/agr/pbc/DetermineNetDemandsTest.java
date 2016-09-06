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
import info.usef.agr.dto.PowerContainerDto;
import info.usef.agr.dto.PowerDataDto;
import info.usef.agr.dto.UdiPortfolioDto;
import info.usef.agr.dto.device.capability.ShiftCapabilityDto;
import info.usef.agr.dto.device.capability.UdiEventDto;
import info.usef.agr.dto.device.capability.UdiEventTypeDto;
import info.usef.agr.workflow.operate.netdemand.DetermineNetDemandStepParameter.OUT;
import info.usef.agr.workflow.operate.netdemand.DetermineNetDemandStepParameter.IN;
import info.usef.core.util.PtuUtil;
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;

import java.math.BigInteger;
import static java.math.BigInteger.ZERO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import nl.energieprojecthoogdalem.forecastservice.element.ElementType;
import nl.energieprojecthoogdalem.messageservice.transportservice.MqttConnection;
import nl.energieprojecthoogdalem.messageservice.transportservice.mqttmessages.MqttResponseMessage;
import nl.energieprojecthoogdalem.util.EANUtil;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.*;

/**
 * Test class in charge of the unit tests related to the {@link DetermineNetDemands}.
 */
@RunWith(PowerMockRunner.class)
public class DetermineNetDemandsTest {

    private static final int CONNECTIONS_COUNT = 4
                            ,PTU_DURATION = 15
                            ,PTU_COUNT = 96
                            ,PTU_NUMBER = PtuUtil.getPtuIndex(new LocalDateTime(), PTU_DURATION)
                            ;

    private static final BigInteger UCL_VALUE = BigInteger.valueOf(300)
            , PV_VALUE = BigInteger.valueOf(500)
            , BATTERY_VALUE = BigInteger.valueOf(200)
            ;

    private static final String ACTUAL_DATA = "{ \"ptu\": " + (PTU_NUMBER-1) + ", \"devices\": [ { \"device\": \"PV\", \"value\" : " + PV_VALUE + " }, { \"device\": \"BATTERY\", \"value\":  " + BATTERY_VALUE + " },{ \"device\": \"Unctr\", \"value\" :  " + UCL_VALUE + " } ]}"
                                ;

    private static final LocalDate PERIOD = new LocalDate();

    private DetermineNetDemands determineNetDemands;

    @Mock
    private CountDownLatch latch;

    @Mock
    private MqttConnection connection;

    @Before
    public void init() throws Exception
    {
        //PowerMockito.when(latch.await(Matchers.anyInt(), Matchers.any(TimeUnit.class))).thenReturn(true);

        PowerMockito.doNothing().when(connection).subscribe(Matchers.anyString());
        PowerMockito.doNothing().when(connection).unsubscribe(Matchers.anyString());
        PowerMockito.doNothing().when(connection).removeNonReceived(Matchers.anyString());
        PowerMockito.when(connection.isConnected()).thenReturn(true);
        PowerMockito.doAnswer(invocation ->
        {
            //0 = send topic, 1 = send payload, 2 = response
            Object[] args = invocation.getArguments();
            MqttResponseMessage responseMessage = (MqttResponseMessage) args[2];
            MqttMessage response = new MqttMessage(ACTUAL_DATA.getBytes());
            responseMessage.setMessage(response);
            responseMessage.getLatch().countDown();
            return null;
        })
        .when(connection).publishAndReceive(Matchers.anyString(), Matchers.anyString(), Matchers.any(MqttResponseMessage.class) );

        determineNetDemands = new DetermineNetDemands();
        Whitebox.setInternalState(determineNetDemands, "messageService", connection);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvoke() throws Exception
    {
        WorkflowContext result = determineNetDemands.invoke(buildInputContext(PERIOD));

        List<UdiEventDto> events = result.get(OUT.UPDATED_UDI_EVENT_DTO_LIST.name(), List.class);
        assertNotNull(events);
        assertEquals(CONNECTIONS_COUNT, events.size());

        List<ConnectionPortfolioDto> connections = result.get(OUT.CONNECTION_PORTFOLIO_DTO_LIST.name(), List.class);
        assertEquals(CONNECTIONS_COUNT, connections.size());

        connections.forEach(connection ->
        {
            PowerDataDto observed = connection.getConnectionPowerPerPTU().get(PTU_NUMBER).getObserved();
            assertEquals(UCL_VALUE, observed.getUncontrolledLoad());
            assertEquals(ZERO, observed.getAverageConsumption());
            assertEquals(ZERO, observed.getAverageProduction());
            assertEquals(ZERO, observed.getPotentialFlexConsumption());
            assertEquals(ZERO, observed.getPotentialFlexProduction());

            connection.getUdis().forEach(udi ->
            {
                PowerDataDto udiObserved = udi.getUdiPowerPerDTU().get(PTU_NUMBER).getObserved();

                switch (udi.getProfile())
                {
                    case ElementType.BATTERY_NOD:
                    case ElementType.BATTERY_ZIH:
                        assertEquals(ZERO, udiObserved.getUncontrolledLoad());
                        assertEquals(BATTERY_VALUE, udiObserved.getAverageConsumption());
                        assertEquals(ZERO, udiObserved.getAverageProduction());
                        assertEquals(ZERO, udiObserved.getPotentialFlexConsumption());
                        assertEquals(ZERO, udiObserved.getPotentialFlexProduction());
                        break;


                    case ElementType.PV:
                        assertEquals(ZERO, udiObserved.getUncontrolledLoad());
                        assertEquals(ZERO, udiObserved.getAverageConsumption());
                        assertEquals(PV_VALUE, udiObserved.getAverageProduction());
                        assertEquals(ZERO, udiObserved.getPotentialFlexConsumption());
                        assertEquals(ZERO, udiObserved.getPotentialFlexProduction());
                        break;
                }

            });
        });

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInvokeForDayAhead() throws Exception
    {
        WorkflowContext result = determineNetDemands.invoke(buildInputContext(PERIOD.plusDays(1)));

        List<UdiEventDto> events = result.get(OUT.UPDATED_UDI_EVENT_DTO_LIST.name(), List.class);
        assertNotNull(events);
        assertEquals(CONNECTIONS_COUNT, events.size());

        List<ConnectionPortfolioDto> connections = result.get(OUT.CONNECTION_PORTFOLIO_DTO_LIST.name(), List.class);
        assertEquals(CONNECTIONS_COUNT, connections.size());

        connections.forEach(connection ->
        {
            PowerDataDto observed = connection.getConnectionPowerPerPTU().get(PTU_NUMBER).getObserved();
            assertNull(observed.getUncontrolledLoad());
            assertNull(observed.getAverageConsumption());
            assertNull(observed.getAverageProduction());
            assertNull(observed.getPotentialFlexConsumption());
            assertNull(observed.getPotentialFlexProduction());

            connection.getUdis().forEach(udi ->
            {
                PowerDataDto udiObserved = udi.getUdiPowerPerDTU().get(PTU_NUMBER).getObserved();
                assertNull(udiObserved.getUncontrolledLoad());
                assertNull(udiObserved.getAverageConsumption());
                assertNull(udiObserved.getAverageProduction());
                assertNull(udiObserved.getPotentialFlexConsumption());
                assertNull(udiObserved.getPotentialFlexProduction());
            });
        });
    }
    private UdiPortfolioDto buildUdi(String EAN, String profile)
    {
        UdiPortfolioDto udi = new UdiPortfolioDto( EAN + "/usef/" + profile, PTU_COUNT, profile);
        IntStream.rangeClosed(1, PTU_COUNT).forEach(ptuIdx -> udi.getUdiPowerPerDTU().put(ptuIdx, new PowerContainerDto(PERIOD, ptuIdx)) );
        return udi;
    }

    private UdiEventDto buildBatteryEvent(String endpoint)
    {
        UdiEventDto batteryEvent = new UdiEventDto();

        batteryEvent.setUdiEndpoint(endpoint + "/usef/" + ElementType.BATTERY);
        batteryEvent.setDeviceSelector(endpoint + "/usef/" + ElementType.BATTERY);
        batteryEvent.getDeviceCapabilities().add(new ShiftCapabilityDto());

        batteryEvent.setId("");
        batteryEvent.setPeriod(PERIOD);
        batteryEvent.setStartDtu(1);
        batteryEvent.setEndDtu(PTU_DURATION);
        batteryEvent.setUdiEventType(UdiEventTypeDto.CONSUMPTION);

        return batteryEvent;
    }

    private  Map<String, Map<String, List<UdiEventDto>>> buildEvents()
    {
        Map<String, Map<String, List<UdiEventDto>>> connectionMap = new HashMap<>();

        IntStream.rangeClosed(1, CONNECTIONS_COUNT).forEach(connIdx ->
        {
            Map<String, List<UdiEventDto>> udiMap = new HashMap<>();
            List<UdiEventDto> events = new ArrayList<>();

            String ean = EANUtil.toEAN(connIdx);
            UdiEventDto event = buildBatteryEvent(ean);

            events.add(event);
            udiMap.put(event.getId(), events);

            connectionMap.put(ean, udiMap);
        });

        return connectionMap;
    }

    private List<ConnectionPortfolioDto> buildConnections()
    {
        List<ConnectionPortfolioDto> portfolios = new ArrayList<>();

        IntStream.rangeClosed(1, CONNECTIONS_COUNT).forEach(connectionIdx ->
        {
            String EAN = EANUtil.toEAN(connectionIdx);
            ConnectionPortfolioDto connection = new ConnectionPortfolioDto(EAN);
            IntStream.rangeClosed(1, PTU_COUNT).forEach(ptuIdx -> connection.getConnectionPowerPerPTU().put(ptuIdx, new PowerContainerDto(PERIOD, ptuIdx)));

            if(connectionIdx < CONNECTIONS_COUNT/2)
            {
                connection.getUdis().add( buildUdi(EAN, ElementType.BATTERY_ZIH)  );
                connection.getUdis().add( buildUdi(EAN, ElementType.PV)  );
            }
            else
            {
                connection.getUdis().add( buildUdi(EAN, ElementType.BATTERY_NOD)  );
            }

            portfolios.add(connection);
        });

        return portfolios;
    }

    private WorkflowContext buildInputContext(LocalDate period)
    {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(IN.PTU_DURATION.name(), PTU_DURATION);
        context.setValue(IN.PERIOD.name(), period);
        context.setValue(IN.CONNECTION_PORTFOLIO_DTO_LIST.name(), buildConnections());
        context.setValue(IN.UDI_EVENT_DTO_MAP.name(), buildEvents());
        return context;
    }

}
