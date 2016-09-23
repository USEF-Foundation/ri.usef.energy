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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import energy.usef.brp.dto.ParticipantAction;
import energy.usef.brp.model.CommonReferenceOperator;
import energy.usef.brp.repository.CommonReferenceOperatorRepository;
import energy.usef.core.config.Role;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.rest.Participant;
import energy.usef.core.rest.RestResult;
import energy.usef.core.rest.RestResultFactory;
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
import java.io.StringWriter;
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
public class BalanceResponsiblePartyTopologyBusinessService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BalanceResponsiblePartyTopologyBusinessService.class);

    @Inject
    CommonReferenceOperatorRepository commonReferenceOperatorRepository;

    @Inject
    BalanceResponsiblePartyValidationBusinessService validationService;

    public BalanceResponsiblePartyTopologyBusinessService() {
    }

    /**
     * Try and retrieve an {@link CommonReferenceOperator} with the given domain name, returning it in a {@Link RestResult}.
     *
     * @param domain a {@link String} containing a domain name of the {@link CommonReferenceOperator} to be retrieved.
     * @return a {@Link RestResult}
     */
    public RestResult findCommonReferenceOperator(String domain) throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();
        CommonReferenceOperator participant = commonReferenceOperatorRepository.findByDomain(domain);

        if (participant != null) {
            result.setCode(HttpResponseCodes.SC_OK);
            result.setBody(createJsonText(new Participant(participant.getId(), participant.getDomain())));
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

        List<Participant> participantList = new ArrayList<>();
        commonReferenceOperatorRepository.findAll().stream().forEach(a -> {
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
     * Process a batch of {@Link CommonReferenceOperator} updates.
     *
     * @param jsonText a json {@link String} containing a batch of {@Link CommonReferenceOperator} updates.
     * @return a {@Link Response} message containing batch update results
     */

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<RestResult> processCommonReferenceOperatorBatch(String jsonText) throws IOException, ProcessingException {
        LOGGER.info("Start processing Common Reference Operator batch.");
        return processParticipantBatch(CRO, jsonText);
    }

    private List<RestResult> processParticipantBatch(Role role, String jsonText) throws IOException, ProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(jsonText);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Map<Integer, RestResult> resultMap = new HashMap<>();

        JsonFactory factory = new JsonFactory();
        StringWriter stringWriter = new StringWriter();

        JsonGenerator generator = factory.createGenerator(stringWriter);
        generator.writeStartArray();

        JsonUtil.validateNodeSyntax("/participant-schema.json", root, resultMap);

        if (!resultMap.containsKey(ROOT_KEY)) {
            LOGGER.error("Valid Json message");
            List<ParticipantAction> actions = objectMapper.readValue(jsonText, new TypeReference<List<ParticipantAction>>() {
            });

            // Bow process all the actions that have the correct syntax.
            for (int entry = 0; entry < actions.size(); entry++) {
                if (!resultMap.containsKey(entry)) {
                    resultMap.put(entry, processParticipantNode(role, actions.get(entry)));
                }
            }
        }

        List<RestResult> result = resultMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(a -> a.getValue())
                .collect(Collectors.toList());
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
        }
        return result;
    }

    private RestResult findParticipant(Role role, String domain) throws IOException {
        RestResult result = new RestResult();
        result.setCode(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR);

        switch (role) {
        case CRO:
            result = findCommonReferenceOperator(domain);
            break;
        }
        return result;
    }

    private RestResult createParticipant(Role role, String domain) throws IOException {
        RestResult result = new RestResult();
        result.setCode(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR);

        switch (role) {
        case CRO:
            result = createCommonReferenceOperator(domain);
            break;
        }
        return result;
    }

    private RestResult deleteParticipant(Role role, String domain) throws IOException {
        RestResult result = new RestResult();
        result.setCode(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR);

        switch (role) {
        case CRO:
            result = deleteCommonReferenceOperator(domain);
            break;
        }
        return result;
    }

    /**
     * Try and create an {@link CommonReferenceOperator} with the given domain name.
     *
     * @param domain a {@link String} containing a domain name of the {@link CommonReferenceOperator} to be created.
     * @return a {@Link RestResult}
     */
    private RestResult createCommonReferenceOperator(String domain) {
        RestResult result = RestResultFactory.getJsonRestResult();
        try {
            validationService.checkDuplicateCommonReferenceOperatorDomain(domain);

            CommonReferenceOperator participant = new CommonReferenceOperator();
            participant.setDomain(domain);
            commonReferenceOperatorRepository.persist(participant);

            result.setCode(HttpResponseCodes.SC_CREATED);
            LOGGER.info("Common Reference Operator {} created", domain);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_CONFLICT);
            result.getErrors().add(e.getMessage());
            LOGGER.info("Duplicate Common Reference Operator {} not created", domain);
        }
        return result;
    }

    /**
     * Try and delete an {@link CommonReferenceOperator} with the given domain name.
     *
     * @param domain a {@link String} containing a domain name of the {@link CommonReferenceOperator} to be deleted.
     * @return a {@Link RestResult}
     */
    private RestResult deleteCommonReferenceOperator(String domain) {
        RestResult result = RestResultFactory.getJsonRestResult();
        try {
            validationService.checkExistingCommonReferenceOperatorDomain(domain);
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
}
