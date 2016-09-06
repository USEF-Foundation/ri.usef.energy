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

import static java.math.BigInteger.ZERO;

import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.dto.PowerDataDto;
import info.usef.agr.dto.UdiPortfolioDto;
import info.usef.agr.dto.device.capability.UdiEventDto;
import info.usef.agr.workflow.operate.netdemand.DetermineNetDemandStepParameter.IN;
import info.usef.agr.workflow.operate.netdemand.DetermineNetDemandStepParameter.OUT;
import info.usef.core.util.PtuUtil;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import nl.energieprojecthoogdalem.forecastservice.element.ElementType;
import nl.energieprojecthoogdalem.messageservice.transportservice.MqttConnection;
import nl.energieprojecthoogdalem.messageservice.transportservice.data.ActualDeviceData;
import nl.energieprojecthoogdalem.messageservice.transportservice.mqttmessages.MqttResponseMessage;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Hoogdalem implementation for the Workflow 'AGR Determine Net Demands'. This implementation expects to find the following
 * parameters as input:
 * <ul>
 * <li>PERIOD: the period for which determine net demands will be executed.</li>
 * <li>PTU_DURATION: ptu duration in minutes ({@link Integer})</li>
 * <li>CONNECTION_PORTFOLIO_DTO_LIST: {@link List} of {@link ConnectionPortfolioDto} objects</li>
 * <li>UDI_EVENT_DTO_MAP: {@link Map} of {@link UdiEventDto} per Udi identifier per connection identifier.</li>
 * </ul>
 * Parameters as output:
 * <ul>
 * <li>CONNECTION_PORTFOLIO_DTO_LIST: List of {@link ConnectionPortfolioDto} containing the data of the Net Demand.</li>
 * <li>UPDATED_UDI_EVENT_DTO_LIST: {@link List} of {@link UdiEventDto} updated with the new capabilities.</li>
 * </ul>
 */
public class DetermineNetDemands implements WorkflowStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DetermineNetDemands.class);

    private static final int WAIT_TIMEOUT = 5 *1000;
    private static final String SUBSCRIBE_SUFFIX = "/usef/actual"
                                ,ENDPOINT_SUFFIX = "/usef/GetActual"
                                ,DEVICES = "devices"
                                ,UNCTR = "Unctr"
//                              , PTU = "ptu"
            ;

    @Inject
    private MqttConnection messageService;

    /**
     * validates the input context,
     * sets the updated udi event list to the input udi event list,
     * foreach connection, tries to receive the power values of the previous ptu for connection average consumption, battery UDI average production | consumption and pv UDI average production
     * if received, applies the data or 0 values to the observed value of the previous ptu
     * if not received sets no observed data
     * sets the updated connections in the context
     * */
    @SuppressWarnings("unchecked")
    @Override
    public WorkflowContext invoke(WorkflowContext context)
    {
        if(!validateInput(context))
        {
            context.setValue(OUT.CONNECTION_PORTFOLIO_DTO_LIST.name(), context.get(IN.CONNECTION_PORTFOLIO_DTO_LIST.name(), List.class));
            passUdiEvents(context);

            return context;
        }

        int ptuDuration = context.get(IN.PTU_DURATION.name(), Integer.class);
        int currentPtu =  PtuUtil.getPtuIndex(new LocalDateTime(), ptuDuration);
        LOGGER.info("requesting data for ptu index {}", currentPtu);

        List<ConnectionPortfolioDto> connections = context.get(IN.CONNECTION_PORTFOLIO_DTO_LIST.name(), List.class);
        passUdiEvents(context);

        //listen for any device response
        if(messageService.isConnected())
        {
            String wildcardSubscribe = '+' + SUBSCRIBE_SUFFIX;
            messageService.subscribe(wildcardSubscribe );

            connections.forEach(connection ->
            {
                String endpoint = getUdiEndpoint(connection.getUdis());
                String serial = endpoint.substring(0, endpoint.indexOf('/'));

                Map<String, BigInteger> actualDeviceData = sendAndReceiveActualData(serial);


                setConnectionObserved(connection, currentPtu, actualDeviceData.get(UNCTR));

                for(UdiPortfolioDto udi : connection.getUdis())
                {
                    switch (udi.getProfile())
                    {
                        case ElementType.BATTERY_ZIH:
                        case ElementType.BATTERY_NOD:
                            setBatteryObserved(udi, currentPtu, actualDeviceData.get(ElementType.BATTERY));
                            break;

                        case ElementType.PV:
                            setPVObserved(udi, currentPtu, actualDeviceData.get(ElementType.PV));
                            break;
                    }
                }

            });

            //stop listening for any response
            messageService.unsubscribe(wildcardSubscribe);
        }
        else
            LOGGER.error("No connection to the mqtt broker, no data can be received");
        context.setValue(OUT.CONNECTION_PORTFOLIO_DTO_LIST.name(), connections);

        return context;
    }

    /**
     * validates if the connection portfolio list is not empty
     * @param context the input context of the coordinator
     * @return true if everything is correctly validated, otherwise false
     * */
    private boolean validateInput(WorkflowContext context)
    {
        String message = null;
        LocalDate now = new LocalDate();

        if(context.get(IN.CONNECTION_PORTFOLIO_DTO_LIST.name(), List.class).isEmpty() )
            message = "Connection portfolio list is empty";

        if(context.get(IN.PERIOD.name(), LocalDate.class).compareTo(now) != 0)
            message = "Input period [" + context.get(IN.PERIOD.name(), LocalDate.class) + "] does not match now[" + now + "]";

        if( message == null)
            return true;

        else
        {
            LOGGER.error("Unable to execute PBC determine net demands reason: {}", message);
            return false;
        }
    }

    /**
     * subscribes to the serial/usef/actual topic,
     * publishes a request on the serial/usef/GetActual topic,
     * waits 5 seconds for a response
     * converts the json data to map values
     *
     * data keys:
     * ptu
     * Unctr
     * BATTERY
     * PV
     *
     * @param serial serial of the CG device to retrieve data from
     * @return a map containing ptu index and device data
     * */
    private Map<String, BigInteger> sendAndReceiveActualData(String serial)
    {
        Map<String, BigInteger> data = defaultDataMap();

        String receiveTopic = serial + SUBSCRIBE_SUFFIX;
        MqttResponseMessage responseMessage = new MqttResponseMessage(receiveTopic, 1);

        //send message
        messageService.publishAndReceive(serial + ENDPOINT_SUFFIX, "get", responseMessage);

        //wait for data
        boolean isReceived = false;
        try
        {
            isReceived = responseMessage.getLatch().await(WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException exception)
        {
            LOGGER.error("interrupted while trying to wait for message");
        }

        //if data available overwrite defaults
        if(isReceived)
        {
            try
            {
                MqttMessage received = responseMessage.getMessage();
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode root = objectMapper.readTree(received.getPayload());

                ActualDeviceData[] devices = objectMapper.convertValue(root.get(DEVICES), ActualDeviceData[].class);
                data = Arrays.stream(devices)
                             .collect(Collectors.toMap(ActualDeviceData::getDevice, ActualDeviceData::getValue));
            }
            catch(IOException exception)
            {
                LOGGER.error("unable to parse payload of message for endpoint {} reason: ", serial, exception);
            }
            catch(NullPointerException exception)
            {
                LOGGER.error("Nullpointer while parsing message for endpoint {} reason: ", serial, exception);
            }
        }
        //clear non-received messages
        else
            messageService.removeNonReceived(receiveTopic);

        return data;
    }

    /**
     * default 0 values for actual data,
     * devices:
     * PV
     * BATTERY
     * Unctr (uncontrolled load)
     *
     * @return  a map with device names as key and zero as values
     * */
    private Map<String, BigInteger> defaultDataMap()
    {
        Map<String, BigInteger> defaultMap = new HashMap<>();

        defaultMap.put(ElementType.BATTERY, ZERO);
        defaultMap.put(ElementType.PV, ZERO);
        defaultMap.put(UNCTR, ZERO);

        return defaultMap;
    }

    /**
     * gets the udi endpoint of any available udi
     * @param udis a list of udis from one connection
     * @return an endpoint from the first found UDI
     * */
    private String getUdiEndpoint(List<UdiPortfolioDto> udis)
    {
        return udis.stream()
                    .findFirst()
                    .get()
                    .getEndpoint();
    }

    /**
     * sets the average consumption of the connection observed
     * @param connection the connection to set the observed value
     * @param ptuIdx the ptu index to set the observed value to
     * @param value the value to use
     * */
    private void setConnectionObserved(ConnectionPortfolioDto connection, int ptuIdx , BigInteger value)
    {
        PowerDataDto observed = connection.getConnectionPowerPerPTU()
                .get(ptuIdx)
                .getObserved();

        observed.setUncontrolledLoad(value);
        observed.setAverageConsumption(ZERO);
        observed.setAverageProduction(ZERO);
        observed.setPotentialFlexConsumption(ZERO);
        observed.setPotentialFlexProduction(ZERO);

    }

    /**
     * sets the battery udi observed value
     * if the value is positive, it will be placed in average consumption
     * if the value is negative, it will be placed in average production
     * @param udi the battery udi to set the observed value
     * @param ptuIdx the ptu index to set the observed value to
     * @param value the value to use
     * */
    private void setBatteryObserved(UdiPortfolioDto udi, int ptuIdx, BigInteger value)
    {
        PowerDataDto observed = udi.getUdiPowerPerDTU()
                .get(ptuIdx)
                .getObserved();

        observed.setUncontrolledLoad(ZERO);
        observed.setPotentialFlexConsumption(ZERO);
        observed.setPotentialFlexProduction(ZERO);

        if(value.signum() > 0)
        {
            observed.setAverageConsumption(value);
            observed.setAverageProduction(ZERO);
        }
        else
        {
            observed.setAverageConsumption(ZERO);
            observed.setAverageProduction(value.abs());
        }

    }

    /**
     * sets the average production of the PV udi observed
     * @param udi the PV udi to set the observed value
     * @param ptuIdx the ptu index to set the observed value to
     * @param value the value to use
     * */
    private void setPVObserved(UdiPortfolioDto udi, int ptuIdx, BigInteger value)
    {
        PowerDataDto observed = udi.getUdiPowerPerDTU()
                .get(ptuIdx)
                .getObserved();

        observed.setUncontrolledLoad(ZERO);
        observed.setAverageConsumption(ZERO);
        observed.setAverageProduction(value);
        observed.setPotentialFlexConsumption(ZERO);
        observed.setPotentialFlexProduction(ZERO);
    }

    /**
     * passes the unmodified UDI Events to the output context, remapping the input context from map to list
     * @param context the workflow context to pass the udi events
     * */
    @SuppressWarnings("unchecked")
    private void passUdiEvents(WorkflowContext context)
    {
        Map<String, Map<String, List<UdiEventDto>>> udiEventDtoMap = context.get(IN.UDI_EVENT_DTO_MAP.name(), Map.class);

        context.setValue(OUT.UPDATED_UDI_EVENT_DTO_LIST.name(), udiEventDtoMap.values().stream()
                .flatMap(map -> map.values().stream())
                .flatMap(Collection::stream).collect(Collectors.toList()));
    }
}
