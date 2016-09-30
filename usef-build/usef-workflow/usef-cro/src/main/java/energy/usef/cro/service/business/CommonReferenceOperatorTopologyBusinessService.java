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
package energy.usef.cro.service.business;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import energy.usef.core.config.Role;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.rest.Participant;
import energy.usef.core.rest.RestResult;
import energy.usef.core.rest.RestResultFactory;
import energy.usef.core.util.JsonUtil;
import energy.usef.cro.dto.ParticipantAction;
import energy.usef.cro.model.Aggregator;
import energy.usef.cro.model.BalanceResponsibleParty;
import energy.usef.cro.model.DistributionSystemOperator;
import energy.usef.cro.model.MeterDataCompany;
import energy.usef.cro.repository.AggregatorRepository;
import energy.usef.cro.repository.BalanceResponsiblePartyRepository;
import energy.usef.cro.repository.DistributionSystemOperatorRepository;
import energy.usef.cro.repository.MeterDataCompanyRepository;
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

import static energy.usef.core.config.Role.*;
import static energy.usef.core.util.JsonUtil.ROOT_KEY;
import static energy.usef.core.util.JsonUtil.createJsonText;

/**
 * This service class implements the business logic related to the CRO part of the common reference query.
 */
@Stateless
public class CommonReferenceOperatorTopologyBusinessService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonReferenceOperatorTopologyBusinessService.class);

    @Inject
    AggregatorRepository aggregatorRepository;

    @Inject
    BalanceResponsiblePartyRepository balanceResponsiblePartyRepository;

    @Inject
    DistributionSystemOperatorRepository distributionSystemOperatorRepository;

    @Inject
    MeterDataCompanyRepository meterDataCompanyRepository;

    @Inject
    CommonReferenceOperatorValidationBusinessService validationService;

    /**
     * Try and retrieve an {@link Aggregator} with the given domain name, returning it in a {@Link RestResult}.
     *
     * @param domain a {@link String} containing a domain name of the {@link Aggregator} to be retrieved.
     * @return a {@Link RestResult}
     */
    public RestResult findAggregator(String domain) throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();
        Aggregator participant = aggregatorRepository.findByDomain(domain);

        if (participant != null) {
            result.setCode(HttpResponseCodes.SC_OK);
            result.setBody(createJsonText(new Participant(participant.getId(), participant.getDomain())));
        } else {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add("Aggregator " + domain + " not found.");
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
        BalanceResponsibleParty participant = balanceResponsiblePartyRepository.findByDomain(domain);

        if (participant != null) {
            result.setCode(HttpResponseCodes.SC_OK);
            result.setBody(createJsonText(new Participant(participant.getId(), participant.getDomain())));
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
        DistributionSystemOperator participant = distributionSystemOperatorRepository.findByDomain(domain);

        if (participant != null) {
            result.setCode(HttpResponseCodes.SC_OK);
            result.setBody(createJsonText(new Participant(participant.getId(), participant.getDomain())));
        } else {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add("DistributionSystemOperator " + domain + " not found.");
        }
        return result;
    }

    /**
     * Try and retrieve an {@link MeterDataCompany} with the given domain name, returning it as a {@Link RestResult}.
     *
     * @param domain a {@link String} containing a domain name of the {@link MeterDataCompany} to be retrieved.
     * @return a {@Link RestResult}
     */
    public RestResult findMeterDataCompany(String domain) throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();
        MeterDataCompany participant = meterDataCompanyRepository.findByDomain(domain);

        if (participant != null) {

            result.setCode(HttpResponseCodes.SC_OK);
            result.setBody(createJsonText(new Participant(participant.getId(), participant.getDomain())));
        } else {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add("MeterDataCompany " + domain + " not found.");
        }
        return result;
    }

    /*
     * Methods to find individual Participants
     */

    /**
     * Try and retrieve all {@link Aggregator} instances.
     */
    public RestResult findAllAggregators() throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();

        List<Participant> participantList = new ArrayList<>();
        aggregatorRepository.findAll().stream().forEach(a -> {
            Participant participant = new Participant();
            participant.setId(a.getId());
            participant.setDomain(a.getDomain());
            participantList.add(participant);
        });
        result.setCode(HttpResponseCodes.SC_OK);
        result.setBody(createJsonText(participantList));

        return result;
    }

    /**
     * Try and retrieve all{@link BalanceResponsibleParty} instances.
     */
    public RestResult findAllBalanceResponsibleParties() throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();

        List<Participant> participantList = new ArrayList<>();
        balanceResponsiblePartyRepository.findAll().stream().forEach(a -> {
            Participant participant = new Participant();
            participant.setId(a.getId());
            participant.setDomain(a.getDomain());
            participantList.add(participant);
        });
        result.setCode(HttpResponseCodes.SC_OK);
        result.setBody(createJsonText(participantList));

        return result;
    }

    /**
     * Try and retrieve all{@link DistributionSystemOperator} instances.
     */
    public RestResult findAllDistributionSystemOperators() throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();

        List<Participant> participantList = new ArrayList<>();
        distributionSystemOperatorRepository.findAll().stream().forEach(a -> {
            Participant participant = new Participant();
            participant.setId(a.getId());
            participant.setDomain(a.getDomain());
            participantList.add(participant);
        });
        result.setCode(HttpResponseCodes.SC_OK);
        result.setBody(createJsonText(participantList));

        return result;
    }

    /**
     * Try and retrieve all{@link MeterDataCompany} instances.
     */
    public RestResult findAllMeterDataCompanies() throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();

        List<Participant> participantList = new ArrayList<>();
        meterDataCompanyRepository.findAll().stream().forEach(a -> {
            Participant participant = new Participant();
            participant.setId(a.getId());
            participant.setDomain(a.getDomain());
            participantList.add(participant);
        });
        result.setCode(HttpResponseCodes.SC_OK);
        result.setBody(createJsonText(participantList));

        return result;
    }

    /**
     * Process a batch of {@Link Aggregator} updates.
     *
     * @param jsonText a json {@link String} containing a batch of {@Link Aggregator} updates.
     * @return a {@Link Response} message containing batch update results
     */

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<RestResult> processAggregatorBatch(String jsonText) throws IOException, ProcessingException {
        LOGGER.info("Start processing Aggregator batch.");
        return processParticipantBatch(AGR, jsonText);
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
     * Process a batch of {@Link MeterDataCompany} updates.
     *
     * @param jsonText a json {@link String} containing a batch of {@Link MeterDataCompany} updates.
     * @return a {@Link Response} message containing batch update results
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<RestResult> processMeterDataCompanyBatch(String jsonText) throws IOException, ProcessingException {
        LOGGER.info("Start processing a MeterDataCompany batch");
        return processParticipantBatch(MDC, jsonText);
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
            result = JsonUtil.notSupported(method, "Participant");
        }
        return result;
    }

    private RestResult findParticipant(Role role, String domain) throws IOException {
        RestResult result = new RestResult();
        result.setCode(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR);

        switch (role) {
        case AGR:
            result = findAggregator(domain);
            break;
        case BRP:
            result = findBalanceResponsibleParty(domain);
            break;
        case DSO:
            result = findDistributionSystemOperator(domain);
            break;
        case MDC:
            result = findMeterDataCompany(domain);
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
        case AGR:
            result = createAggregator(domain);
            break;
        case BRP:
            result = createBalanceResponsibleParty(domain);
            break;
        case DSO:
            result = createDistributionSystemOperator(domain);
            break;
        case MDC:
            result = createMeterDataCompany(domain);
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
        case AGR:
            result = deleteAggregator(domain);
            break;
        case BRP:
            result = deleteBalanceResponsibleParty(domain);
            break;
        case DSO:
            result = deleteDistributionSystemOperator(domain);
            break;
        case MDC:
            result = deleteMeterDataCompany(domain);
            break;
        default:
            result = JsonUtil.unknownRole(role.toString());
        }
        return result;
    }

    private RestResult createAggregator(String domain) {
        RestResult result = RestResultFactory.getJsonRestResult();
        try {
            validationService.checkDuplicateAggregatorDomain(domain);

            Aggregator participant = new Aggregator();
            participant.setDomain(domain);
            aggregatorRepository.persist(participant);

            result.setCode(HttpResponseCodes.SC_CREATED);
            LOGGER.info("Aggregator {} created", domain);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_CONFLICT);
            result.getErrors().add(e.getMessage());
            LOGGER.info("Duplicate Aggregator {} not created", domain);
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

    private RestResult createMeterDataCompany(String domain) {
        RestResult result = RestResultFactory.getJsonRestResult();
        try {
            validationService.checkDuplicateMeterDataCompanyDomain(domain);

            MeterDataCompany participant = new MeterDataCompany();
            participant.setDomain(domain);
            meterDataCompanyRepository.persist(participant);

            result.setCode(HttpResponseCodes.SC_CREATED);
            LOGGER.info("MeterDataCompany {} created", domain);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_CONFLICT);
            result.getErrors().add(e.getMessage());
            LOGGER.info("Duplicate MeterDataCompany {} not created", domain);
        }
        return result;
    }

    private RestResult deleteAggregator(String domain) {
        RestResult result = RestResultFactory.getJsonRestResult();
        try {
            validationService.checkExistingAggregatorDomain(domain);
            aggregatorRepository.deleteByDomain(domain);
            result.setCode(HttpResponseCodes.SC_OK);
            LOGGER.info("Aggregator {} deleted", domain);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add(e.getMessage());
            LOGGER.info("Aggregator {} not found", domain);
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

    private RestResult deleteMeterDataCompany(String domain) {
        RestResult result = RestResultFactory.getJsonRestResult();
        try {
            validationService.checkExistingMeterDataCompanyDomain(domain);
            meterDataCompanyRepository.deleteByDomain(domain);
            result.setCode(HttpResponseCodes.SC_OK);
            LOGGER.info("MeterDataCompany {} deleted", domain);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add(e.getMessage());
            LOGGER.info("MeterDataCompany {} not found", domain);
        }
        return result;
    }
}
