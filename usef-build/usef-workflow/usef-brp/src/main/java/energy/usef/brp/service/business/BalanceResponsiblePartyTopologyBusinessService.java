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
package energy.usef.brp.service.business;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import energy.usef.brp.dto.ConnectionActionDto;
import energy.usef.brp.dto.ParticipantActionDto;
import energy.usef.brp.dto.SynchronisationConnectionDto;
import energy.usef.brp.model.CommonReferenceOperator;
import energy.usef.brp.model.SynchronisationConnection;
import energy.usef.brp.model.SynchronisationConnectionStatus;
import energy.usef.brp.model.SynchronisationConnectionStatusType;
import energy.usef.brp.repository.CommonReferenceOperatorRepository;
import energy.usef.brp.repository.SynchronisationConnectionRepository;
import energy.usef.brp.repository.SynchronisationConnectionStatusRepository;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.rest.RestResult;
import energy.usef.core.rest.RestResultFactory;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.JsonUtil;
import org.jboss.resteasy.util.HttpResponseCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static energy.usef.core.util.JsonUtil.ROOT_KEY;
import static energy.usef.core.util.JsonUtil.createJsonText;

/**
 * This service class implements the business logic related to the CRO part of the common reference query.
 */
@Stateless
public class BalanceResponsiblePartyTopologyBusinessService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BalanceResponsiblePartyTopologyBusinessService.class);

    private static final String JSON_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    @Inject
    BalanceResponsiblePartyValidationBusinessService validationService;

    @Inject
    CommonReferenceOperatorRepository commonReferenceOperatorRepository;

    @Inject
    SynchronisationConnectionRepository synchronisationConnectionRepository;

    @Inject
    SynchronisationConnectionStatusRepository synchronisationConnectionStatusRepository;

    /**
     * Try and retrieve a {@link SynchronisationConnection} with the given entityAddress, returning it in a {@Link RestResult}.
     *
     * @param entityAddress a {@link String} containing a entit addressof the {@link SynchronisationConnection} to be retrieved.
     * @return a {@Link RestResult}
     */
    public RestResult findSynchronisationConnection(String entityAddress) throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();
        SynchronisationConnection connection = synchronisationConnectionRepository.findByEntityAddress(entityAddress);

        if (connection != null) {
            result.setCode(HttpResponseCodes.SC_OK);
            result.setBody(createJsonText(new SynchronisationConnectionDto(connection.getId(), connection.getEntityAddress(),
                    DateTimeUtil.printDateTime(connection.getLastModificationTime(), JSON_DATE_TIME_FORMAT))));
        } else {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add("SynchronisationConnection " + entityAddress + " not found.");
        }
        return result;
    }

    /**
     * Try and retrieve all {@link SynchronisationConnection} instances.
     */
    public RestResult findAllSynchronisationConnections() throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();

        List<SynchronisationConnectionDto> connectionList = new ArrayList<>();
        synchronisationConnectionRepository.findAll().stream().forEach(
                connection -> connectionList.add(new SynchronisationConnectionDto(connection.getId(), connection.getEntityAddress(),
                        DateTimeUtil.printDateTime(connection.getLastModificationTime(), JSON_DATE_TIME_FORMAT))));

        result.setCode(HttpResponseCodes.SC_OK);
        result.setBody(createJsonText(connectionList));

        return result;
    }

    /**
     * Process a batch of {@Link SynchronisationConnection} updates.
     *
     * @param jsonText a json {@link String} containing a batch of {@Link SynchronisationConnection} updates.
     * @return a {@Link Response} message containing batch update results
     */

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<RestResult> processSynchronisationConnectionBatch(String jsonText) throws IOException, ProcessingException {
        LOGGER.info("Start processing Common Reference Operator batch.");

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(jsonText);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Map<Integer, RestResult> resultMap = new HashMap<>();
        JsonUtil.validateNodeSyntax("/connection-schema.json", root, resultMap);

        if (!resultMap.containsKey(ROOT_KEY)) {
            LOGGER.error("Valid Json message");
            List<ConnectionActionDto> actions = objectMapper.readValue(jsonText, new TypeReference<List<ConnectionActionDto>>() {
            });

            List<CommonReferenceOperator> existingCommonReferenceOperators = commonReferenceOperatorRepository.findAll();

            // Now process all the actions that have the correct syntax.
            for (int entry = 0; entry < actions.size(); entry++) {
                if (!resultMap.containsKey(entry)) {
                    resultMap
                            .put(entry, processSynchronisationConnectionNode(actions.get(entry), existingCommonReferenceOperators));
                }
            }
        }

        return resultMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(a -> a.getValue()).collect(Collectors.toList());
    }

    /**
     * Try and retrieve a {@link CommonReferenceOperator} with the given domain name, returning it in a {@Link RestResult}.
     *
     * @param domain a {@link String} containing a domain name of the {@link CommonReferenceOperator} to be retrieved.
     * @return a {@Link RestResult}
     */
    public RestResult findCommonReferenceOperator(String domain) throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();
        CommonReferenceOperator participant = commonReferenceOperatorRepository.findByDomain(domain);

        if (participant != null) {
            result.setCode(HttpResponseCodes.SC_OK);
            result.setBody(createJsonText(participant));
        } else {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add("Common Reference Operator " + domain + " not found.");
        }
        return result;
    }

    /**
     * Try and retrieve all {@link CommonReferenceOperator} instances.
     */
    public RestResult findAllCommonReferenceOperators() throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();

        result.setCode(HttpResponseCodes.SC_OK);
        result.setBody(createJsonText(commonReferenceOperatorRepository.findAll()));

        return result;
    }

    /**
     * Process a batch of {@Link CommonReferenceOperator} updates.
     *
     * @param jsonText a json {@link String} containing a batch of {@Link CommonReferenceOperator} updates.
     * @return a {@Link Response} message containing batch update results
     */

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<RestResult> processCommonReferenceOperatorBatch(String jsonText) throws IOException, ProcessingException {
        LOGGER.info("Start processing Common Reference Operator batch.");

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(jsonText);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Map<Integer, RestResult> resultMap = new HashMap<>();
        JsonUtil.validateNodeSyntax("/participant-schema.json", root, resultMap);

        if (!resultMap.containsKey(ROOT_KEY)) {
            LOGGER.error("Valid Json message");
            List<ParticipantActionDto> actions = objectMapper.readValue(jsonText, new TypeReference<List<ParticipantActionDto>>() {
            });

            List<SynchronisationConnection> existingSynchronisationConnections = synchronisationConnectionRepository.findAll();

            // Now process all the actions that have the correct syntax.
            for (int entry = 0; entry < actions.size(); entry++) {
                if (!resultMap.containsKey(entry)) {
                    resultMap
                            .put(entry, processCommonReferenceOperatorNode(actions.get(entry), existingSynchronisationConnections));
                }
            }
        }

        return resultMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(a -> a.getValue()).collect(Collectors.toList());
    }

    private RestResult processCommonReferenceOperatorNode(ParticipantActionDto action,
            List<SynchronisationConnection> existingSynchronisationConnections) throws IOException {
        String method = action.getMethod();
        String domain = action.getDomain();
        RestResult result = new RestResult();
        result.setCode(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR);

        switch (method) {
        case HttpMethod.GET:
            result = findCommonReferenceOperator(domain);
            break;
        case HttpMethod.POST:
            result = createCommonReferenceOperator(domain, existingSynchronisationConnections);
            break;
        case HttpMethod.DELETE:
            result = deleteCommonReferenceOperator(domain);
            break;
        default:
            result = JsonUtil.notSupported(method, "CommonReferenceOperator");
        }
        return result;
    }

    private RestResult processSynchronisationConnectionNode(ConnectionActionDto action,
            List<CommonReferenceOperator> existingCommonReferenceOperators) throws IOException {
        String method = action.getMethod();
        String entityAddress = action.getEntityAddress();
        RestResult result = new RestResult();
        result.setCode(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR);

        switch (method) {
        case HttpMethod.GET:
            result = findSynchronisationConnection(entityAddress);
            break;
        case HttpMethod.POST:
            result = createSynchronisationConnection(entityAddress, existingCommonReferenceOperators);
            break;
        case HttpMethod.DELETE:
            result = deleteSynchronisationConnection(entityAddress);
            break;
        default:
            result = JsonUtil.notSupported(method, "SynchronisationConnection");
        }
        return result;
    }

    private RestResult createCommonReferenceOperator(String domain,
            List<SynchronisationConnection> existingSynchronisationConnections) {
        RestResult result = RestResultFactory.getJsonRestResult();
        try {
            validationService.checkDuplicateCommonReferenceOperatorDomain(domain);

            CommonReferenceOperator commonReferenceOperator = new CommonReferenceOperator();
            commonReferenceOperator.setDomain(domain);
            commonReferenceOperatorRepository.persist(commonReferenceOperator);

            existingSynchronisationConnections.forEach(synchronisationConnection -> {
                SynchronisationConnectionStatus synchronisationConnectionStatus = new SynchronisationConnectionStatus();
                synchronisationConnectionStatus.setSynchronisationConnection(synchronisationConnection);
                synchronisationConnectionStatus.setCommonReferenceOperator(commonReferenceOperator);
                synchronisationConnectionStatus.setStatus(SynchronisationConnectionStatusType.MODIFIED);
                synchronisationConnectionStatusRepository.persist(synchronisationConnectionStatus);
            });

            result.setCode(HttpResponseCodes.SC_CREATED);
            LOGGER.info("Common Reference Operator {} created", domain);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_CONFLICT);
            result.getErrors().add(e.getMessage());
            LOGGER.info("Duplicate Common Reference Operator {} not created", domain);
        }
        return result;
    }

    private RestResult deleteCommonReferenceOperator(String domain) {
        RestResult result = RestResultFactory.getJsonRestResult();
        try {
            validationService.checkExistingCommonReferenceOperatorDomain(domain);
            CommonReferenceOperator commonReferenceOperator = commonReferenceOperatorRepository.findByDomain(domain);
            synchronisationConnectionStatusRepository.deleteFor(commonReferenceOperator);

            commonReferenceOperatorRepository.deleteByDomain(domain);
            result.setCode(HttpResponseCodes.SC_OK);
            LOGGER.info("Common Reference Operator {} deleted", domain);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add(e.getMessage());
            LOGGER.info("Common Reference Operator {} not found", domain);
        }
        return result;
    }

    private RestResult createSynchronisationConnection(String entityAddress,
            List<CommonReferenceOperator> existingCommonReferenceOperators) {
        RestResult result = RestResultFactory.getJsonRestResult();
        try {
            validationService.checkDuplicateSynchronisationConnection(entityAddress);

            SynchronisationConnection synchronisationConnection = new SynchronisationConnection();
            synchronisationConnection.setEntityAddress(entityAddress);
            synchronisationConnection.setLastModificationTime(DateTimeUtil.getCurrentDateTime());
            synchronisationConnectionRepository.persist(synchronisationConnection);

            existingCommonReferenceOperators.forEach(commonReferenceOperator -> {
                SynchronisationConnectionStatus synchronisationConnectionStatus = new SynchronisationConnectionStatus();
                synchronisationConnectionStatus.setSynchronisationConnection(synchronisationConnection);
                synchronisationConnectionStatus.setCommonReferenceOperator(commonReferenceOperator);
                synchronisationConnectionStatus.setStatus(SynchronisationConnectionStatusType.MODIFIED);
                synchronisationConnectionStatusRepository.persist(synchronisationConnectionStatus);
            });

            result.setCode(HttpResponseCodes.SC_CREATED);
            LOGGER.info("SynchronisationConnection {} created", entityAddress);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_CONFLICT);
            result.getErrors().add(e.getMessage());
            LOGGER.info("Duplicate SynchronisationConnection {} not created", entityAddress);
        }
        return result;
    }

    private RestResult deleteSynchronisationConnection(String entityAddress) {
        RestResult result = RestResultFactory.getJsonRestResult();
        try {
            validationService.checkExistingSynchronisationConnection(entityAddress);
            SynchronisationConnection connection = synchronisationConnectionRepository.findByEntityAddress(entityAddress);
            synchronisationConnectionStatusRepository.deleteFor(connection);
            synchronisationConnectionRepository.deleteByEntityAddress(entityAddress);
            result.setCode(HttpResponseCodes.SC_OK);
            LOGGER.info("SynchronisationConnection {} deleted", entityAddress);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add(e.getMessage());
            LOGGER.info("SynchronisationConnection {} not found", entityAddress);
        }
        return result;
    }
}
