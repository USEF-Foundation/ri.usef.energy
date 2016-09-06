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
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.dto.UdiPortfolioDto;
import energy.usef.agr.model.ConnectionForecastSnapshot;
import energy.usef.agr.repository.ConnectionForecastSnapshotRepository;
import energy.usef.agr.workflow.operate.identifychangeforecast.IdentifyChangeInForecastStepParameter;
import energy.usef.core.dto.PtuContainerDto;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.PrognosisDto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Unit test to test the AgrIdentifyChangeInForecastStub.
 */
@RunWith(PowerMockRunner.class)
public class AgrIdentifyChangeInForecastStubTest {

    public static final String ENTITY_ADDRESS_1 = "ean.000000000001";
    public static final String ENTITY_ADDRESS_2 = "ean.000000000002";
    public static final int DTU_SIZE = 360;
    public static final int PTU_SIZE = 360;
    public static final LocalDate DAY1 = new LocalDate(2015, 1, 1);
    public static final LocalDate DAY2 = new LocalDate(2015, 1, 2);
    private AgrIdentifyChangeInForecastStub agrIdentifyChangeInForecastStub;
    @Mock
    private ConnectionForecastSnapshotRepository connectionForecastSnapshotRepository;

    @Before
    public void setUp() {
        agrIdentifyChangeInForecastStub = new AgrIdentifyChangeInForecastStub();
        Whitebox.setInternalState(agrIdentifyChangeInForecastStub, connectionForecastSnapshotRepository);

        PowerMockito.when(connectionForecastSnapshotRepository.findConnectionForecastSnapshots(Matchers.eq(DAY1)))
                .then(call -> buildConnectionForecastSnapshotListForDay1());
        PowerMockito.when(connectionForecastSnapshotRepository.findConnectionForecastSnapshots(Matchers.eq(DAY2)))
                .then(call -> buildConnectionForecastSnapshotListForDay2());
    }

    /**
     * This test will build a portfolio with 2 connections. Connection ean.000000000001 will have forecasts for two days
     * (2015-01-01, 2015-01-02), while connection ean.000000000002 will have forecasts for the first date only.
     * <p>
     * The portoflio
     */
    @SuppressWarnings("unchecked")
    @Test
    public void invokeTestWithNotEmptyConnectionPortfolioForDay1() {
        WorkflowContext inContext = new DefaultWorkflowContext();
        List<ConnectionPortfolioDto> connectionPortfolioDto = buildConnectionPortfolioDtoForDay1();
        inContext.setValue(IdentifyChangeInForecastStepParameter.IN.CONNECTION_PORTFOLIO.name(), connectionPortfolioDto);
        inContext.setValue(IdentifyChangeInForecastStepParameter.IN.LATEST_A_PLANS_DTO_LIST.name(),
                Collections.singletonList(new PrognosisDto()));
        inContext.setValue(IdentifyChangeInForecastStepParameter.IN.PTU_DURATION.name(), PTU_SIZE);
        inContext.setValue(IdentifyChangeInForecastStepParameter.IN.PERIOD.name(), DAY1);

        WorkflowContext outContext = agrIdentifyChangeInForecastStub.invoke(inContext);
        Assert.assertNotNull(outContext.getValue(IdentifyChangeInForecastStepParameter.OUT.FORECAST_CHANGED.name()));

        ArgumentCaptor<List> snapshotsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(connectionForecastSnapshotRepository, Mockito.times(1))
                .serializeConnectionForecastSnapshots(snapshotsCaptor.capture());
        List<ConnectionForecastSnapshot> capturedList = snapshotsCaptor.getValue();
        Assert.assertNotNull(capturedList);
        Assert.assertEquals(8, capturedList.size());
        // verify that snapshot is flagged as changed for connection 1 on day 1
        capturedList.stream()
                .filter(snapshot -> snapshot.getConnectionEntityAddress().equals("ean.000000000001"))
                .filter(snapshot -> snapshot.getPtuDate().equals(new LocalDate(2015, 1, 1)))
                .forEach(snapshot -> Assert.assertTrue(snapshot.getChanged()));
        // verify that snapshots are not flagged as changed for connection 2 on day 2
        capturedList.stream()
                .filter(snapshot -> !snapshot.getConnectionEntityAddress().equals("ean.000000000001") || !snapshot.getPtuDate()
                        .equals(new LocalDate(2015, 1, 1)))
                .forEach(snapshot -> Assert.assertFalse(snapshot.getChanged()));
        // verify that the global flag is 'changed'
        Assert.assertTrue((Boolean) outContext.getValue(IdentifyChangeInForecastStepParameter.OUT.FORECAST_CHANGED.name()));
        Assert.assertNotNull(outContext.getValue(IdentifyChangeInForecastStepParameter.OUT.FORECAST_CHANGED_PTU_CONTAINER_DTO_LIST.name()));
        Assert.assertFalse(
                ((List<PtuContainerDto>) outContext.getValue(IdentifyChangeInForecastStepParameter.OUT.FORECAST_CHANGED_PTU_CONTAINER_DTO_LIST.name())).isEmpty());
    }

    /**
     * This test will build a portfolio with 1 connections. Connection ean.000000000001 will 2015-01-02.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void invokeTestWithNotEmptyConnectionPortfolioForDay2() {
        WorkflowContext inContext = new DefaultWorkflowContext();
        List<ConnectionPortfolioDto> connectionPortfolioDto = buildConnectionPortfolioDtoForDay2();
        inContext.setValue(IdentifyChangeInForecastStepParameter.IN.CONNECTION_PORTFOLIO.name(), connectionPortfolioDto);
        inContext.setValue(IdentifyChangeInForecastStepParameter.IN.LATEST_A_PLANS_DTO_LIST.name(),
                Collections.singletonList(new PrognosisDto()));
        inContext.setValue(IdentifyChangeInForecastStepParameter.IN.PTU_DURATION.name(), PTU_SIZE);
        inContext.setValue(IdentifyChangeInForecastStepParameter.IN.PERIOD.name(), DAY2);

        WorkflowContext outContext = agrIdentifyChangeInForecastStub.invoke(inContext);
        Assert.assertNotNull(outContext.getValue(IdentifyChangeInForecastStepParameter.OUT.FORECAST_CHANGED.name()));

        ArgumentCaptor<List> snapshotsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(connectionForecastSnapshotRepository, Mockito.times(1))
                .serializeConnectionForecastSnapshots(snapshotsCaptor.capture());
        List<ConnectionForecastSnapshot> capturedList = snapshotsCaptor.getValue();
        Assert.assertNotNull(capturedList);
        Assert.assertEquals(4, capturedList.size());
        // verify that snapshots are not flagged as changed for connection 2 on day 2
        capturedList.stream()
                .filter(snapshot -> !snapshot.getConnectionEntityAddress().equals("ean.000000000001") || !snapshot.getPtuDate()
                        .equals(new LocalDate(2015, 1, 1)))
                .forEach(snapshot -> Assert.assertFalse(snapshot.getChanged()));
        // verify that the global flag is 'changed'
        Assert.assertFalse((Boolean) outContext.getValue(IdentifyChangeInForecastStepParameter.OUT.FORECAST_CHANGED.name()));
        Assert.assertNotNull(outContext.getValue(IdentifyChangeInForecastStepParameter.OUT.FORECAST_CHANGED_PTU_CONTAINER_DTO_LIST.name()));
        Assert.assertTrue(
                ((List<PtuContainerDto>) outContext.getValue(IdentifyChangeInForecastStepParameter.OUT.FORECAST_CHANGED_PTU_CONTAINER_DTO_LIST.name())).isEmpty());
    }

    @Test
    public void invokeTestWithEmptyConnectionPortfolio() {
        WorkflowContext inContext = new DefaultWorkflowContext();

        inContext.setValue(IdentifyChangeInForecastStepParameter.IN.CONNECTION_PORTFOLIO.name(), new ArrayList<>());

        List<PrognosisDto> aPlanDtos = new ArrayList<>();
        aPlanDtos.add(new PrognosisDto());
        inContext.setValue(IdentifyChangeInForecastStepParameter.IN.LATEST_A_PLANS_DTO_LIST.name(), aPlanDtos);

        WorkflowContext outContext = agrIdentifyChangeInForecastStub.invoke(inContext);
        Assert.assertFalse((boolean) outContext.getValue(IdentifyChangeInForecastStepParameter.OUT.FORECAST_CHANGED.name()));
    }

    private List<ConnectionPortfolioDto> buildConnectionPortfolioDtoForDay1() {
        ConnectionPortfolioDto connectionDto1OnDay1 = new ConnectionPortfolioDto(ENTITY_ADDRESS_1);
        ConnectionPortfolioDto connectionDto2OnDay1 = new ConnectionPortfolioDto(ENTITY_ADDRESS_2);

        IntStream.rangeClosed(1, 4).mapToObj(index -> {
            PowerContainerDto ptuDto = new PowerContainerDto(DAY1, index);
            ptuDto.getForecast().setAverageConsumption(BigInteger.TEN);
            return ptuDto;
        }).forEach(ptu -> connectionDto1OnDay1.getConnectionPowerPerPTU().put(ptu.getTimeIndex(), ptu));
        IntStream.rangeClosed(1, 4).mapToObj(index -> {
            PowerContainerDto ptuDto = new PowerContainerDto(DAY1, index);
            ptuDto.getForecast().setAverageConsumption(BigInteger.ONE);
            return ptuDto;
        }).forEach(ptu -> connectionDto2OnDay1.getConnectionPowerPerPTU().put(ptu.getTimeIndex(), ptu));

        UdiPortfolioDto udi1 = new UdiPortfolioDto(null, DTU_SIZE, null);
        IntStream.rangeClosed(1, 4).mapToObj(dtuIndex -> {
            PowerContainerDto dtu = new PowerContainerDto(DAY1, dtuIndex);
            dtu.getForecast().setAverageConsumption(BigInteger.valueOf(5));
            return dtu;
        }).forEach(dtu -> udi1.getUdiPowerPerDTU().put(dtu.getTimeIndex(), dtu));
        UdiPortfolioDto udi2 = new UdiPortfolioDto(null, DTU_SIZE, null);
        IntStream.rangeClosed(1, 4).mapToObj(dtuIndex -> {
            PowerContainerDto dtu = new PowerContainerDto(DAY1, dtuIndex);
            dtu.getForecast().setAverageConsumption(BigInteger.valueOf(5));
            return dtu;
        }).forEach(dtu -> udi2.getUdiPowerPerDTU().put(dtu.getTimeIndex(), dtu));
        connectionDto1OnDay1.getUdis().add(udi1);
        connectionDto2OnDay1.getUdis().add(udi2);

        List<ConnectionPortfolioDto> result = new ArrayList<>();
        result.add(connectionDto1OnDay1);
        result.add(connectionDto2OnDay1);

        return result;
    }

    private List<ConnectionPortfolioDto> buildConnectionPortfolioDtoForDay2() {
        ConnectionPortfolioDto connectionDto1OnDay2 = new ConnectionPortfolioDto(ENTITY_ADDRESS_1);

        IntStream.rangeClosed(1, 4).mapToObj(index -> {
            PowerContainerDto ptuDto = new PowerContainerDto(DAY2, index);
            ptuDto.getForecast().setAverageConsumption(BigInteger.ONE);
            return ptuDto;
        }).forEach(ptu -> connectionDto1OnDay2.getConnectionPowerPerPTU().put(ptu.getTimeIndex(), ptu));
        UdiPortfolioDto udi3 = new UdiPortfolioDto(null, DTU_SIZE, null);
        IntStream.rangeClosed(1, 4).mapToObj(dtuIndex -> {
            PowerContainerDto dtu = new PowerContainerDto(DAY2, dtuIndex);
            dtu.getForecast().setAverageConsumption(BigInteger.valueOf(5));
            return dtu;
        }).forEach(dtu -> udi3.getUdiPowerPerDTU().put(dtu.getTimeIndex(), dtu));
        connectionDto1OnDay2.getUdis().add(udi3);

        return Collections.singletonList(connectionDto1OnDay2);
    }

    private List<ConnectionForecastSnapshot> buildConnectionForecastSnapshotListForDay1() {
        List<ConnectionForecastSnapshot> snapshots = IntStream.rangeClosed(1, 4).mapToObj(index -> {
            ConnectionForecastSnapshot snapshot = new ConnectionForecastSnapshot();
            snapshot.setPtuDate(DAY1);
            snapshot.setPtuIndex(index);
            snapshot.setConnectionEntityAddress(ENTITY_ADDRESS_1);
            snapshot.setPower(BigInteger.TEN);
            return snapshot;
        }).collect(Collectors.toList());
        IntStream.rangeClosed(1, 4).mapToObj(index -> {
            ConnectionForecastSnapshot snapshot = new ConnectionForecastSnapshot();
            snapshot.setPtuDate(DAY1);
            snapshot.setPtuIndex(index);
            snapshot.setConnectionEntityAddress(ENTITY_ADDRESS_2);
            snapshot.setPower(BigInteger.valueOf(6));
            return snapshot;
        }).forEach(snapshots::add);
        return snapshots;
    }

    private List<ConnectionForecastSnapshot> buildConnectionForecastSnapshotListForDay2() {
        return IntStream.rangeClosed(1, 4).mapToObj(index -> {
            ConnectionForecastSnapshot snapshot = new ConnectionForecastSnapshot();
            snapshot.setPtuDate(DAY2);
            snapshot.setPtuIndex(index);
            snapshot.setConnectionEntityAddress(ENTITY_ADDRESS_1);
            snapshot.setPower(BigInteger.valueOf(6));
            return snapshot;
        }).collect(Collectors.toList());
    }

}
