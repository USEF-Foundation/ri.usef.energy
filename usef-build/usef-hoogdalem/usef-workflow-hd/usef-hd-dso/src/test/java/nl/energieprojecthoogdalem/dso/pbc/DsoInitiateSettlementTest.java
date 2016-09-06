/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.dso.pbc;

import info.usef.core.workflow.DefaultWorkflowContext;
import info.usef.core.workflow.WorkflowContext;
import info.usef.core.workflow.dto.ConnectionMeterDataDto;
import info.usef.core.workflow.dto.FlexOrderDto;
import info.usef.core.workflow.dto.FlexOrderSettlementDto;
import info.usef.core.workflow.dto.MeterDataDto;
import info.usef.core.workflow.dto.MeterDataSetDto;
import info.usef.core.workflow.dto.PrognosisDto;
import info.usef.core.workflow.dto.PtuFlexOrderDto;
import info.usef.core.workflow.dto.PtuMeterDataDto;
import info.usef.core.workflow.dto.PtuPrognosisDto;
import info.usef.core.workflow.dto.SettlementDto;
import info.usef.core.workflow.settlement.CoreInitiateSettlementParameter.IN;
import info.usef.core.workflow.settlement.CoreInitiateSettlementParameter.OUT;
import info.usef.dso.workflow.dto.GridMonitoringDto;
import info.usef.dso.workflow.dto.PtuGridMonitoringDto;
import info.usef.dso.workflow.settlement.initiate.DsoInitiateSettlementParameter;
import nl.energieprojecthoogdalem.dso.pbc.DsoInitiateSettlement;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class in charge of the unit test related to the {@link DsoInitiateSettlement} class.
 */
public class DsoInitiateSettlementTest {

    private static final LocalDate START_DATE = new LocalDate(2015, 10, 1);
    private static final LocalDate END_DATE = new LocalDate(2015, 10, 30);
    private static final String CONGESTION_POINT = "ean.111111111111";
    private static final int PTU_DURATION = 120;
    private static final int PTUS_PER_DAY = 12;
    private static final String AGR1_DOMAIN = "agr1.usef-example.com";
    private static final String AGR2_DOMAIN = "agr2.usef-example.com";
    private DsoInitiateSettlement dsoInitiateSettlement;

    @Before
    public void setUp() throws Exception {
        dsoInitiateSettlement = new DsoInitiateSettlement();
    }

    @Test
    public void testInvokeWithSmartMeterData() throws Exception {
        WorkflowContext outContext = dsoInitiateSettlement.invoke(buildWorkflowContextWithMeterData());
        SettlementDto settlementDto = outContext.get(OUT.SETTLEMENT_DTO.name(), SettlementDto.class);
        Assert.assertNotNull(settlementDto);
        Assert.assertEquals(2, settlementDto.getFlexOrderSettlementDtos().size());
        FlexOrderSettlementDto flexOrderSettlementDto = settlementDto.getFlexOrderSettlementDtos().get(0);
        flexOrderSettlementDto.getPtuSettlementDtos().stream().forEach(ptuSettlementDto -> {
            Assert.assertEquals(BigInteger.valueOf(1500), ptuSettlementDto.getActualPower());
            Assert.assertEquals(BigInteger.valueOf(1000), ptuSettlementDto.getPrognosisPower());
            Assert.assertEquals(BigInteger.valueOf(-1000), ptuSettlementDto.getOrderedFlexPower());
            Assert.assertEquals(BigInteger.valueOf(500), ptuSettlementDto.getPowerDeficiency());
            Assert.assertEquals(BigInteger.valueOf(-500), ptuSettlementDto.getDeliveredFlexPower());
        });
    }

    private WorkflowContext buildWorkflowContextWithMeterData() {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(IN.PTU_DURATION.name(), PTU_DURATION);
        context.setValue(IN.START_DATE.name(), START_DATE);
        context.setValue(IN.END_DATE.name(), END_DATE);
        context.setValue(IN.FLEX_ORDER_DTO_LIST.name(), buildFlexOrderDtos());
        context.setValue(IN.PROGNOSIS_DTO_LIST.name(), buildPrognosisDtos());
        context.setValue(DsoInitiateSettlementParameter.IN.SMART_METER_DATA.name(), buildSmartMeterData());
        return context;
    }

    @Test
    public void testInvokeWithGridMonitoringData() throws Exception {
        WorkflowContext outContext = dsoInitiateSettlement.invoke(buildWorkflowContextWithGridMonitoring());
        SettlementDto settlementDto = outContext.get(OUT.SETTLEMENT_DTO.name(), SettlementDto.class);
        Assert.assertNotNull(settlementDto);
        Assert.assertEquals(2, settlementDto.getFlexOrderSettlementDtos().size());
        FlexOrderSettlementDto flexOrderSettlementDto = settlementDto.getFlexOrderSettlementDtos().stream()
                .filter(fos -> AGR1_DOMAIN.equals(fos.getFlexOrder().getParticipantDomain()))
                .findFirst().get();
        flexOrderSettlementDto.getPtuSettlementDtos().stream().forEach(ptuSettlementDto -> {
            Assert.assertEquals(BigInteger.valueOf(1500), ptuSettlementDto.getActualPower());
            Assert.assertEquals(BigInteger.valueOf(1000), ptuSettlementDto.getPrognosisPower());
            Assert.assertEquals(BigInteger.valueOf(-1000), ptuSettlementDto.getOrderedFlexPower());
            Assert.assertEquals(BigInteger.valueOf(500), ptuSettlementDto.getPowerDeficiency());
            Assert.assertEquals(BigInteger.valueOf(-500), ptuSettlementDto.getDeliveredFlexPower());
        });
    }

    private WorkflowContext buildWorkflowContextWithGridMonitoring() {
        WorkflowContext context = new DefaultWorkflowContext();
        context.setValue(IN.PTU_DURATION.name(), PTU_DURATION);
        context.setValue(IN.START_DATE.name(), START_DATE);
        context.setValue(IN.END_DATE.name(), END_DATE);
        context.setValue(IN.FLEX_ORDER_DTO_LIST.name(), buildFlexOrderDtos());
        context.setValue(IN.PROGNOSIS_DTO_LIST.name(), buildPrognosisDtos());
        context.setValue(DsoInitiateSettlementParameter.IN.GRID_MONITORING_DATA.name(), buildGridMonitoringData());
        return context;
    }

    private List<FlexOrderDto> buildFlexOrderDtos() {
        FlexOrderDto order1 = buildFlexOrder(4L, CONGESTION_POINT, AGR1_DOMAIN, START_DATE);
        FlexOrderDto order2 = buildFlexOrder(5L, CONGESTION_POINT, AGR2_DOMAIN, START_DATE);
        return Arrays.asList(order1, order2);
    }

    private FlexOrderDto buildFlexOrder(Long sequence, String congestionPoint, String agrDomain, LocalDate period) {
        FlexOrderDto flexOrderDto = new FlexOrderDto();
        flexOrderDto.setSequenceNumber(sequence);
        flexOrderDto.setConnectionGroupEntityAddress(congestionPoint);
        flexOrderDto.setParticipantDomain(agrDomain);
        flexOrderDto.setPeriod(period);
        IntStream.rangeClosed(1, PTUS_PER_DAY)
                .mapToObj(index -> new PtuFlexOrderDto(BigInteger.valueOf(index), BigInteger.valueOf(-1000L), BigDecimal.TEN))
                .forEach(ptu -> flexOrderDto.getPtus().add(ptu));
        return flexOrderDto;
    }

    private List<PrognosisDto> buildPrognosisDtos() {
        PrognosisDto prognosis1 = buildPrognosis(1L, CONGESTION_POINT, AGR1_DOMAIN, START_DATE);
        PrognosisDto prognosis2 = buildPrognosis(2L, CONGESTION_POINT, AGR2_DOMAIN, START_DATE);
        return Arrays.asList(prognosis1, prognosis2);
    }

    private PrognosisDto buildPrognosis(Long sequence, String congestionPoint, String agrDomain, LocalDate period) {
        PrognosisDto prognosisDto = new PrognosisDto();
        prognosisDto.setPeriod(period);
        prognosisDto.setConnectionGroupEntityAddress(congestionPoint);
        prognosisDto.setParticipantDomain(agrDomain);
        prognosisDto.setSequenceNumber(sequence);
        IntStream.rangeClosed(1, PTUS_PER_DAY).mapToObj(index -> {
            PtuPrognosisDto ptuPrognosisDto = new PtuPrognosisDto();
            ptuPrognosisDto.setPtuIndex(BigInteger.valueOf(index));
            ptuPrognosisDto.setPower(BigInteger.valueOf(1000));
            return ptuPrognosisDto;
        }).forEach(ptu -> prognosisDto.getPtus().add(ptu));
        return prognosisDto;
    }

    private List<MeterDataSetDto> buildSmartMeterData() {
        MeterDataSetDto meterDataSetDto = new MeterDataSetDto(CONGESTION_POINT);
        MeterDataDto meterDataDto = new MeterDataDto();
        meterDataDto.setPeriod(START_DATE);
        ConnectionMeterDataDto connectionMeterDataDto1 = buildConnectionMeterData(AGR1_DOMAIN, 3);
        ConnectionMeterDataDto connectionMeterDataDto2 = buildConnectionMeterData(AGR2_DOMAIN, 1);
        meterDataDto.getConnectionMeterDataDtos().add(connectionMeterDataDto1);
        meterDataDto.getConnectionMeterDataDtos().add(connectionMeterDataDto2);
        meterDataSetDto.getMeterDataDtos().add(meterDataDto);
        return Collections.singletonList(meterDataSetDto);
    }

    private ConnectionMeterDataDto buildConnectionMeterData(String agrDomain, Integer entityCount) {
        ConnectionMeterDataDto connectionMeterDataDto = new ConnectionMeterDataDto();
        connectionMeterDataDto.setAgrDomain(agrDomain);
        connectionMeterDataDto.setEntityCount(BigInteger.valueOf(entityCount));
        IntStream.rangeClosed(1, PTUS_PER_DAY).mapToObj(index -> {
            PtuMeterDataDto ptuMeterDataDto = new PtuMeterDataDto(index);
            ptuMeterDataDto.setPower(BigInteger.valueOf(1500));
            return ptuMeterDataDto;
        }).forEach(ptu -> connectionMeterDataDto.getPtuMeterDataDtos().add(ptu));
        return connectionMeterDataDto;
    }

    private List<GridMonitoringDto> buildGridMonitoringData() {
        return Collections.singletonList(buildGridMonitoring());
    }

    private GridMonitoringDto buildGridMonitoring() {
        GridMonitoringDto gridMonitoringDto = new GridMonitoringDto(CONGESTION_POINT, START_DATE);
        gridMonitoringDto.getConnectionCountPerAggregator().put(AGR1_DOMAIN, 3);
        gridMonitoringDto.getConnectionCountPerAggregator().put(AGR2_DOMAIN, 1);
        IntStream.rangeClosed(1, PTUS_PER_DAY).mapToObj(index -> {
            PtuGridMonitoringDto ptuGridMonitoringDto = new PtuGridMonitoringDto();
            ptuGridMonitoringDto.setPtuIndex(index);
            ptuGridMonitoringDto.setActualPower(BigInteger.valueOf(2000));
            return ptuGridMonitoringDto;
        }).forEach(ptu -> gridMonitoringDto.getPtuGridMonitoringDtos().add(ptu));
        return gridMonitoringDto;
    }
}
