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

package energy.usef.pbcfeeder;

import energy.usef.pbcfeeder.config.ConfigPbcFeeder;
import energy.usef.pbcfeeder.config.ConfigPbcFeederParam;
import energy.usef.pbcfeeder.dto.PbcStubDataDto;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RestService for fetching JSON-data from PBCFeeder-deployment. The endpoint of the PBCFeeder is configured within the main
 * configuration file.
 */
public class PbcFeederClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(PbcFeederClient.class);

    @Inject
    private ConfigPbcFeeder config;

    /**
     * Return a given amount of {@link PbcStubDataDto} in a list starting from the PtuIndex on the given
     * date.
     *
     * @param date
     * @param ptuIndex
     * @param amount
     * @return
     */
    public List<PbcStubDataDto> getPbcStubDataList(LocalDate date, int ptuIndex, int amount) {
        String pbcEndpoint =
                config.getProperty(ConfigPbcFeederParam.PBC_FEEDER_ENDPOINT) + "/ptu/" + date.toString() + "/" + ptuIndex + "/" + amount;

        String value = get(pbcEndpoint);
        ObjectMapper mapper = new ObjectMapper();

        List<PbcStubDataDto> pbcStubDataDtoList = new ArrayList<>();
        try {
            pbcStubDataDtoList = mapper.readValue(value, new TypeReference<List<PbcStubDataDto>>() {
            });
        } catch (IOException e) {
            LOGGER.error("Exception caught: {}", e);
        }
        return pbcStubDataDtoList;
    }

    /**
     * Gets the list of power limits for the specified congestion point.
     *
     * @param congestionPointId {@link Integer} congestion point id (typically 1,2 or 3).
     * @return a {@link List} of {@link BigDecimal}: the first element of the list is the lower limit, the second element of the
     * list is the upper limit.
     */
    public List<BigDecimal> getCongestionPointPowerLimits(Integer congestionPointId) {
        String pbcEndpoint = config.getProperty(ConfigPbcFeederParam.PBC_FEEDER_ENDPOINT) + "/powerLimit/" + congestionPointId;
        String value = get(pbcEndpoint);
        ObjectMapper mapper = new ObjectMapper();
        try {
            LOGGER.debug("Received power limits from PBC Feeder endpoint: {}", value);
            return mapper.readValue(value, new TypeReference<List<BigDecimal>>() {
            });
        } catch (IOException e) {
            LOGGER.error("Exception caught while getting power limits for congestion point with id [{}]", congestionPointId, e);
        }
        return new ArrayList<>();
    }

    /**
     * Perform GET request for given URL and return JSON-value as String.
     *
     * @param url
     * @return
     */
    public String get(String url) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(url);
        Response response = target.request().buildGet().invoke();
        String value = response.readEntity(String.class);
        response.close();
        return value;
    }
}
