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
import info.usef.agr.workflow.validate.flexoffer.FlexOfferDetermineFlexibilityStepParameter.IN;
import info.usef.agr.workflow.validate.flexoffer.FlexOfferDetermineFlexibilityStepParameter.OUT;
import info.usef.core.util.DateTimeUtil;
import info.usef.core.util.PtuUtil;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.WorkflowStep;
import info.usef.core.workflow.dto.DispositionTypeDto;
import info.usef.core.workflow.dto.FlexOfferDto;
import info.usef.core.workflow.dto.FlexRequestDto;
import info.usef.core.workflow.dto.PtuFlexOfferDto;
import info.usef.core.workflow.dto.PtuFlexRequestDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import static java.math.BigInteger.ZERO;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import nl.energieprojecthoogdalem.agr.devicemessages.ReservedDevice;
import nl.energieprojecthoogdalem.configurationservice.AgrConfiguration;
import nl.energieprojecthoogdalem.forecastservice.element.ElementType;
import nl.energieprojecthoogdalem.util.PortfolioUtil;
import nl.energieprojecthoogdalem.util.ReserveDeviceUtil;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hoogdalem Implementation of workflow step "FlexOfferDetermineFlexibility".
 * <p>
 * The PBC receives the following parameters as input: <ul> <li>PERIOD: period {@link org.joda.time.LocalDate} for which flex
 * requests are processed.</li> <li>PTU_DURATION: duration of a PTU in minutes.</li> <li>LATEST_D_PROGNOSES_DTO_LIST: {@link List}
 * of {@link info.usef.core.workflow.dto.PrognosisDto}.</li> <li>LATEST_A_PLANS_DTO_LIST : the list of the latest A-Plans {@link
 * info.usef.core.workflow.dto.PrognosisDto} of the 'A-Plan'type.</li> <li>FLEX_OFFER_DTO_LIST : the list of already placed flex
 * offers {@link FlexOfferDto} for the period.</li> <li>FLEX_REQUEST_DTO_LIST : the list of flex requests {@link FlexRequestDto} to
 * process.</li> <li>CONNECTION_PORTFOLIO_DTO : the current connection portfolio, a list of {@link ConnectionPortfolioDto}.</li>
 * <li>CONNECTION_GROUPS_TO_CONNECTIONS_MAP : a map providing the relationship between a connection group and the connections
 * attached to it.</li> </ul>
 * <p>
 * The PBC returns the following parameters as output: <ul> <li>FLEX_OFFER_DTO_LIST : Flex offer DTO list {@link List} of {@link
 * FlexOfferDto}.</li> </ul>
 */

@SuppressWarnings("unchecked")
public class FlexOfferDetermineFlexibility implements WorkflowStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FlexOfferDetermineFlexibility.class);
    private static final int FLEX_OFFER_EXPIRATION_DAYS = 1;
    private int CHARGE_DURATION;

    private static final String FLEX_CONSUMPTION = "CONSUMPTION"
            ,FLEX_PRODUCTION = "PRODUCTION"
            ;

    private BigDecimal MAX_CHARGE;

    /**
     * retrieves flex requests, connection portfolio,
     * splits ZIH, NOD connections,
     * determines the offer for ZIH,
     * shifts the requested potential flex consumption for ZIH,
     * shifts the available potential flex production ZIH,
     * determines the offer for NOD,
     * shifts the requested potential flex production for NOD,
     * shifts the available potential flex consumption NOD,
     * reserves shifted devices to a file,
     * returns the created flex offer(s)
     * */
    @Override
    public WorkflowContext invoke(WorkflowContext context)
    {
        LOGGER.debug("Received context parameters: {}", context);

        //input
        LocalDate period = context.get(IN.PERIOD.name(), LocalDate.class);
        int ptuDuration = context.get(IN.PTU_DURATION.name(), Integer.class)
                ,ptuCount = PtuUtil.getNumberOfPtusPerDay(period, ptuDuration)
                ,existingFlexOffersSize = context.get(IN.FLEX_OFFER_DTO_LIST.name(), List.class).size();

        if (existingFlexOffersSize > 0)
            LOGGER.warn("Ignoring {} existing flex offers !", existingFlexOffersSize);

        Properties prop = AgrConfiguration.getConfig("CREATE_FLEX_OFFER", period, ptuDuration);
        MAX_CHARGE = new BigDecimal(prop.getProperty(AgrConfiguration.BATTERY_CHARGE));
        CHARGE_DURATION = Integer.parseInt(prop.getProperty(AgrConfiguration.BATTERY_CHARGE_DURATION));


        List<FlexRequestDto> flexRequests = context.get(IN.FLEX_REQUEST_DTO_LIST.name(), List.class);
        List<ConnectionPortfolioDto> allConnections = context.get(IN.CONNECTION_PORTFOLIO_DTO.name(), List.class);

        List<ConnectionPortfolioDto> zihConnections = new ArrayList<>();
        List<ConnectionPortfolioDto> nodConnections = new ArrayList<>();

        PortfolioUtil.splitZIHNOD(allConnections, zihConnections, nodConnections);

        if(allConnections.size() > 0)
            LOGGER.warn("found {} ZIH connections, {} NOD connections, {} UNKNOWN connections", zihConnections.size(), nodConnections.size(), allConnections.size());

        //output
        List<FlexOfferDto> flexOffers = new ArrayList<>();

        for (FlexRequestDto flexRequest : flexRequests)
        {
            Map<String, ReservedDevice> deviceMap = new HashMap<>();

            Map<Integer, BigInteger> offer = defaultOfferMap(ptuCount);
            Map<Integer, PtuFlexRequestDto> requests = flexRequest.getPtus()
                    .stream()
                    .collect(Collectors.toMap(ptuFlexRequestDto -> ptuFlexRequestDto.getPtuIndex().intValue(), Function.identity()));

            List<Integer> shiftedPtuIndexes = new ArrayList<>();

            determineZIHFlexOffer(shiftedPtuIndexes, offer, requests, zihConnections, ptuCount);

            if(shiftedPtuIndexes.size() > 0)
            {
                shiftBatteries(sumBatteryPerPtu(zihConnections, FLEX_PRODUCTION), offer, new BigDecimal(shiftedPtuIndexes.size()), new BigDecimal(zihConnections.size()), ElementType.BATTERY_ZIH);
                ReserveDeviceUtil.reserveDeviceMessages(deviceMap, zihConnections, shiftedPtuIndexes, period, ElementType.BATTERY_ZIH);
            }

            shiftedPtuIndexes.clear();

            determineNODFlexOffer(shiftedPtuIndexes, offer, requests, nodConnections, ptuCount);

            if(shiftedPtuIndexes.size() > 0)
            {
                shiftBatteries( sumBatteryPerPtu(nodConnections, FLEX_CONSUMPTION), offer, new BigDecimal(shiftedPtuIndexes.size()), new BigDecimal(nodConnections.size()), ElementType.BATTERY_NOD);
                ReserveDeviceUtil.reserveDeviceMessages(deviceMap, nodConnections, shiftedPtuIndexes, period, ElementType.BATTERY_NOD);
            }

            ReserveDeviceUtil.writeReservation(flexRequest.getPeriod().toString(ReserveDeviceUtil.PERIOD_STRING_FORMAT), deviceMap);

            flexOffers.add(createFlexOffer(flexRequest, offer));

        }

        context.setValue(OUT.FLEX_OFFER_DTO_LIST.name(), flexOffers);
        return context;
    }

    /**
     * iterates over every connection address -&#62;
     *                     udi (where profile is battery zih or nod) -&#62;
     *                     and sums the potential flex forecast value, depending on flexType
     *
     * @param connections a list of {@link ConnectionPortfolioDto}
     * @param flexType the flexType to filter ( production / consumption)
     * @return returns a map per ptu index containing the summed potential flex production forecast values of the battery udi per connection, per connection group
     * */
    private Map<Integer, BigInteger> sumBatteryPerPtu( List<ConnectionPortfolioDto> connections, String flexType)
    {
        Map<Integer, BigInteger> batProdPerPtu = new HashMap<>();

        connections.stream()
                .flatMap(connection ->
                         connection.getUdis()
                                   .stream()
                                   .filter(udi -> ElementType.BATTERY_NOD.equals(udi.getProfile()) || ElementType.BATTERY_ZIH.equals(udi.getProfile())))
                .flatMap(udi -> udi.getUdiPowerPerDTU().values().stream())
                .forEach(powerContainerDto -> batProdPerPtu.merge(powerContainerDto.getTimeIndex(), FLEX_PRODUCTION.equals(flexType) ? powerContainerDto.getForecast().getPotentialFlexProduction() : powerContainerDto.getForecast().getPotentialFlexConsumption(), BigInteger::add));

        return batProdPerPtu;
    }

    /**
     * checks if the getDisposition of the {@link PtuFlexRequestDto} is equal to the {@link DispositionTypeDto} REQUESTED
     * @param  ptu the {@link PtuFlexRequestDto} to validate
     * @return boolean indicating if the {@link DispositionTypeDto} is equal to REQUESTED
     * */
    private static boolean isRequested(PtuFlexRequestDto ptu)
    {
        return DispositionTypeDto.REQUESTED.equals(ptu.getDisposition());
    }

    /**
     * adds the requested power values to the offer map,
     * calculated from the first request index to the request duration,
     * with value batteriesPerSet * MAX_CHARGE
     * @param offer a flex offer map where ptu indexes will be added with calculated power values
     * @param batteriesPerSet the amount of batteries required to solve the flex request
     * @param firstRequestIdx the first requested index
     * @param requestDuration the first requested index + charge duration
     * */
    private void shiftOffer(Map<Integer, BigInteger> offer, BigDecimal batteriesPerSet ,int firstRequestIdx, int requestDuration)
    {
        for(int requestIdx = firstRequestIdx; requestIdx <= requestDuration; requestIdx++)
        {
            BigDecimal power = batteriesPerSet.multiply(MAX_CHARGE);
            offer.merge(requestIdx, power.toBigInteger(), BigInteger::add);
        }
    }

    /**
     * shifts the available batteries in the flex offer
     * subtracts each value of the potential flex map to the flex offer map,
     * with calculation offer = offer - flex * (shifted / total)
     * @param flex the potential flex map for ZIH
     * @param offer the flex offer map to add power values to
     * @param numberOfShiftedBatteries the number of batteries to be shifted
     * @param totalNumberOfBatteries the total number of batteries available for one proposition
     * @param type the battery type (BATTERY ZIH | NOD)
     * */
    private void shiftBatteries(Map<Integer, BigInteger> flex, Map<Integer, BigInteger> offer, BigDecimal numberOfShiftedBatteries, BigDecimal totalNumberOfBatteries, String type)
    {
        for(int availableIdx = 1; availableIdx <= offer.size(); availableIdx++)
        {
            BigInteger power = new BigDecimal(flex.get(availableIdx)).multiply(numberOfShiftedBatteries).divide(totalNumberOfBatteries, BigDecimal.ROUND_CEILING).toBigInteger();

            if(ElementType.BATTERY_ZIH.equals(type))
                offer.merge(availableIdx, power, BigInteger::subtract);

            else
                offer.merge(availableIdx, power, BigInteger::add);
        }
    }

    /**
     * returns a default map with 0 values from 1 to the ptu count number
     * @param ptuCount the amount of values to generate 0 values for
     * @return a map with 1 - ptuCount as key and 0 as BigInteger value
     * */
    private Map<Integer, BigInteger> defaultOfferMap(int ptuCount)
    {
        return IntStream.rangeClosed(1, ptuCount)
                .boxed()
                .collect(Collectors.toMap(Function.identity(), idx -> ZERO));
    }

    /**
     * adds shifted ptu indexes to the list for the amount of batteries per set
     * @param shiftPtu the list to add new shift
     * @param batteriesPerSet the amount batteries that needs to be shifted
     * @param firstRequestIndex the index number to shift the batteries to
     * */
    private void addShiftIndexes(List<Integer> shiftPtu, BigDecimal batteriesPerSet, int firstRequestIndex)
    {
        IntStream.range(0, batteriesPerSet.intValue()).forEach(battery -> shiftPtu.add(firstRequestIndex));
    }

    /**
     * creates a {@link FlexOfferDto} for the offer map
     * @param flexRequest the flex request we create an offer for
     * @param offer the offer map containing power values for each ptu
     * @return the {@link FlexOfferDto} for the given flexRequest, filled with the offer ptus
     * */
    private FlexOfferDto createFlexOffer(FlexRequestDto flexRequest, Map<Integer, BigInteger> offer)
    {
        FlexOfferDto flexOffer = new FlexOfferDto();
        flexOffer.setPeriod(flexRequest.getPeriod());
        flexOffer.setExpirationDateTime(DateTimeUtil.getCurrentDateTime().plusDays(FLEX_OFFER_EXPIRATION_DAYS).withTime(0, 0, 0, 0));
        flexOffer.setConnectionGroupEntityAddress(flexRequest.getConnectionGroupEntityAddress());
        flexOffer.setParticipantDomain(flexRequest.getParticipantDomain());
        flexOffer.setFlexRequestSequenceNumber(flexRequest.getSequenceNumber());

        flexOffer.setPtus
                (
                        offer.entrySet()
                                .stream()
                                .map(map ->
                                {
                                    PtuFlexOfferDto offerPtu = new PtuFlexOfferDto();

                                    offerPtu.setPrice(BigDecimal.ZERO);
                                    offerPtu.setPtuIndex(BigInteger.valueOf(map.getKey()));
                                    offerPtu.setPower(map.getValue());

                                    return offerPtu;
                                })
                                .collect(Collectors.toList())
                );

        return flexOffer;
    }

    /**
     * determines the peak power value in the first requested index and first requested index + charge duration,
     * checks whenever there are more batteries available than needed,
     * checks whenever we try to shift more batteries than total batteries and already shifted batteries,
     * @param batteriesAvailable the available batteries for the firstRequestIdx
     * @param requests the requested map to determine the peak power value
     * @param shiftedPtuIndexes a list to add shifted zih ptu indexes to
     * @param firstRequestIdx the first requested ptu we want to solve
     * @param requestDuration the duration of one request (first requested idx + charge duration)
     * @param connectionSize the size of the list containing ZIH | NOD {@link ConnectionPortfolioDto}'s
     * @return a number indicating how many batteries can be shifted
     * */
    private BigDecimal determineBatteriesPerSet(BigDecimal batteriesAvailable, Map<Integer, PtuFlexRequestDto> requests
            , List<Integer> shiftedPtuIndexes , int firstRequestIdx, int requestDuration, int connectionSize )
    {
        BigInteger peakValue = ZERO;
        BigDecimal batteriesNeeded
                ,batteriesPerSet
                ;

        for(int requestIdx = firstRequestIdx; requestIdx <= requestDuration; requestIdx++)
        {
            BigInteger requestPower = requests.get(requestIdx).getPower().abs();

            if(requestPower.compareTo(peakValue) > 0)
                peakValue = requestPower;

        }

        batteriesNeeded = new BigDecimal(peakValue).divide(MAX_CHARGE, BigDecimal.ROUND_CEILING);
        LOGGER.trace("peak value is {}, batteries needed {}", peakValue, batteriesNeeded);

        //limit to batteries available in firstRequestIdx
        batteriesPerSet = batteriesAvailable.compareTo(batteriesNeeded) > 0 ? batteriesNeeded : batteriesAvailable;
        LOGGER.trace("batteries needed {}, batteries available {} -> batteriesPerSet becomes {}", batteriesNeeded, batteriesAvailable, batteriesPerSet);

        //limit to total batteries available
        if(shiftedPtuIndexes.size() + batteriesPerSet.intValue() > connectionSize)
            batteriesPerSet = BigDecimal.valueOf(connectionSize - shiftedPtuIndexes.size());
        LOGGER.trace("shifted batteries {}, total batteries {} -> batteriesPerSet becomes {}", shiftedPtuIndexes.size(), connectionSize, batteriesPerSet);

        return batteriesPerSet;

    }

    /**
     * determines the offer for a requested period
     * sums the pot flex consumption of ZIH connection per ptu,
     * iterates over ptu the amount of ptus in a day,
     * when a requested ptu is found and the power value is negative,
     * determines the amount of batteries available that can be shifted,
     * places the shifted power values between the first requested ptu index and first requested ptu index + 8
     * places the first requested index a couple amount of times according to the number of batteries needed in the ShiftedPtuIndexes list
     *
     * @param shiftedPtuIndexes list containing requested ptus that needs to be shifted
     * @param offer a map containing the flex offer power per ptu
     * @param requests a map of power values of the flex request per ptu
     * @param zihConnections the connections for the ZIH proposition
     * @param ptuCount the amount of ptus in a day
     * */
    private void determineZIHFlexOffer( List<Integer> shiftedPtuIndexes, Map<Integer, BigInteger> offer, Map<Integer, PtuFlexRequestDto> requests
            , List<ConnectionPortfolioDto> zihConnections, int ptuCount)
    {
        Map<Integer, BigInteger> zihForecastFlexConsumption = sumBatteryPerPtu(zihConnections, FLEX_CONSUMPTION);

        for(int idx = 1; idx <= ptuCount; idx++ )
        {
            PtuFlexRequestDto request = requests.get(idx);
            if(isRequested(request) && request.getPower().signum() > 0)
            {
                if( shiftedPtuIndexes.size() == zihConnections.size() )
                    break;

                BigDecimal batteriesAvailable = new BigDecimal(zihForecastFlexConsumption.get(idx))
                        .divide(MAX_CHARGE, BigDecimal.ROUND_CEILING);

                if(batteriesAvailable.equals(BigDecimal.ZERO))
                {
                    LOGGER.debug("request idx {} no batteries available for proposition {}", idx, ElementType.ZIH);
                    continue;
                }

                int requestDuration = (idx + CHARGE_DURATION < ptuCount) ? idx + CHARGE_DURATION : ptuCount;
                BigDecimal batteriesPerSet = determineBatteriesPerSet(batteriesAvailable, requests, shiftedPtuIndexes, idx, requestDuration, zihConnections.size());

                shiftOffer(offer, batteriesPerSet, idx, requestDuration);
                addShiftIndexes(shiftedPtuIndexes, batteriesPerSet, idx);

                idx = requestDuration;
            }
        }
    }

    /**
     * determines the offer for a requested period
     * sums the pot flex production of NOD connection per ptu,
     * iterates over ptu the amount of ptus in a day,
     * when a requested ptu is found and the power value is negative,
     * determines the amount of batteries available that can be shifted,
     * places the shifted power values between the first requested ptu index and first requested ptu index + 8
     * places the first requested index a couple amount of times according to the number of batteries needed in the ShiftedPtuIndexes list
     *
     * @param shiftedPtuIndexes list containing requested ptus that needs to be shifted
     * @param offer a map containing the flex offer power per ptu
     * @param requests a map of power values of the flex request per ptu
     * @param nodConnections the connections for the NOD proposition
     * @param ptuCount the amount of ptus in a day
     * */
    private void determineNODFlexOffer( List<Integer> shiftedPtuIndexes, Map<Integer, BigInteger> offer, Map<Integer, PtuFlexRequestDto> requests
            , List<ConnectionPortfolioDto> nodConnections, int ptuCount)
    {
        Map<Integer, BigInteger> nodForecastFlexProduction = sumBatteryPerPtu(nodConnections, FLEX_PRODUCTION);

        for(int idx = 1; idx <= ptuCount; idx++ )
        {
            PtuFlexRequestDto request = requests.get(idx);
            if(isRequested(request) && request.getPower().signum() < 0)
            {
                if( shiftedPtuIndexes.size() == nodConnections.size() )
                    break;

                BigDecimal batteriesAvailable = new BigDecimal(nodForecastFlexProduction.get(idx))
                        .divide(MAX_CHARGE, BigDecimal.ROUND_CEILING);

                if(batteriesAvailable.equals(BigDecimal.ZERO))
                {
                    LOGGER.debug("request idx {} no batteries available for proposition {}", idx, ElementType.NOD);
                    continue;
                }

                int requestDuration = (idx + CHARGE_DURATION < ptuCount) ? idx + CHARGE_DURATION : ptuCount;
                BigDecimal batteriesPerSet = determineBatteriesPerSet(batteriesAvailable, requests, shiftedPtuIndexes, idx, requestDuration, nodConnections.size());

                shiftOffer(offer, batteriesPerSet.negate(), idx, requestDuration);
                addShiftIndexes(shiftedPtuIndexes, batteriesPerSet, idx);

                idx = requestDuration;
            }
        }
    }
}