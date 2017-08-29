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

package energy.usef.agr.workflow.nonudi.service;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.workflow.nonudi.dto.BalanceResponsiblePartyDto;
import energy.usef.agr.workflow.nonudi.dto.CongestionManagementProfileDto;
import energy.usef.agr.workflow.nonudi.dto.CongestionManagementStatusDto;
import energy.usef.agr.workflow.nonudi.dto.CongestionPointDto;
import energy.usef.agr.workflow.nonudi.dto.ConnectionDto;
import energy.usef.agr.workflow.nonudi.dto.ObjectiveAgentProfileDto;
import energy.usef.agr.workflow.nonudi.dto.ObjectiveAgentStatusDto;
import energy.usef.core.exception.TechnicalException;

import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jodah.recurrent.Recurrent;
import net.jodah.recurrent.RetryPolicy;

/**
 * PowerMatcher implementation for non-udi related services. This PowerMatcher service will handle all calls to the PowerMatcher
 * rest endpoint.
 */
@Singleton
public class PowerMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(PowerMatcher.class);
    private static final int MAX_RETRIES = 60;
    private static final int RETRY_DELAY_MS = 1000;
    private static final String POWER_MATCHER_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final String ENDPOINT_BALANCE_RESPONSIBLE_PARTY = "/rest/clustermanagement/brp";
    private static final String ENDPOINT_CONGESTION_POINT = "/rest/clustermanagement/congestionpoint";
    private static final String ENDPOINT_CONNECTION = "/rest/clustermanagement/connection";
    private static final String ENDPOINT_OBJECTIVE_AGENT = "/rest/objectiveagent";
    private static final String ENDPOINT_CONGESTION_MANAGEMENT = "/rest/congestionmanagement";

    private static ObjectMapper objectMapper;
    private static Client client;
    private static String restEndpointUri;
    private static RetryPolicy retryPolicy;

    @Inject
    private ConfigAgr configAgr;

    /**
     * Returns a interval ({@link String}) based on the date, ptuIndex, ptuDuration and number of ptus. For example:
     * "2015-05-01T13:00:00Z/2015-05-01T14:00:00Z". This time format is compatible with the PowerMatcher.
     *
     * @param date
     * @param ptuIndex
     * @param ptuDuration
     * @param numberOfPtus
     * @return
     */
    public static String getInterval(LocalDate date, int ptuIndex, int ptuDuration, int numberOfPtus) {
        LocalDateTime start = date.toDateTimeAtStartOfDay().toLocalDateTime().plusMinutes((ptuIndex - 1) * ptuDuration);
        LocalDateTime end = start.plusMinutes(numberOfPtus * ptuDuration).minusSeconds(1);


        DateTime startDateTime = start.toDateTime().toDateTime(DateTimeZone.UTC);
        DateTime endDateTime = end.toDateTime().toDateTime(DateTimeZone.UTC);
        return startDateTime.toString(POWER_MATCHER_TIME_FORMAT) + "/" + endDateTime.toString(POWER_MATCHER_TIME_FORMAT);
    }

    /**
     * Initialize the PowerMatcher service.
     */
    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() {
        LOGGER.debug("Initializing PowerMatcher");
        restEndpointUri = configAgr.getProperty(ConfigAgrParam.AGR_POWERMATCHER_ENDPOINT_URI);
        objectMapper = new ObjectMapper();
        client = ClientBuilder.newClient();
        retryPolicy = new RetryPolicy().retryOn(TechnicalException.class).withDelay(RETRY_DELAY_MS, TimeUnit.MILLISECONDS)
                .withMaxRetries(MAX_RETRIES);
    }

    // **************************************** //
    // *** Balance Responsible Party methods ** //
    // **************************************** //

    /**
     * Cleanup the PowerMatcher service.
     */
    @PreDestroy
    public void destroy() {
        LOGGER.debug("Cleaning up PowerMatcher");
        client.close();
    }

    /**
     * Return a list of all Balance Responsible Parties (BRP) currently known at the PowerMatcher.
     *
     * @return {@link List} of {@link BalanceResponsiblePartyDto} objects.
     */
    @SuppressWarnings("unchecked")
    public List<BalanceResponsiblePartyDto> findAllBalanceResponsibleParties() {
        String url = restEndpointUri + ENDPOINT_BALANCE_RESPONSIBLE_PARTY;
        HttpReturnValue<List<BalanceResponsiblePartyDto>> returnValue = httpGet(url,
                new TypeReference<List<BalanceResponsiblePartyDto>>() {
                });

        return returnValue.getReturnValue();
    }

    /**
     * Return a single Balance Responsible Party (BRP) object if BRP is currently known at the PowerMatcher.
     *
     * @param brpId {@link String} containing the BRP id.
     * @return {@link BalanceResponsiblePartyDto} object if exists, otherwise NULL.
     */
    @SuppressWarnings("unchecked")
    public BalanceResponsiblePartyDto findBalanceResponsibleParty(String brpId) {
        String url = restEndpointUri + ENDPOINT_BALANCE_RESPONSIBLE_PARTY + "/" + brpId;
        HttpReturnValue<BalanceResponsiblePartyDto> returnValue = httpGet(url, new TypeReference<BalanceResponsiblePartyDto>() {
        });

        if (returnValue.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            return null;
        }
        return returnValue.getReturnValue();
    }

    /**
     * Creates a Balance Responsible Party (BRP) in the PowerMatcher.
     *
     * @param brp A {@link BalanceResponsiblePartyDto} object containing the BRP.
     */
    public void createBalanceResponsibleParty(BalanceResponsiblePartyDto brp) {
        String url = restEndpointUri + ENDPOINT_BALANCE_RESPONSIBLE_PARTY + "/" + brp.getBrpId();
        httpPut(url, brp);
    }

    // ******************************* //
    // *** Congestion Point methods ** //
    // ******************************* //

    /**
     * Deletes a Balance Responsible Party (BRP) from the PowerMatcher.
     *
     * @param brpId The BRP Id that needs to be deleted.
     */
    public void deleteBalanceResponsibleParty(String brpId) {
        String url = restEndpointUri + ENDPOINT_BALANCE_RESPONSIBLE_PARTY + "/" + brpId;
        httpDelete(url);
    }

    /**
     * Return a list of all Congestion Points (CP) currently known at the PowerMatcher.
     *
     * @return {@link List} of {@link CongestionPointDto} objects.
     */
    @SuppressWarnings("unchecked")
    public List<CongestionPointDto> findAllCongestionPoints() {
        String url = restEndpointUri + ENDPOINT_CONGESTION_POINT;
        HttpReturnValue<List<CongestionPointDto>> returnValue = httpGet(url, new TypeReference<List<CongestionPointDto>>() {
        });

        return returnValue.getReturnValue();
    }

    /**
     * Return a single CongestionPoint (CP) object if CP is currently known at the PowerMatcher.
     *
     * @param congestionPointId {@link String} containing the CP id.
     * @return {@link CongestionPointDto} object if exists, otherwise NULL.
     */
    @SuppressWarnings("unchecked")
    public CongestionPointDto findCongestionPoint(String congestionPointId) {
        String url = restEndpointUri + ENDPOINT_CONGESTION_POINT + "/" + congestionPointId;
        HttpReturnValue<CongestionPointDto> returnValue = httpGet(url, new TypeReference<CongestionPointDto>() {
        });

        if (returnValue.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            return null;
        }
        return returnValue.getReturnValue();
    }

    /**
     * Creates a CongestionPoint (CP) in the PowerMatcher.
     *
     * @param congestionPoint A {@link CongestionPointDto} object containing the CP.
     */
    public void createCongestionPoint(CongestionPointDto congestionPoint) {
        String url = restEndpointUri + ENDPOINT_CONGESTION_POINT + "/" + congestionPoint.getCpId();
        httpPut(url, congestionPoint);
    }

    // ************************* //
    // *** Connection methods ** //
    // ************************* //

    /**
     * Deletes a CongestionPoint (CP) from the PowerMatcher.
     *
     * @param cpId The CP Id of the CongestionPoint that needs to be deleted.
     */
    public void deleteCongestionPoint(String cpId) {
        String url = restEndpointUri + ENDPOINT_CONGESTION_POINT + "/" + cpId;
        httpDelete(url);
    }

    /**
     * Return a list of all Connections (CONN) currently known at the PowerMatcher.
     *
     * @return {@link List} of {@link ConnectionDto} objects.
     */
    @SuppressWarnings("unchecked")
    public List<ConnectionDto> findAllConnections() {
        String url = restEndpointUri + ENDPOINT_CONNECTION;
        HttpReturnValue<BalanceResponsiblePartyDto> returnValue = httpGet(url, new TypeReference<List<ConnectionDto>>() {
        });

        return returnValue.getReturnValue();
    }

    /**
     * Return a single Connection object if CP is currently known at the PowerMatcher.
     *
     * @param connectionId {@link String} containing the Connection id.
     * @return {@link ConnectionDto} object if exists, otherwise NULL.
     */
    @SuppressWarnings("unchecked")
    public ConnectionDto findConnection(String connectionId) {
        String url = restEndpointUri + ENDPOINT_CONNECTION + "/" + connectionId;
        HttpReturnValue<BalanceResponsiblePartyDto> returnValue = httpGet(url, new TypeReference<ConnectionDto>() {
        });

        if (returnValue.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            return null;
        }
        return returnValue.getReturnValue();
    }

    /**
     * Creates a Connection in the PowerMatcher.
     *
     * @param connection A {@link ConnectionDto} object containing the Connection.
     */
    public void createConnection(ConnectionDto connection) {
        String url = restEndpointUri + ENDPOINT_CONNECTION + "/" + connection.getConnectionId();
        httpPut(url, connection);
    }

    // ******************************* //
    // *** Objective Agent methods *** //
    // ******************************* //

    /**
     * Deletes a Connection (CP) from the PowerMatcher.
     *
     * @param connection A {@link ConnectionDto} object containing the Connection.
     */
    public void deleteConnection(ConnectionDto connection) {
        String url = restEndpointUri + ENDPOINT_CONNECTION + "/" + connection.getConnectionId();
        httpDelete(url);
    }

    /**
     * Retrieve the ADS Goal Realization for the given connection group.
     *
     * @param brpUsefIdentifier {@link String} USEF identifier of BRP the connection group.
     *
     * @return {@link ObjectiveAgentStatusDto} object containing the current goal status returned by the PowerMatcher Objective
     * Agent.
     */
    @SuppressWarnings("unchecked")
    public Optional<ObjectiveAgentStatusDto> retrieveBrpAdsGoalRealization(String brpUsefIdentifier) {
        String url = restEndpointUri + ENDPOINT_OBJECTIVE_AGENT + "/" + brpUsefIdentifier + "/status";
        HttpReturnValue<ObjectiveAgentProfileDto> httpReturnValue = httpGet(url,
                new TypeReference<ObjectiveAgentStatusDto>() {
                });
        return Optional.ofNullable(httpReturnValue.getReturnValue());
    }

    /**
     * Retrieve the ADS Goal Realization for the given congestion point.
     *
     * @param congestionPointIdentifier
     * @return {@link CongestionManagementStatusDto} object containing the current goal status returned by the PowerMatcher
     * CongestionManagement.
     */
    @SuppressWarnings("unchecked")
    public Optional<CongestionManagementStatusDto> retrieveCongestionPointAdsGoalRealization(String congestionPointIdentifier) {
        String url = restEndpointUri + ENDPOINT_CONGESTION_MANAGEMENT + "/" + congestionPointIdentifier + "/status";
        HttpReturnValue<CongestionManagementStatusDto> httpReturnValue = httpGet(url,
                new TypeReference<CongestionManagementStatusDto>() {
                });
        return Optional.ofNullable(httpReturnValue.getReturnValue());
    }

    // ************************************* //
    // *** Congestion Management methods *** //
    // ************************************* //

    /**
     * Do an HTTP POST request with a list of {@link ObjectiveAgentProfileDto}'s to the objective agent of PowerMatcher.
     *
     * @param usefIdentifier the identifier of the brp
     * @param objectiveAgentProfileDtos List of {@link ObjectiveAgentProfileDto}'s.
     */
    public void postObjectiveAgent(String usefIdentifier, List<ObjectiveAgentProfileDto> objectiveAgentProfileDtos) {
        String url = restEndpointUri + ENDPOINT_OBJECTIVE_AGENT + "/" + usefIdentifier + "/profile";
        httpPost(url, objectiveAgentProfileDtos);
    }

    // ********************************* //
    // *** Some public helper methods ** //
    // ********************************* //

    /**
     * Do an HTTP POST request with a list of {@link CongestionManagementProfileDto}'s to the congestion management of the
     * PowerMatcher.
     *
     * @param usefIdentifier the identifier of the congestion point
     * @param congestionManagementProfileDtos List of {@link CongestionManagementProfileDto}'s.
     */
    public void postCongestionManagement(String usefIdentifier,
            List<CongestionManagementProfileDto> congestionManagementProfileDtos) {
        String url = restEndpointUri + ENDPOINT_CONGESTION_MANAGEMENT + "/" + usefIdentifier + "/profile";
        httpPost(url, congestionManagementProfileDtos);
    }

    // *************************** //
    // *** Some private methods ** //
    // *************************** //

    /**
     * Perform GET request for given URL and return JSON-value as String.
     * <p>
     * This method runs multiple times until it is successful.
     *
     * @param url
     * @param typeReference
     * @return
     */
    private HttpReturnValue httpGet(String url, TypeReference typeReference) throws TechnicalException {
        return Recurrent.get(() -> {
            try {
                WebTarget target = client.target(url);
                Response response = target.request().get();
                String json = response.readEntity(String.class);
                int status = response.getStatus();
                response.close();

                return new HttpReturnValue(status, readValue(json, typeReference));
            } catch (Exception e) {
                LOGGER.error("Error while performing http GET request: {}", e.getMessage());
                throw new TechnicalException("Error while performing http GET request", e);
            }
        }, retryPolicy);
    }

    /**
     * Perform PUT request for given URL and return JSON-value as String.
     * <p>
     * This method runs multiple times until it is successful.
     *
     * @param url
     * @return
     */
    private HttpReturnValue httpPut(String url, Object object) throws TechnicalException {
        return Recurrent.get(() -> {
            try {
                String jsonObject = writeValue(object);
                WebTarget target = client.target(url);
                Response response = target.request().put(Entity.entity(jsonObject, MediaType.APPLICATION_JSON_TYPE));
                String json = response.readEntity(String.class);
                int status = response.getStatus();
                response.close();

                return new HttpReturnValue(status, json);
            } catch (Exception e) {
                LOGGER.error("Error while performing http PUT request: {}", e.getMessage());
                throw new TechnicalException("Error while performing http PUT request", e);
            }
        }, retryPolicy);
    }

    /**
     * Perform PUT request for given URL and return JSON-value as String.
     * <p>
     * This method runs multiple times until it is successful.
     *
     * @param url
     * @return
     */
    private HttpReturnValue httpPost(String url, Object object) throws TechnicalException {
        return Recurrent.get(() -> {
            try {
                String jsonObject = writeValue(object);
                WebTarget target = client.target(url);
                Response response = target.request().post(Entity.entity(jsonObject, MediaType.APPLICATION_JSON_TYPE));
                String json = response.readEntity(String.class);
                int status = response.getStatus();
                response.close();

                return new HttpReturnValue(status, json);
            } catch (Exception e) {
                LOGGER.error("Error while performing http POST request: {}", e.getMessage());
                throw new TechnicalException("Error while performing http POST request", e);
            }
        }, retryPolicy);
    }

    /**
     * Perform DELETE request for given URL and return JSON-value as String.
     * <p>
     * This method runs multiple times until it is successful.
     *
     * @param url
     * @return
     */
    private HttpReturnValue httpDelete(String url) throws TechnicalException {
        return Recurrent.get(() -> {
            try {
                WebTarget target = client.target(url);
                Response response = target.request().delete();
                String json = response.readEntity(String.class);
                int status = response.getStatus();
                response.close();

                return new HttpReturnValue(status, json);
            } catch (Exception e) {
                LOGGER.error("Error while performing http DELETE request: {}", e.getMessage());
                throw new TechnicalException("Error while performing http DELETE request", e);
            }
        }, retryPolicy);
    }

    private <T> T readValue(String json, TypeReference valueTypeRef) {
        try {
            return objectMapper.readValue(json, valueTypeRef);
        } catch (Exception e) {
            LOGGER.warn("Exception caught while parsing json '{}'", json, e);
            return null;
        }
    }

    private String writeValue(Object object) {
        StringWriter output = new StringWriter();
        try {
            objectMapper.writeValue(output, object);
        } catch (Exception e) {
            LOGGER.warn("Exception caught while parsing object '{}' to json", object, e);
            return null;
        }
        return output.toString();
    }
}
