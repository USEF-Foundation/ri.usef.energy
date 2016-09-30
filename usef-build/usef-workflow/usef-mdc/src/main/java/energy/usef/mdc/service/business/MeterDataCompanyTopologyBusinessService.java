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
package energy.usef.mdc.service.business;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import energy.usef.core.config.Role;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.rest.RestResult;
import energy.usef.core.rest.RestResultFactory;
import energy.usef.core.util.JsonUtil;
import energy.usef.mdc.dto.ConnectionAction;
import energy.usef.mdc.dto.ParticipantAction;
import energy.usef.mdc.model.BalanceResponsibleParty;
import energy.usef.mdc.model.CommonReferenceOperator;
import energy.usef.mdc.model.Connection;
import energy.usef.mdc.model.DistributionSystemOperator;
import energy.usef.mdc.repository.BalanceResponsiblePartyRepository;
import energy.usef.mdc.repository.CommonReferenceOperatorRepository;
import energy.usef.mdc.repository.DistributionSystemOperatorRepository;
import energy.usef.mdc.repository.MdcConnectionRepository;
import org.jboss.resteasy.util.HttpResponseCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static energy.usef.core.config.Role.*;
import static energy.usef.core.util.JsonUtil.ROOT_KEY;
import static energy.usef.core.util.JsonUtil.createJsonText;

/**
 * This service class implements the business logic related to the CRO part of the common reference query.
 */
@Stateless
public class MeterDataCompanyTopologyBusinessService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterDataCompanyTopologyBusinessService.class);


    @Inject
    BalanceResponsiblePartyRepository balanceResponsiblePartyRepository;

    @Inject
    DistributionSystemOperatorRepository distributionSystemOperatorRepository;

    @Inject
    CommonReferenceOperatorRepository commonReferenceOperatorRepository;

    @Inject
    MdcConnectionRepository mdcConnectionRepository;

    @Inject
    MeterDataCompanyValidationBusinessService validationService;

    /**
     * Try and retrieve an {@link Connection} with the given entityAddress, returning it as a {@Link RestResult}.
     *
     * @param entityAddress a {@link String} containing a entityAddress of the {@link Connection} to be retrieved.
     * @return a {@Link RestResult}
     */
    public RestResult findConnection(String entityAddress) throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();
        Connection connection = mdcConnectionRepository.find(entityAddress);

        if (connection != null) {
            result.setCode(HttpResponseCodes.SC_OK);
            result.setBody(createJsonText(connection));
        } else {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add("Connection " + entityAddress + " not found.");
        }
        return result;
    }
    /**
     * Try and retrieve an {@link BalanceResponsibleParty} with the given domain name, returning it as a {@Link RestResult}.
     *
     * @param domain a {@link String} containing a domain name of the {@link BalanceResponsibleParty} to be retrieved.
     * @return a {@Link RestResult}
     */
    public RestResult findBalanceResponsibleParty(String domain) throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();
        BalanceResponsibleParty participant = balanceResponsiblePartyRepository.find(domain);

        if (participant != null) {
            result.setCode(HttpResponseCodes.SC_OK);
            result.setBody(createJsonText(participant));
        } else {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add("BalanceResponsibleParty " + domain + " not found.");
        }
        return result;
    }

    /**
     * Try and retrieve an {@link DistributionSystemOperator} with the given domain name, returning it as a {@Link RestResult}.
     *
     * @param domain a {@link String} containing a domain name of the {@link DistributionSystemOperator} to be retrieved.
     * @return a {@Link RestResult}
     */
    public RestResult findDistributionSystemOperator(String domain) throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();
        DistributionSystemOperator participant = distributionSystemOperatorRepository.find(domain);

        if (participant != null) {
            result.setCode(HttpResponseCodes.SC_OK);
            result.setBody(createJsonText(participant.getDomain()));
        } else {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add("DistributionSystemOperator " + domain + " not found.");
        }
        return result;
    }

    /**
     * Try and retrieve an {@link CommonReferenceOperator} with the given domain name, returning it as a {@Link RestResult}.
     *
     * @param domain a {@link String} containing a domain name of the {@link CommonReferenceOperator} to be retrieved.
     * @return a {@Link RestResult}
     */
    public RestResult findCommonReferenceOperator(String domain) throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();
        CommonReferenceOperator participant = commonReferenceOperatorRepository.find(domain);

        if (participant != null) {

            result.setCode(HttpResponseCodes.SC_OK);
            result.setBody(createJsonText(participant.getDomain()));
        } else {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add("CommonReferenceOperator " + domain + " not found.");
        }
        return result;
    }

    /**
     * Try and retrieve all {@link Connection} instances.
     */
    public RestResult findAllConnections() throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();

        List<Connection> connectionList = mdcConnectionRepository.findAllConnections();
        result.setCode(HttpResponseCodes.SC_OK);
        result.setBody(createJsonText(connectionList));

        return result;
    }

    /**
     * Try and retrieve all{@link BalanceResponsibleParty} instances.
     */
    public RestResult findAllBalanceResponsibleParties() throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();

        List<BalanceResponsibleParty> participantList = balanceResponsiblePartyRepository.findAll();
        result.setCode(HttpResponseCodes.SC_OK);
        result.setBody(createJsonText(participantList));

        return result;
    }

    /**
     * Try and retrieve all{@link DistributionSystemOperator} instances.
     */
    public RestResult findAllDistributionSystemOperators() throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();

        List<DistributionSystemOperator> participantList = distributionSystemOperatorRepository.findAll();
        result.setCode(HttpResponseCodes.SC_OK);
        result.setBody(createJsonText(participantList));

        return result;
    }

    /**
     * Try and retrieve all{@link CommonReferenceOperator} instances.
     */
    public RestResult findAllCommonReferenceOperators() throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();

        List<CommonReferenceOperator> participantList = commonReferenceOperatorRepository.findAll();
        result.setCode(HttpResponseCodes.SC_OK);
        result.setBody(createJsonText(participantList));

        return result;
    }

    /**
     * Process a batch of {@Link Connection} updates.
     *
     * @param jsonText a json {@link String} containing a batch of {@Link Connection} updates.
     * @return a {@Link Response} message containing batch update results
     */

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<RestResult> processConnectionBatch(String jsonText) throws IOException, ProcessingException {
        LOGGER.info("Start processing Connection batch.");
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(jsonText);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Map<Integer, RestResult> resultMap = new HashMap<>();
        JsonUtil.validateNodeSyntax("/connection-schema.json", root, resultMap);

        if (!resultMap.containsKey(ROOT_KEY)) {
            List<ConnectionAction> actions = objectMapper.readValue(jsonText, new TypeReference<List<ConnectionAction>>() {
            });

            // Now process all the actions that have the correct syntax.
            for (int entry = 0; entry < actions.size(); entry++) {
                if (!resultMap.containsKey(entry)) {

                    resultMap.put(entry, processConnectionNode(actions.get(entry)));
                }
            }
        }

        return resultMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(a -> a.getValue()).collect(Collectors.toList());
    }

    /**
     * Process a batch of {@Link BalanceResponsibleParty} updates.
     *
     * @param jsonText a json {@link String} containing a batch of {@Link BalanceResponsibleParty} updates.
     * @return a {@Link Response} message containing batch update results
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<RestResult> processBalanceResponsiblePartyBatch(String jsonText) throws IOException, ProcessingException {
        LOGGER.info("Start processing BalanceResponsibleParty batch");
        return processParticipantBatch(BRP, jsonText);
    }

    /**
     * Process a batch of {@Link DistributionSystemOperator} updates.
     *
     * @param jsonText a json {@link String} containing a batch of {@Link DistributionSystemOperator} updates.
     * @return a {@Link Response} message containing batch update results
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<RestResult> processDistributionSystemOperatorBatch(String jsonText) throws IOException, ProcessingException {
        LOGGER.info("Start processing DistributionSystemOperator batch");
        return processParticipantBatch(DSO, jsonText);
    }

    /**
     * Process a batch of {@Link CommonReferenceOperator} updates.
     *
     * @param jsonText a json {@link String} containing a batch of {@Link CommonReferenceOperator} updates.
     * @return a {@Link Response} message containing batch update results
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<RestResult> processCommonReferenceOperatorBatch(String jsonText) throws IOException, ProcessingException {
        LOGGER.info("Start processing a CommonReferenceOperator batch");
        return processParticipantBatch(CRO, jsonText);
    }

    private List<RestResult> processParticipantBatch(Role role, String jsonText) throws IOException, ProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(jsonText);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Map<Integer, RestResult> resultMap = new HashMap<>();
        JsonUtil.validateNodeSyntax("/participant-schema.json", root, resultMap);

        if (!resultMap.containsKey(ROOT_KEY)) {
            LOGGER.error("Valid Json message");
            List<ParticipantAction> actions = objectMapper.readValue(jsonText, new TypeReference<List<ParticipantAction>>() {
            });

            // Now process all the actions that have the correct syntax.
            for (int entry = 0; entry < actions.size(); entry++) {
                if (!resultMap.containsKey(entry)) {
                    resultMap.put(entry, processParticipantNode(role, actions.get(entry)));
                }
            }
        }

        return resultMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(a -> a.getValue()).collect(Collectors.toList());
    }

    private RestResult processConnectionNode(ConnectionAction action) throws IOException {
        String method = action.getMethod();
        String entityAddress = action.getEntityAddress();
        RestResult result = new RestResult();
        result.setCode(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR);

        switch (method) {
        case HttpMethod.GET:
            result = findConnection(entityAddress);
            break;
        case HttpMethod.POST:
            result = createConnection(entityAddress);
            break;
        case HttpMethod.DELETE:
            result = deleteConnection(entityAddress);
            break;
        default:
            result = JsonUtil.notSupported(method, "Connection");
        }
        return result;
    }

    private RestResult processParticipantNode(Role role, ParticipantAction action) throws IOException {
        String method = action.getMethod();
        String domain = action.getDomain();
        RestResult result = new RestResult();
        result.setCode(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR);

        switch (method) {
        case HttpMethod.GET:
            result = findParticipant(role, domain);
            break;
        case HttpMethod.POST:
            result = createParticipant(role, domain);
            break;
        case HttpMethod.DELETE:
            result = deleteParticipant(role, domain);
            break;
        default:
            result = JsonUtil.notSupported(method, "Particicpant");
        }
        return result;
    }

    private RestResult findParticipant(Role role, String domain) throws IOException {
        RestResult result = new RestResult();
        result.setCode(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR);

        switch (role) {
        case BRP:
            result = findBalanceResponsibleParty(domain);
            break;
        case CRO:
            result = findCommonReferenceOperator(domain);
            break;
        case DSO:
            result = findDistributionSystemOperator(domain);
            break;
        default:
            result = JsonUtil.unknownRole(role.toString());
        }
        return result;
    }

    private RestResult createParticipant(Role role, String domain) throws IOException {
        RestResult result = new RestResult();
        result.setCode(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR);

        switch (role) {
        case BRP:
            result = createBalanceResponsibleParty(domain);
            break;
        case CRO:
            result = createCommonReferenceOperator(domain);
            break;
        case DSO:
            result = createDistributionSystemOperator(domain);
            break;
        default:
            result = JsonUtil.unknownRole(role.toString());
        }
        return result;
    }

    private RestResult deleteParticipant(Role role, String domain) throws IOException {
        RestResult result = new RestResult();
        result.setCode(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR);

        switch (role) {
        case BRP:
            result = deleteBalanceResponsibleParty(domain);
            break;
        case CRO:
            result = deleteCommonReferenceOperator(domain);
            break;
        case DSO:
            result = deleteDistributionSystemOperator(domain);
            break;
        default:
            result = JsonUtil.unknownRole(role.toString());
        }
        return result;
    }

    private RestResult createConnection(String entityAddress) {
        RestResult result = RestResultFactory.getJsonRestResult();
        try {
            validationService.checkDuplicateConnection(entityAddress);

            Connection participant = new Connection();
            participant.setEntityAddress(entityAddress);
            mdcConnectionRepository.persist(participant);

            result.setCode(HttpResponseCodes.SC_CREATED);
            LOGGER.info("Connection {} created", entityAddress);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_CONFLICT);
            result.getErrors().add(e.getMessage());
            LOGGER.info("Duplicate Connection {} not created", entityAddress);
        }
        return result;
    }

    private RestResult createBalanceResponsibleParty(String domain) {
        RestResult result = RestResultFactory.getJsonRestResult();
        try {
            validationService.checkDuplicateBalanceResponsiblePartyDomain(domain);

            BalanceResponsibleParty participant = new BalanceResponsibleParty();
            participant.setDomain(domain);
            balanceResponsiblePartyRepository.persist(participant);

            result.setCode(HttpResponseCodes.SC_CREATED);
            LOGGER.info("BalanceResponsibleParty {} created", domain);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_CONFLICT);
            result.getErrors().add(e.getMessage());
            LOGGER.info("Duplicate BalanceResponsibleParty {} not created", domain);
        }
        return result;
    }

    private RestResult createDistributionSystemOperator(String domain) {
        RestResult result = RestResultFactory.getJsonRestResult();
        try {
            validationService.checkDuplicateDistributionSystemOperatorDomain(domain);

            DistributionSystemOperator participant = new DistributionSystemOperator();
            participant.setDomain(domain);
            distributionSystemOperatorRepository.persist(participant);

            result.setCode(HttpResponseCodes.SC_CREATED);
            LOGGER.info("DistributionSystemOperator {} created", domain);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_CONFLICT);
            result.getErrors().add(e.getMessage());
            LOGGER.info("Duplicate DistributionSystemOperator {} not created", domain);
        }
        return result;
    }

    private RestResult createCommonReferenceOperator(String domain) {
        RestResult result = RestResultFactory.getJsonRestResult();
        try {
            validationService.checkDuplicateCommonReferenceOperatorDomain(domain);

            CommonReferenceOperator participant = new CommonReferenceOperator();
            participant.setDomain(domain);
            commonReferenceOperatorRepository.persist(participant);

            result.setCode(HttpResponseCodes.SC_CREATED);
            LOGGER.info("CommonReferenceOperator {} created", domain);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_CONFLICT);
            result.getErrors().add(e.getMessage());
            LOGGER.info("Duplicate CommonReferenceOperator {} not created", domain);
        }
        return result;
    }

    private RestResult deleteConnection(String domain) {
        RestResult result = RestResultFactory.getJsonRestResult();
        try {
            validationService.checkExistingConnection(domain);
            mdcConnectionRepository.deleteByEntityAddress(domain);
            result.setCode(HttpResponseCodes.SC_OK);
            LOGGER.info("Connection {} deleted", domain);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add(e.getMessage());
            LOGGER.info("Connection {} not found", domain);
        }
        return result;
    }

    private RestResult deleteBalanceResponsibleParty(String domain) {
        RestResult result = RestResultFactory.getJsonRestResult();
        try {
            validationService.checkExistingBalanceResponsiblePartyDomain(domain);
            balanceResponsiblePartyRepository.deleteByDomain(domain);
            result.setCode(HttpResponseCodes.SC_OK);
            LOGGER.info("BalanceResponsibleParty {} deleted", domain);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add(e.getMessage());
            LOGGER.info("BalanceResponsibleParty {} not found", domain);
        }
        return result;
    }

    private RestResult deleteDistributionSystemOperator(String domain) {
        RestResult result = RestResultFactory.getJsonRestResult();
        try {
            validationService.checkExistingDistributionSystemOperatorDomain(domain);
            distributionSystemOperatorRepository.deleteByDomain(domain);
            result.setCode(HttpResponseCodes.SC_OK);
            LOGGER.info("DistributionSystemOperator {} deleted", domain);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add(e.getMessage());
            LOGGER.info("DistributionSystemOperator {} not found", domain);
        }
        return result;
    }

    private RestResult deleteCommonReferenceOperator(String domain) {
        RestResult result = RestResultFactory.getJsonRestResult();
        try {
            validationService.checkExistingCommonReferenceOperatorDomain(domain);
            commonReferenceOperatorRepository.deleteByDomain(domain);
            result.setCode(HttpResponseCodes.SC_OK);
            LOGGER.info("CommonReferenceOperator {} deleted", domain);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add(e.getMessage());
            LOGGER.info("CommonReferenceOperator {} not found", domain);
        }
        return result;
    }
}
