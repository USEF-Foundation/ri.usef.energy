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

import static java.util.stream.Collectors.groupingBy;

import energy.usef.agr.dto.ConnectionPortfolioDto;
import energy.usef.agr.dto.PowerContainerDto;
import energy.usef.agr.dto.UdiPortfolioDto;
import energy.usef.agr.model.ConnectionForecastSnapshot;
import energy.usef.agr.repository.ConnectionForecastSnapshotRepository;
import energy.usef.agr.workflow.operate.identifychangeforecast.IdentifyChangeInForecastStepParameter.IN;
import energy.usef.agr.workflow.operate.identifychangeforecast.IdentifyChangeInForecastStepParameter.OUT;
import energy.usef.core.dto.PtuContainerDto;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.PtuUtil;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.WorkflowStep;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub implementation of the PBC which is in charge of deciding whether the connection forecast is changed.
 * <p>
 * The PBC receives the following parameters as input to take the decision:
 * <ul>
 * <li>CONNECTION_PORTFOLIO : a Map of List {@link ConnectionPortfolioDto} per period.</li>
 * <li>PERIOD: the period for which changes in forecast are being identified.</li>
 * <li>PTU_DURATION : the size of a PTU in minutes.</li>
 * </ul>
 * <p>
 * The PBC must return one flag indicating whether Re-optimize portfolio workflow should be triggered. The output parameter:
 * <ul>
 * <li>FORECAST_CHANGED : boolean value indicating that the Re-optimize portfolio workflow should be triggered</li>
 * </ul>
 * <p>
 * The step implementation of this PBC will simply flip a coin to determine whether to return false or true. Probability is 50%. If
 * the Connection Portfolio or A-Plan list is empty the output flag is FALSE.
 */
public class AgrIdentifyChangeInForecastStub implements WorkflowStep {

    private static final int PRECISION = 2;
    private static final Logger LOGGER = LoggerFactory.getLogger(AgrIdentifyChangeInForecastStub.class);
    private static final BigDecimal POWER_CHANGE_PERCENTAGE = new BigDecimal("1");
    private static final BigDecimal GLOBAL_POWER_CHANGE_PERCENTAGE = new BigDecimal("5");
    @Inject
    private ConnectionForecastSnapshotRepository connectionForecastSnapshotRepository;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public WorkflowContext invoke(WorkflowContext context) {
        LOGGER.debug("Invoking PBC 'AGRIdentifyChangeInForecast'.");
        List<ConnectionPortfolioDto> connectionPortfolioDto = (List<ConnectionPortfolioDto>) context.getValue(
                IN.CONNECTION_PORTFOLIO.name());

        Integer ptuDuration = context.get(IN.PTU_DURATION.name(), Integer.class);
        LocalDate period = context.get(IN.PERIOD.name(), LocalDate.class);

        if (connectionPortfolioDto.isEmpty()) {
            context.setValue(OUT.FORECAST_CHANGED.name(), false);
            context.setValue(OUT.FORECAST_CHANGED_PTU_CONTAINER_DTO_LIST.name(), new ArrayList<PtuContainerDto>());
            return context;
        }
        Map<Integer, Map<String, ConnectionForecastSnapshot>> previousConnecionForecastSnapshots =
                findPreviousConnectionForecastSnapshots(period);
        Map<Integer, Map<String, ConnectionForecastSnapshot>> incomingConnectionForecastSnaphots =
                transformPortolioToSnapshots(connectionPortfolioDto, period, ptuDuration);
        boolean forecastChanged = haveForecastsChanged(previousConnecionForecastSnapshots, incomingConnectionForecastSnaphots,
                period, ptuDuration);
        context.setValue(OUT.FORECAST_CHANGED.name(), forecastChanged);

        int currentPtuIndex = PtuUtil.getPtuIndex(DateTimeUtil.getCurrentDateTime(), ptuDuration);
        LocalDate today = DateTimeUtil.getCurrentDate();
        // save the new snapshots
        connectionForecastSnapshotRepository.serializeConnectionForecastSnapshots(incomingConnectionForecastSnaphots.values()
                .stream()
                .flatMap(addressToSnapshotMap -> addressToSnapshotMap.values()
                        .stream()
                        .filter(connectionForecastSnapshot -> !(connectionForecastSnapshot.getPtuDate().isEqual(today)
                                && connectionForecastSnapshot.getPtuIndex() < currentPtuIndex)))
                .collect(Collectors.toList()));

        // per snapshot having changed forecasts, return the related ptu in the context with that flag.
        List<PtuContainerDto> changedPtus = incomingConnectionForecastSnaphots.values().stream()
                .flatMap(ptuToAddressToSnapshotMap -> ptuToAddressToSnapshotMap.values().stream())
                .filter(snapshot -> snapshot.getChanged() == Boolean.TRUE)
                .map(snapshot -> new PtuContainerDto(snapshot.getPtuDate(), snapshot.getPtuIndex()))
                .collect(Collectors.toList());
        context.setValue(OUT.FORECAST_CHANGED_PTU_CONTAINER_DTO_LIST.name(), changedPtus);
        return context;
    }

    private boolean haveForecastsChanged(
            Map<Integer, Map<String, ConnectionForecastSnapshot>> previousConnecionForecastSnapshots,
            Map<Integer, Map<String, ConnectionForecastSnapshot>> incomingConnectionForecastSnaphots, LocalDate period,
            int ptuDuration) {
        BigInteger changedPower = BigInteger.ZERO;
        BigInteger totalPower = BigInteger.ZERO;
        final int currentPtuIndex = PtuUtil.getPtuIndex(DateTimeUtil.getCurrentDateTime(), ptuDuration);
        final boolean processingPeriodIsToday = DateTimeUtil.getCurrentDate().isEqual(period);

        for (Integer ptuIndex : incomingConnectionForecastSnaphots.keySet()) {
            if ((processingPeriodIsToday && ptuIndex < currentPtuIndex) || !previousConnecionForecastSnapshots.containsKey(
                    ptuIndex)) {
                continue;
            }
            for (String connectionEntityAddress : incomingConnectionForecastSnaphots.get(ptuIndex).keySet()) {
                if (!previousConnecionForecastSnapshots.get(ptuIndex).containsKey(connectionEntityAddress)) {
                    continue;
                }
                ConnectionForecastSnapshot incomingSnapshot = incomingConnectionForecastSnaphots.get(ptuIndex)
                        .get(connectionEntityAddress);

                ConnectionForecastSnapshot previousSnapshot = previousConnecionForecastSnapshots.get(ptuIndex)
                        .get(connectionEntityAddress);

                if (!comparePowerWithinThreshold(incomingSnapshot.getPower(), previousSnapshot.getPower())) {
                    incomingSnapshot.setChanged(true);
                    changedPower = changedPower.add(incomingSnapshot.getPower());
                }
                totalPower = totalPower.add(incomingSnapshot.getPower());
            }
        }
        return isChangedPowerTooDifferent(changedPower, totalPower);
    }

    /**
     * Transforms the Connection Porfolio to a map structure of ConnectionForecastSnapshot.
     * The structure is as follows:
     * <ol>
     * <li>Index of the PTU ({@link Integer})</li>
     * <li>Entity Address of the connection ({@link String}) coupled with {@link ConnectionForecastSnapshot}</li>
     * </ol>
     *
     * @param connectionPortfolio the list of {@link ConnectionPortfolioDto} for the period.
     * @param period {@link LocalDate} the period for which the portfolio will be transformed to a snaphshot.
     * @param ptuDuration {@link Integer} the duration of PTU in minutes.
     * @return a {@link Map} with the {@link ConnectionForecastSnapshot} per Connection entity address per PTU index.
     */
    private Map<Integer, Map<String, ConnectionForecastSnapshot>> transformPortolioToSnapshots(
            List<ConnectionPortfolioDto> connectionPortfolio, LocalDate period, Integer ptuDuration) {
        Map<Integer, Map<String, ConnectionForecastSnapshot>> resultMap = new HashMap<>();
        for (ConnectionPortfolioDto connectionPortfolioDto : connectionPortfolio) {
            String connectionEntityAddress = connectionPortfolioDto.getConnectionEntityAddress();
            populateSnapshotsAtConnectionLevel(resultMap, connectionPortfolioDto, period);
            for (UdiPortfolioDto udiPortfolioDto : connectionPortfolioDto.getUdis()) {
                populateSnapshotsAtUdiLevel(resultMap, udiPortfolioDto, connectionEntityAddress, period, ptuDuration);
            }
        }
        return resultMap;
    }

    private void populateSnapshotsAtConnectionLevel(
            Map<Integer, Map<String, ConnectionForecastSnapshot>> snapshotsPerAddressPerPtu,
            ConnectionPortfolioDto connectionPortfolioDto, LocalDate period) {
        String connectionEntityAddress = connectionPortfolioDto.getConnectionEntityAddress();
        for (PowerContainerDto powerContainer : connectionPortfolioDto.getConnectionPowerPerPTU().values()) {
            Map<String, ConnectionForecastSnapshot> addressToSnapshotMap = snapshotsPerAddressPerPtu.getOrDefault(
                    powerContainer.getTimeIndex(), new HashMap<>());
            addressToSnapshotMap.put(connectionEntityAddress,
                    buildConnectionForecastSnapshot(period, powerContainer, connectionEntityAddress));
            snapshotsPerAddressPerPtu.put(powerContainer.getTimeIndex(), addressToSnapshotMap);
        }
    }

    private void populateSnapshotsAtUdiLevel(Map<Integer, Map<String, ConnectionForecastSnapshot>> snapshotsPerAddressPerPtu,
            UdiPortfolioDto udiPortfolioDto, String connectionEntityAddress, LocalDate period, int ptuDuration) {
        Integer dtusPerPtu = ptuDuration / udiPortfolioDto.getDtuSize();
        // group DTU power container per PTU index
        Map<Integer, List<PowerContainerDto>> dtuPowerPerPtuMap = udiPortfolioDto.getUdiPowerPerDTU().values()
                .stream().collect(Collectors.groupingBy(powerContainer -> (powerContainer.getTimeIndex() - 1) / dtusPerPtu + 1));

        for (Integer ptuIndex : dtuPowerPerPtuMap.keySet()) {
            if (snapshotsPerAddressPerPtu.get(ptuIndex) == null) {
                snapshotsPerAddressPerPtu.put(ptuIndex, new HashMap<>());
            }
            Map<String, ConnectionForecastSnapshot> snapshotPerAddress = snapshotsPerAddressPerPtu.get(ptuIndex);
            OptionalDouble powerAverage = computeUdiAveragePowerForPtu(dtuPowerPerPtuMap, ptuIndex);
            if (!powerAverage.isPresent()) {
                continue;
            }
            ConnectionForecastSnapshot dtuConnectionForecastSnapshot = buildConnectionForecastSnapshot(
                    powerAverage.getAsDouble(), period, connectionEntityAddress, ptuIndex);
            if (snapshotPerAddress.get(connectionEntityAddress) == null) {
                snapshotPerAddress.put(connectionEntityAddress, dtuConnectionForecastSnapshot);
            } else {
                mergeConnectionForecastSnapshots(snapshotPerAddress, dtuConnectionForecastSnapshot);
            }
        }
    }

    private OptionalDouble computeUdiAveragePowerForPtu(Map<Integer, List<PowerContainerDto>> dtuPowerPerPtuMap, Integer ptuIndex) {
        return dtuPowerPerPtuMap.get(ptuIndex)
                .stream()
                .filter(powerContainer -> powerContainer.getForecast() != null)
                .mapToInt(powerContainer -> powerContainer.getForecast().calculatePower().intValue())
                .average();
    }

    private void mergeConnectionForecastSnapshots(Map<String, ConnectionForecastSnapshot> addressToPowerMap,
            ConnectionForecastSnapshot dtuConnectionForecastSnapshot) {
        ConnectionForecastSnapshot presentSnapshot = addressToPowerMap.get(
                dtuConnectionForecastSnapshot.getConnectionEntityAddress());
        if (presentSnapshot.getPower() == null) {
            presentSnapshot.setPower(dtuConnectionForecastSnapshot.getPower());
        } else if (dtuConnectionForecastSnapshot.getPower() != null) {
            presentSnapshot.setPower(presentSnapshot.getPower().add(dtuConnectionForecastSnapshot.getPower()));
        }
    }

    /**
     * Builds a new ConnectionForecastSnapshot from a PtuDto (containing the uncontrolled load information).
     *
     * @param period {@link LocalDate} the period.
     * @param powerContainer {@link PowerContainerDto} the actual powerContainer.
     * @param connectionEntityAddress {@link String} connection entity address
     * @return a {@link ConnectionForecastSnapshot}.
     */
    private ConnectionForecastSnapshot buildConnectionForecastSnapshot(LocalDate period, PowerContainerDto powerContainer,
            String connectionEntityAddress) {
        ConnectionForecastSnapshot snapshot = new ConnectionForecastSnapshot();
        snapshot.setPower(powerContainer.getForecast().calculatePower());
        snapshot.setPtuIndex(powerContainer.getTimeIndex());
        snapshot.setPtuDate(period);
        snapshot.setConnectionEntityAddress(connectionEntityAddress);
        snapshot.setChanged(false);
        return snapshot;
    }

    /**
     * Builds a new ConnectionForecastSnapshot from a DtuDto (containing the controlled load information for a device).
     *
     * @param averagePower {@link Double} average power of the multiple dtus of a Ptu.
     * @param period {@link LocalDate} period of the dtus.
     * @param connectionEntityAddress {@link String} connection entity address
     * @param index {@link Integer} index of the related ptu (depending on the dtu size).
     * @return a {@link ConnectionForecastSnapshot}.
     */
    private ConnectionForecastSnapshot buildConnectionForecastSnapshot(Double averagePower, LocalDate period,
            String connectionEntityAddress, int index) {
        ConnectionForecastSnapshot snapshot = new ConnectionForecastSnapshot();
        snapshot.setPower(BigInteger.valueOf(averagePower.longValue()));
        snapshot.setPtuIndex(index);
        snapshot.setPtuDate(period);
        snapshot.setConnectionEntityAddress(connectionEntityAddress);
        snapshot.setChanged(false);
        return snapshot;
    }

    /**
     * Finds the previous ConnectionForecastSnapshot in from the database and partition them in a map structure.
     * The a map structure is as follows:
     * <ol>
     * <li>Index of the PTU ({@link Integer})</li>
     * <li>Entity Address of the connection ({@link String}) coupled with {@link ConnectionForecastSnapshot}</li>
     * </ol>
     *
     * @return a {@link Map} with the aforementioned structure.
     */
    private Map<Integer, Map<String, ConnectionForecastSnapshot>> findPreviousConnectionForecastSnapshots(LocalDate period) {
        List<ConnectionForecastSnapshot> connectionForecastSnapshots = connectionForecastSnapshotRepository
                .findConnectionForecastSnapshots(period);
        // NB: when reducing, there should be only one snapshot anyway (so the code there is valid).
        return connectionForecastSnapshots.stream()
                .collect(groupingBy(ConnectionForecastSnapshot::getPtuIndex,
                        groupingBy(ConnectionForecastSnapshot::getConnectionEntityAddress,
                                Collectors.reducing(null, (snapshot1, snapshot2) -> snapshot2))));
    }

    /**
     * Compare the incoming power to the previous power and returns <code>true</code> if the difference between both is bigger than
     * POWER_CHANGE_PERCENTAGE. Formula is the following: <br />
     * <br />
     * <code>
     * |(incomingPower - previousPower) / previousPower| >? POWER_CHANGE_PERCENTAGE
     * </code> <br/>
     *
     * @param incomingPower {@link BigInteger}
     * @param previousPower {@link BigInteger}
     * @return {@link Boolean}
     */
    private boolean comparePowerWithinThreshold(BigInteger incomingPower, BigInteger previousPower) {
        LOGGER.trace("Comparing incoming power [{}] against previous power [{}] (max. difference percentage = [{}]%)",
                incomingPower, previousPower, POWER_CHANGE_PERCENTAGE);
        if (incomingPower == null || previousPower == null) {
            return true;
        }
        if (BigInteger.ZERO.equals(previousPower)) {
            return BigInteger.ZERO.equals(incomingPower);
        }
        return new BigDecimal(incomingPower.subtract(previousPower)).divide(new BigDecimal(previousPower), MathContext.DECIMAL64)
                .abs()
                .setScale(PRECISION, RoundingMode.HALF_UP)
                .compareTo(POWER_CHANGE_PERCENTAGE.divide(BigDecimal.valueOf(100))) != 1;
    }

    /**
     * Compare the total power to the changed power and returns <code>true</code> if the changed power is bigger than
     * GLOBAL_POWER_CHANGE_PERCENTAGE of the total power. Formula is the following: <br />
     * <br />
     * <code>
     * |changedPower / totalPower| >? GLOBAL_POWER_CHANGE_PERCENTAGE
     * </code> <br/>
     *
     * @param changedPower {@link BigInteger}
     * @param totalPower {@link BigInteger}
     * @return {@link Boolean}
     */
    private boolean isChangedPowerTooDifferent(BigInteger changedPower, BigInteger totalPower) {
        LOGGER.trace("Comparing changed power [{}] against total power [{}] (max. difference percentage = [{}]%)", changedPower,
                totalPower, GLOBAL_POWER_CHANGE_PERCENTAGE);
        if (BigInteger.ZERO.equals(totalPower)) {
            return !BigInteger.ZERO.equals(changedPower);
        }
        return new BigDecimal(changedPower).divide(new BigDecimal(totalPower), MathContext.DECIMAL64)
                .abs()
                .setScale(PRECISION, RoundingMode.HALF_UP)
                .compareTo(GLOBAL_POWER_CHANGE_PERCENTAGE.divide(BigDecimal.valueOf(100))) == 1;
    }
}
