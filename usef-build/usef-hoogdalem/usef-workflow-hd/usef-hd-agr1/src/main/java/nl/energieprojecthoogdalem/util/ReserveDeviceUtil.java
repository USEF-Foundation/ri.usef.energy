/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.util;

import info.usef.agr.dto.ConnectionPortfolioDto;
import info.usef.agr.dto.UdiPortfolioDto;
import info.usef.core.config.AbstractConfig;
import nl.energieprojecthoogdalem.agr.devicemessages.ReservedDevice;
import nl.energieprojecthoogdalem.forecastservice.element.ElementType;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * utility to read | write reserved device messages
 * contents of RESERVED_MESSAGES_FILE
 * {
 *     "key1":
 *     {
 *          "endpointName":
 *          {
 *              "startIndex": 50
 *              ,"period": "yyyy-MM-dd"
 *          }
 *          ,"endpointName":
 *          {
 *              "startIndex": 70
 *              ,"period": "yyyy-MM-dd"
 *          }
 *           ,...
 *     }
 *
 *     ,"key2": {}
 *
 *     ,...
 * }
 * */
public class ReserveDeviceUtil
{
    public static String PERIOD_STRING_FORMAT = "yyyy-MM-dd";
    private static final Logger LOGGER = LoggerFactory.getLogger(ReserveDeviceUtil.class);
    private static String RESERVED_MESSAGES_FILE = AbstractConfig.getConfigurationFolder() + "reserved_messages.json"
            ;

    /**
     * reads the reservation using the specified key and returns the reserved map
     * @return a map containing shift requests (period, shifted start index per endpoint), a empty map if not found
     * @param key the key to search shift requests for
     * */
    public static Map<String, ReservedDevice> readReservation(String key)
    {
        Map<String, ReservedDevice> reservations = new HashMap<>();

        try
        {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setDateFormat(new SimpleDateFormat(PERIOD_STRING_FORMAT));

            JsonNode root = readFile();
            if(root != null)
            {
                JsonNode endpoints = root.get(key);

                if(endpoints == null)
                {
                    LOGGER.error("Unable to find key {} in file", key);
                    return  reservations;
                }

                Iterator<Map.Entry<String, JsonNode>> deviceIteratior = endpoints.getFields();
                while(deviceIteratior.hasNext())
                {
                    Map.Entry<String, JsonNode> entry = deviceIteratior.next();
                    reservations.put(entry.getKey(), mapper.treeToValue(entry.getValue(), ReservedDevice.class ));
                }
            }
        }
        catch(NullPointerException exception)
        {
            LOGGER.error("Uncaught nullpointer reading key {} in file, reason: ", key, exception);
        }
        catch (IOException exception)
        {
            LOGGER.error("unable to convert json in key to a ReservedDevice reason: ", exception);
        }

        return reservations;
    }

    /**
     * reads the json file and returns the root structure of the json file,
     * passes null when file can't be read
     * @return the root json node of the file or null if the file was not created
     * */
    private static JsonNode readFile()
    {
        JsonNode root = null;

        try
        {
            ObjectMapper objectMapper = new ObjectMapper();
            root = objectMapper.readTree(new FileInputStream(RESERVED_MESSAGES_FILE));
        }
        catch (IOException exception)
        {
            LOGGER.warn("unable to read {}", RESERVED_MESSAGES_FILE);
        }

        return root;
    }

    /**
     * writes | appends the resevation map to a json file using the provided device map
     * @param key the key to write the device map to
     * @param deviceMap the map containing shift requests (period, shifted start index per endpoint)
     * */
    public static void writeReservation(String key, Map<String, ReservedDevice> deviceMap)
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setDateFormat(new SimpleDateFormat(PERIOD_STRING_FORMAT));

        ObjectNode root = (ObjectNode) readFile();
        if(root == null)
        {
            try
            {
                ObjectNode devicesInReservation = mapper.createObjectNode();
                devicesInReservation.put (key, (JsonNode) mapper.valueToTree(deviceMap));
                mapper.writeValue(new File(RESERVED_MESSAGES_FILE), devicesInReservation);
            }
            catch (IllegalArgumentException exception)
            {
                LOGGER.error("unable to convert device map to json node, reason: ", exception);
            }
            catch(IOException exception)
            {
                LOGGER.error("Unable to write key {} to file, reason: ", key, exception);
            }
        }
        else
        {
            try
            {
                root.put(key, (JsonNode) mapper.valueToTree(deviceMap));
                mapper.writeValue(new File(RESERVED_MESSAGES_FILE), root);
            }
            catch (IOException exception)
            {
                LOGGER.error("Unable to append key {} to file, reason: ", key, exception);
            }
        }
    }


    /**
     * deletes a specific reservation in the json file using the provided key
     * @param key the key to delete a reservation for
     * */
    public static void deleteReservation(String key)
    {
        try
        {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = (ObjectNode) readFile();
            root.remove(key);

            mapper.writeValue(new File(RESERVED_MESSAGES_FILE), root);
        }
        catch (IOException exception)
        {
            LOGGER.error("unable to delete key {}, error writing new file, reason: ", exception);
        }
    }

    /**
     * deletes the json file where reservations will be written to
     * */
    public static void deleteReservationFile()
    {
        File reservationFile = new File(RESERVED_MESSAGES_FILE);

        if(reservationFile.delete())
            LOGGER.info("device reservation file deleted.");

        else
            LOGGER.info("device reservation file not deleted!");
    }


    /**
     * reserves device messages for batteries depending on type,
     * iterates over every ptu index that needs to be shifted,
     * looks for an available battery,
     * writes a reservation in the device map
     * @param deviceMap map to add shift reservations per endpoint to
     * @param connections the connections to select battery udis from
     * @param shiftedPtuIndexes a list containing shifted ptu indexes
     * @param period the date when the devices will be shifted
     * @param type search for zih or nod udis using "BATTERY_ZIH" or "BATTERY_NOD"
     * */
    public static void reserveDeviceMessages(Map<String, ReservedDevice> deviceMap, List<ConnectionPortfolioDto> connections
            , List<Integer> shiftedPtuIndexes, LocalDate period, String type)
    {
        Collections.sort(shiftedPtuIndexes);

        for(Integer shiftedPtuIdx : shiftedPtuIndexes)
        {
            Iterator<ConnectionPortfolioDto> connectionIterator = connections.iterator();

            UdiPortfolioDto batteryUdi = null;

            while(connectionIterator.hasNext())
            {
                ConnectionPortfolioDto connection = connectionIterator.next();
                batteryUdi = findBatteryUdi(connection.getUdis(), type, shiftedPtuIdx);

                if(batteryUdi != null)
                {
                    connectionIterator.remove();
                    break;
                }

            }

            if(batteryUdi != null)
            {
                ReservedDevice device = new ReservedDevice(shiftedPtuIdx , period.toString(PERIOD_STRING_FORMAT));
                deviceMap.put(batteryUdi.getEndpoint(), device);
            }
            else
                LOGGER.error("Unable to find available {} udi for shifted ptu index {}!", type, shiftedPtuIdx);
        }
    }

    /**
     * filters the specific {@link UdiPortfolioDto} from the list using the type parameter name as filter,
     * checks if the potential flex for the shift index is greater then 0
     * and returns the first matching argument otherwise null,
     * there is only one battery udi per connection
     *
     * @param udis the udis to filter from
     * @param type the udi type to search for
     * @param shiftIndex the shift index to check if the udi can be shifted
     * @return a battery udi that can be shifted or null if the battery cannot be shifted
     * */
    private static  UdiPortfolioDto findBatteryUdi(List<UdiPortfolioDto> udis, String type, int shiftIndex)
    {
        return udis.stream()
                .filter(udi -> type.equals(udi.getProfile()))
                .filter(udi ->
                {
                    switch(type)
                    {
                        case ElementType.BATTERY_ZIH:
                            return udi.getUdiPowerPerDTU().get(shiftIndex).getForecast().getPotentialFlexConsumption().compareTo(BigInteger.ZERO) > 0;

                        case ElementType.BATTERY_NOD:
                            return udi.getUdiPowerPerDTU().get(shiftIndex).getForecast().getPotentialFlexProduction().compareTo(BigInteger.ZERO) > 0;

                        default:
                            return false;
                    }
                })
                .findFirst()
                .orElse(null);

    }
}
