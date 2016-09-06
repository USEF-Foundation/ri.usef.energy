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

package energy.usef.agr.repository;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.model.ConnectionForecastSnapshot;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository class in charge of the 'database' operations related to the {@link ConnectionForecastSnapshot} entities. The
 * serialization format behind this repository might not be a database (can be JSON file).
 */
@Stateless
public class ConnectionForecastSnapshotRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionForecastSnapshotRepository.class);
    private static final String YYYYMMDD = "yyyyMMdd";
    private static final String AGR_IDENTIFY_CHANGE_IN_FORECAST_SERIALIZATION_FILENAME = "connection_forecast_snapshots.json";

    @Inject
    private ConfigAgr configAgr;

    /**
     * Finds the {@link ConnectionForecastSnapshot} for the given connection and the specified ptu date and ptu index.
     *
     * @param connectionEntityAddress {@link String} entity address of the connection.
     * @param ptuDate {@link LocalDate} period of the related ptu.
     * @param ptuIndex {@link Integer} index of the related ptu.
     * @return a {@link ConnectionForecastSnapshot} or <code>null</code>.
     */
    public ConnectionForecastSnapshot findConnectionForecastSnapshot(String connectionEntityAddress, LocalDate ptuDate,
            Integer ptuIndex) {
        return deserializeConnectionForecastSnapshots(ptuDate).stream()
                .filter(snapshot -> snapshot.getConnectionEntityAddress().equals(connectionEntityAddress))
                .filter(snapshot -> snapshot.getPtuIndex().equals(ptuIndex))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds all the connection forecast snapshots and order them by PTU_DATE, PTU_INDEX and then CONNECTION_ENTITY_ADDRESS.
     *
     * @param period  {@link LocalDate} the period of the snapshots
     * @return a {@link List} of {@link ConnectionForecastSnapshot} entities
     */
    public List<ConnectionForecastSnapshot> findConnectionForecastSnapshots(LocalDate period) {
        return deserializeConnectionForecastSnapshots(period).stream().collect(Collectors.toList());
    }

    /**
     * Serialize a collection of ConnectionForecastSnaphosts. This method will erase all the existing content in the file specified
     * in the configuration.
     *
     * @param connectionForecastSnapshots {@link Collection} of {@link ConnectionForecastSnapshot}.
     */
    public void serializeConnectionForecastSnapshots(Collection<ConnectionForecastSnapshot> connectionForecastSnapshots) {
        if (connectionForecastSnapshots == null || connectionForecastSnapshots.isEmpty()) {
            return;
        }
        Map<LocalDate, List<ConnectionForecastSnapshot>> connectionForecastSnapshotsPerPeriod = connectionForecastSnapshots.stream()
                .collect(Collectors.groupingBy(ConnectionForecastSnapshot::getPtuDate));

        connectionForecastSnapshotsPerPeriod.forEach(this::serializeConnectionForecastSnapshotsForPeriod);
    }

    private void serializeConnectionForecastSnapshotsForPeriod(LocalDate period, List<ConnectionForecastSnapshot> snapshots) {
        StringWriter stringWriter = new StringWriter();
        try {
            new ObjectMapper().writeValue(stringWriter, snapshots);
        } catch (IOException e) {
            LOGGER.error("Error while trying to serialize the ConnectionForecastSnaphosts in JSON format.", e);
            return;
        }
        String datePrefix = DateTimeFormat.forPattern(YYYYMMDD).print(period) + "_";
        String filePath = ConfigAgr.getConfigurationFolder() + datePrefix + AGR_IDENTIFY_CHANGE_IN_FORECAST_SERIALIZATION_FILENAME;
        try {
            if (new File(filePath).createNewFile()) {
                LOGGER.debug("File [{}] did not exist yet and has been created.", filePath);
            }
        } catch (IOException e) {
            LOGGER.error("Error while verifying the presence of the file [{}]", filePath, e);
            return;
        }
        try (OutputStream fos = new FileOutputStream(filePath)) {
            fos.write(stringWriter.toString().getBytes());
        } catch (IOException e) {
            LOGGER.error("Error while trying to write the ConnectionForecastSnaphot JSON to file.", e);
        }
    }

    /**
     * Deserialize the JSON content containing the information related to the Connection Forecast Snapshots from the filesystem.
     *
     * @return a {@link List} of {@link ConnectionForecastSnapshot}.
     */
    private List<ConnectionForecastSnapshot> deserializeConnectionForecastSnapshots(LocalDate period) {
        String datePrefix = DateTimeFormat.forPattern(YYYYMMDD).print(period) + "_";
        String filePath = ConfigAgr.getConfigurationFolder() + datePrefix + AGR_IDENTIFY_CHANGE_IN_FORECAST_SERIALIZATION_FILENAME;
        List<ConnectionForecastSnapshot> snapshots = null;
        // fetch the json from the file in the configuration folder of the participant

        File snapshotsFile = new File(filePath);
        if (snapshotsFile.exists()) {
            try (InputStream inputStream = new FileInputStream(snapshotsFile)) {
                snapshots = new ObjectMapper().readValue(new BufferedInputStream(inputStream),
                        new TypeReference<List<ConnectionForecastSnapshot>>() {
                        });
            } catch (IOException e) {
                LOGGER.error("Error while trying to deserialize ConnectionForecastSnapshot in JSON format.", e);
            }
        }

        if (snapshots == null) {
            snapshots = new ArrayList<>();
        }
        return snapshots;
    }

}
