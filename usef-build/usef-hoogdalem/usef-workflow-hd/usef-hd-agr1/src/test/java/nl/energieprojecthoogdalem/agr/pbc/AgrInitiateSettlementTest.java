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
import info.usef.agr.dto.ForecastPowerDataDto;
import info.usef.agr.dto.PowerContainerDto;
import info.usef.agr.dto.PowerDataDto;
import info.usef.agr.dto.UdiPortfolioDto;
import info.usef.agr.workflow.settlement.initiate.AgrInitiateSettlementParameter;
import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.dto.FlexOrderDto;
import info.usef.core.workflow.dto.FlexOrderSettlementDto;
import info.usef.core.workflow.dto.PrognosisDto;
import info.usef.core.workflow.dto.PtuFlexOrderDto;
import info.usef.core.workflow.dto.PtuPrognosisDto;
import info.usef.core.workflow.dto.SettlementDto;
import info.usef.core.workflow.settlement.CoreInitiateSettlementParameter;
import info.usef.core.workflow.settlement.CoreInitiateSettlementParameter.OUT;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AgrInitiateSettlementTest {

    private static final int PTUS_PER_DAY = 12;
    private static final int PTU_DURATION = 120;
    private static final LocalDate START_DATE = new LocalDate(2015, 11, 1);
    private static final LocalDate END_DATE = new LocalDate(2015, 11, 30);
    private AgrInitiateSettlement agrInitiateSettlement;

    private static final String congestionPoint1 = "ean.000000000001";
    private static final String congestionPoint2 = "ean.000000000002";
    private static final String connection1 = "ean.000000000003";
    private static final String connection2 = "ean.000000000004";
    private static final String connection3 = "ean.000000000005";

    @Before
    public void init() {
        agrInitiateSettlement = new AgrInitiateSettlement();
    }

    @Test
    public void testInvoke() throws Exception {
        WorkflowContext context = buildHappyFlowContext();
        // invocation
        WorkflowContext outContext = agrInitiateSettlement.invoke(context);
        // verifications
        SettlementDto settlementDto = outContext.get(OUT.SETTLEMENT_DTO.name(), SettlementDto.class);
        Assert.assertNotNull(settlementDto);
        Assert.assertEquals(START_DATE, settlementDto.getStartDate());
        Assert.assertEquals(END_DATE, settlementDto.getEndDate());
        Assert.assertEquals(4, settlementDto.getFlexOrderSettlementDtos().size());
        // validate flex order for first day, congestion point 1
        FlexOrderSettlementDto fosdto11 = settlementDto.getFlexOrderSettlementDtos()
                .stream()
                .filter(item -> congestionPoint1.equals(item.getFlexOrder().getConnectionGroupEntityAddress()))
                .filter(item -> START_DATE.equals(item.getPeriod())).findFirst().get();
        FlexOrderSettlementDto fosdto12 = settlementDto.getFlexOrderSettlementDtos()
                .stream()
                .filter(item -> congestionPoint2.equals(item.getFlexOrder().getConnectionGroupEntityAddress()))
                .filter(item -> START_DATE.equals(item.getPeriod())).findFirst().get();
        FlexOrderSettlementDto fosdto21 = settlementDto.getFlexOrderSettlementDtos()
                .stream()
                .filter(item -> congestionPoint1.equals(item.getFlexOrder().getConnectionGroupEntityAddress()))
                .filter(item -> START_DATE.plusDays(1).equals(item.getPeriod())).findFirst().get();
        FlexOrderSettlementDto fosdto22 = settlementDto.getFlexOrderSettlementDtos()
                .stream()
                .filter(item -> congestionPoint2.equals(item.getFlexOrder().getConnectionGroupEntityAddress()))
                .filter(item -> START_DATE.plusDays(1).equals(item.getPeriod())).findFirst().get();
        assertFlexOrderSettlementDtoValues(fosdto11, 2060L, 3000L, -1000L, -1000L, 0L);
        assertFlexOrderSettlementDtoValues(fosdto12, 1030L, 2000L, -1000L, -1000L, 0L);
        assertFlexOrderSettlementDtoValues(fosdto21, 4060L, 3000L, -1000L, 0L, 1000L);
        assertFlexOrderSettlementDtoValues(fosdto22, 2030L, 1000L, -1000L, 0L, 1000L);
    }

    private void assertFlexOrderSettlementDtoValues(FlexOrderSettlementDto fosdto, Long actualPower,
            Long prognosisPower, Long orderedFlexPower, Long deliveredFlexPower, Long powerDeficiency) {
        fosdto.getPtuSettlementDtos().stream().forEach(ptuSettlementDto -> {
            Assert.assertEquals(BigInteger.valueOf(prognosisPower), ptuSettlementDto.getPrognosisPower());
            Assert.assertEquals(BigInteger.valueOf(actualPower), ptuSettlementDto.getActualPower());
            Assert.assertEquals(BigInteger.valueOf(orderedFlexPower), ptuSettlementDto.getOrderedFlexPower());
            Assert.assertEquals(BigInteger.valueOf(deliveredFlexPower), ptuSettlementDto.getDeliveredFlexPower());
            Assert.assertEquals(BigInteger.valueOf(powerDeficiency), ptuSettlementDto.getPowerDeficiency());
        });
    }

    private WorkflowContext buildHappyFlowContext() {
        WorkflowContext inContext = new DefaultWorkflowContext();
        inContext.setValue(CoreInitiateSettlementParameter.IN.PTU_DURATION.name(), PTU_DURATION);
        inContext.setValue(CoreInitiateSettlementParameter.IN.START_DATE.name(), START_DATE);
        inContext.setValue(CoreInitiateSettlementParameter.IN.END_DATE.name(), END_DATE);
        inContext.setValue(CoreInitiateSettlementParameter.IN.FLEX_ORDER_DTO_LIST.name(), buildFlexOrders());
        inContext.setValue(CoreInitiateSettlementParameter.IN.PROGNOSIS_DTO_LIST.name(), buildPrognoses());
        inContext.setValue(AgrInitiateSettlementParameter.IN.CONNECTION_PORTFOLIO_DTO_LIST.name(), buildConnectionPortfolio());
        inContext.setValue(AgrInitiateSettlementParameter.IN.CONNECTION_GROUPS_TO_CONNECTIONS_MAP.name(),
                buildPlanboardRelations());
        inContext.setValue(AgrInitiateSettlementParameter.IN.CONNECTION_GROUP_PORTFOLIO_DTO_PER_PERIOD_MAP.name(), new HashMap<>());
        return inContext;
    }

    private Map<String, List<String>> buildPlanboardRelations() {
        Map<String, List<String>> result = new HashMap<>();
        result.put(congestionPoint1, Arrays.asList(connection1, connection2));
        result.put(congestionPoint2, Collections.singletonList(connection3));
        return result;
    }

    private Map<LocalDate, List<ConnectionPortfolioDto>> buildConnectionPortfolio() {

        ConnectionPortfolioDto connectionDto11 = createConnectionPortfolioDto(connection1);
        ConnectionPortfolioDto connectionDto21 = createConnectionPortfolioDto(connection2);
        ConnectionPortfolioDto connectionDto31 = createConnectionPortfolioDto(connection3);
        ConnectionPortfolioDto connectionDto12 = createConnectionPortfolioDto(connection1);
        ConnectionPortfolioDto connectionDto22 = createConnectionPortfolioDto(connection2);
        ConnectionPortfolioDto connectionDto32 = createConnectionPortfolioDto(connection3);

        Map<LocalDate, List<ConnectionPortfolioDto>> portfolioMap = new HashMap<>();
        // uncontrolled load for connections: 1000W per Ptu on day 1, 2000W per Ptu on day 2
        IntStream.rangeClosed(1, PTUS_PER_DAY)
                .forEach(index -> connectionDto11.getConnectionPowerPerPTU()
                        .put(index, buildPowerContainerDto(START_DATE, index, BigInteger.valueOf(1000L))));
        IntStream.rangeClosed(1, PTUS_PER_DAY)
                .forEach(index -> connectionDto12.getConnectionPowerPerPTU()
                        .put(index, buildPowerContainerDto(START_DATE.plusDays(1), index, BigInteger.valueOf(2000L))));
        IntStream.rangeClosed(1, PTUS_PER_DAY)
                .forEach(index -> connectionDto21.getConnectionPowerPerPTU()
                        .put(index, buildPowerContainerDto(START_DATE, index, BigInteger.valueOf(1000L))));

        IntStream.rangeClosed(1, PTUS_PER_DAY)
                .forEach(index -> connectionDto22.getConnectionPowerPerPTU()
                        .put(index, buildPowerContainerDto(START_DATE.plusDays(1), index, BigInteger.valueOf(2000L))));
        IntStream.rangeClosed(1, PTUS_PER_DAY)
                .forEach(index -> connectionDto31.getConnectionPowerPerPTU()
                        .put(index, buildPowerContainerDto(START_DATE, index, BigInteger.valueOf(1000L))));
        IntStream.rangeClosed(1, PTUS_PER_DAY)
                .forEach(index -> connectionDto32.getConnectionPowerPerPTU()
                        .put(index, buildPowerContainerDto(START_DATE.plusDays(1), index, BigInteger.valueOf(2000L))));

        portfolioMap.put(START_DATE, Arrays.asList(connectionDto11, connectionDto21, connectionDto31));
        portfolioMap.put(START_DATE.plusDays(1), Arrays.asList(connectionDto12, connectionDto22, connectionDto32));
        return portfolioMap;
    }

    private PowerContainerDto buildPowerContainerDto(LocalDate period, Integer index, BigInteger power) {
        PowerContainerDto powerContainerDto = new PowerContainerDto(period, index);
        powerContainerDto.setProfile(buildUncontrolledPowerDataDto(power));
        powerContainerDto.setForecast(buildForecastUncontrolledPowerDataDto(power));
        powerContainerDto.setObserved(buildUncontrolledPowerDataDto(power));
        return powerContainerDto;
    }

    private PowerDataDto buildUncontrolledPowerDataDto(BigInteger power) {
        PowerDataDto powerDataDto = new PowerDataDto();
        powerDataDto.setUncontrolledLoad(power);
        return powerDataDto;
    }

    private ForecastPowerDataDto buildForecastUncontrolledPowerDataDto(BigInteger power) {
        ForecastPowerDataDto powerDataDto = new ForecastPowerDataDto();
        powerDataDto.setUncontrolledLoad(power);
        return powerDataDto;
    }

    private List<FlexOrderDto> buildFlexOrders() {
        FlexOrderDto flexOrderDto11 = new FlexOrderDto();
        flexOrderDto11.setParticipantDomain("dso1.usef-example.com");
        flexOrderDto11.setConnectionGroupEntityAddress(congestionPoint1);
        flexOrderDto11.setPeriod(START_DATE);
        flexOrderDto11.setSequenceNumber(1L);
        FlexOrderDto flexOrderDto12 = new FlexOrderDto();
        flexOrderDto12.setParticipantDomain("dso1.usef-example.com");
        flexOrderDto12.setConnectionGroupEntityAddress(congestionPoint2);
        flexOrderDto12.setPeriod(START_DATE);
        flexOrderDto12.setSequenceNumber(2L);
        FlexOrderDto flexOrderDto21 = new FlexOrderDto();
        flexOrderDto21.setParticipantDomain("dso1.usef-example.com");
        flexOrderDto21.setConnectionGroupEntityAddress(congestionPoint1);
        flexOrderDto21.setPeriod(START_DATE.plusDays(1));
        flexOrderDto21.setSequenceNumber(3L);
        FlexOrderDto flexOrderDto22 = new FlexOrderDto();
        flexOrderDto22.setParticipantDomain("dso1.usef-example.com");
        flexOrderDto22.setConnectionGroupEntityAddress(congestionPoint2);
        flexOrderDto22.setPeriod(START_DATE.plusDays(1));
        flexOrderDto22.setSequenceNumber(4L);

        // ptus
        IntStream.rangeClosed(1, PTUS_PER_DAY)
                .mapToObj(index -> new PtuFlexOrderDto(BigInteger.valueOf(index), BigInteger.valueOf(-1000), BigDecimal.ONE))
                .forEach(ptu -> flexOrderDto11.getPtus().add(ptu));
        IntStream.rangeClosed(1, PTUS_PER_DAY)
                .mapToObj(index -> new PtuFlexOrderDto(BigInteger.valueOf(index), BigInteger.valueOf(-1000), BigDecimal.ONE))
                .forEach(ptu -> flexOrderDto12.getPtus().add(ptu));
        IntStream.rangeClosed(1, PTUS_PER_DAY)
                .mapToObj(index -> new PtuFlexOrderDto(BigInteger.valueOf(index), BigInteger.valueOf(-1000), BigDecimal.ONE))
                .forEach(ptu -> flexOrderDto21.getPtus().add(ptu));
        IntStream.rangeClosed(1, PTUS_PER_DAY)
                .mapToObj(index -> new PtuFlexOrderDto(BigInteger.valueOf(index), BigInteger.valueOf(-1000), BigDecimal.ONE))
                .forEach(ptu -> flexOrderDto22.getPtus().add(ptu));

        return Arrays.asList(flexOrderDto11, flexOrderDto12, flexOrderDto21, flexOrderDto22);
    }

    private List<PrognosisDto> buildPrognoses() {
        PrognosisDto prognosisDto11 = new PrognosisDto();
        prognosisDto11.setParticipantDomain("dso1.usef-example.com");
        prognosisDto11.setConnectionGroupEntityAddress(congestionPoint1);
        prognosisDto11.setPeriod(START_DATE);
        prognosisDto11.setSequenceNumber(1L);
        PrognosisDto prognosisDto12 = new PrognosisDto();
        prognosisDto12.setParticipantDomain("dso1.usef-example.com");
        prognosisDto12.setConnectionGroupEntityAddress(congestionPoint2);
        prognosisDto12.setPeriod(START_DATE);
        prognosisDto12.setSequenceNumber(2L);
        PrognosisDto prognosisDto21 = new PrognosisDto();
        prognosisDto21.setParticipantDomain("dso1.usef-example.com");
        prognosisDto21.setConnectionGroupEntityAddress(congestionPoint1);
        prognosisDto21.setPeriod(START_DATE.plusDays(1));
        prognosisDto21.setSequenceNumber(3L);
        PrognosisDto prognosisDto22 = new PrognosisDto();
        prognosisDto22.setParticipantDomain("dso1.usef-example.com");
        prognosisDto22.setConnectionGroupEntityAddress(congestionPoint2);
        prognosisDto22.setPeriod(START_DATE.plusDays(1));
        prognosisDto22.setSequenceNumber(4L);
        // ptus
        IntStream.rangeClosed(1, PTUS_PER_DAY)
                .mapToObj(index -> new PtuPrognosisDto(BigInteger.valueOf(index), BigInteger.valueOf(3000)))
                .forEach(ptu -> prognosisDto11.getPtus().add(ptu));
        IntStream.rangeClosed(1, PTUS_PER_DAY)
                .mapToObj(index -> new PtuPrognosisDto(BigInteger.valueOf(index), BigInteger.valueOf(2000)))
                .forEach(ptu -> prognosisDto12.getPtus().add(ptu));
        IntStream.rangeClosed(1, PTUS_PER_DAY)
                .mapToObj(index -> new PtuPrognosisDto(BigInteger.valueOf(index), BigInteger.valueOf(3000)))
                .forEach(ptu -> prognosisDto21.getPtus().add(ptu));
        IntStream.rangeClosed(1, PTUS_PER_DAY)
                .mapToObj(index -> new PtuPrognosisDto(BigInteger.valueOf(index), BigInteger.valueOf(1000)))
                .forEach(ptu -> prognosisDto22.getPtus().add(ptu));

        return Arrays.asList(prognosisDto11, prognosisDto12, prognosisDto21, prognosisDto22);
    }

    private ConnectionPortfolioDto createConnectionPortfolioDto(String connection) {
        ConnectionPortfolioDto connectionPortfolioDto = new ConnectionPortfolioDto(connection);
        LocalDate period = new LocalDateTime().withHourOfDay(0)
                .withMinuteOfHour(20).withSecondOfMinute(1).withMillisOfSecond(1).toLocalDate();
        UdiPortfolioDto udiPortfolio = new UdiPortfolioDto("endpoint:" + connection, 60, "");

        IntStream.rangeClosed(1, 24).forEach(dtuIndex -> {
                    PowerContainerDto powerContainerDto = new PowerContainerDto(period, dtuIndex);
                    BigInteger consumption = BigInteger.valueOf((30L));
                    if (consumption.compareTo(BigInteger.ZERO) < 0) {
                        powerContainerDto.getForecast().setAverageConsumption(BigInteger.ZERO);
                        powerContainerDto.getForecast().setAverageProduction(BigInteger.ZERO);
                    } else {
                        powerContainerDto.getForecast().setAverageConsumption(consumption);
                        powerContainerDto.getForecast().setAverageProduction(BigInteger.ZERO);
                    }

                    udiPortfolio.getUdiPowerPerDTU().put(dtuIndex, powerContainerDto);
                }

        );

        connectionPortfolioDto.getUdis().add(udiPortfolio);

        // add uncontrolled load on connections level
        for (
                int ptuIndex = 1;
                ptuIndex <= 12; ptuIndex++)

        {
            PowerContainerDto uncontrolledLoad = new PowerContainerDto(period, ptuIndex);
            uncontrolledLoad.getForecast().setUncontrolledLoad(BigInteger.valueOf(100));
            connectionPortfolioDto.getConnectionPowerPerPTU().put(ptuIndex, uncontrolledLoad);
        }

        return connectionPortfolioDto;
    }

}
