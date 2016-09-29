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
package energy.usef.dso.service.business;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.rest.Participant;
import energy.usef.core.rest.RestResult;
import energy.usef.core.rest.RestResultFactory;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.JsonUtil;
import energy.usef.dso.dto.SynchronisationCongestionPointDto;
import energy.usef.dso.dto.SynchronisationConnectionDto;
import energy.usef.dso.model.CommonReferenceOperator;
import energy.usef.dso.model.SynchronisationCongestionPoint;
import energy.usef.dso.model.SynchronisationCongestionPointStatus;
import energy.usef.dso.model.SynchronisationConnection;
import energy.usef.dso.model.SynchronisationConnectionStatusType;
import energy.usef.dso.repository.CommonReferenceOperatorRepository;
import energy.usef.dso.repository.SynchronisationCongestionPointRepository;
import energy.usef.dso.repository.SynchronisationCongestionPointStatusRepository;
import energy.usef.dso.repository.SynchronisationConnectionRepository;
import energy.usef.dso.workflow.dto.CongestionPointActionDto;
import energy.usef.dso.workflow.dto.ParticipantActionDto;
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
public class DistributionSystemOperatorTopologyBusinessService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionSystemOperatorTopologyBusinessService.class);

    private static final String JSON_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    @Inject
    CommonReferenceOperatorRepository commonReferenceOperatorRepository;

    @Inject
    DistributionSystemOperatorValidationBusinessService validationService;

    @Inject
    SynchronisationConnectionRepository synchronisationConnectionRepository;

    @Inject
    SynchronisationCongestionPointRepository synchronisationCongestionPointRepository;

    @Inject
    SynchronisationCongestionPointStatusRepository synchronisationCongestionPointStatusRepository;

    /**
     * Try and retrieve an {@link CommonReferenceOperator} with the given domain name, returning it in a {@Link RestResult}.
     *
     * @param domain a {@link String} containing the domain name of the {@link CommonReferenceOperator} to be retrieved.
     * @return a {@Link RestResult}
     */
    public RestResult findCommonReferenceOperator(String domain) throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();
        CommonReferenceOperator object = commonReferenceOperatorRepository.findByDomain(domain);

        if (object != null) {
            result.setCode(HttpResponseCodes.SC_OK);
            result.setBody(createJsonText(new Participant(object.getId(), object.getDomain())));
        } else {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add("Common Reference Operator " + domain + " not found.");
        }
        return result;
    }

    /**
     * Try and retrieve an {@link SynchronisationCongestionPoint} with the given domain name, returning it in a {@Link RestResult}.
     *
     * @param entityAddress a {@link String} containing the entity address of the {@link SynchronisationCongestionPoint} to be retrieved.
     * @return a {@Link RestResult}
     */
    public RestResult findSynchronisationCongestionPoint(String entityAddress) throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();
        SynchronisationCongestionPoint object = synchronisationCongestionPointRepository.findByEntityAddress(entityAddress);

        if (object != null) {
            result.setCode(HttpResponseCodes.SC_OK);
            SynchronisationCongestionPointDto congestionPointDto = new SynchronisationCongestionPointDto(object.getId(), object.getEntityAddress(), DateTimeUtil
                    .printDateTime(object.getLastModificationTime(),JSON_DATE_TIME_FORMAT));

            object.getConnections().stream().forEach(b->{
                SynchronisationConnectionDto connectionDto = new SynchronisationConnectionDto();
                connectionDto.setId(b.getId());
                connectionDto.setEntityAddress(b.getEntityAddress());

                congestionPointDto.getConnections().add(connectionDto);
            });

            result.setBody(createJsonText(congestionPointDto));
        } else {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add("Synchronisation Congestion Point " + entityAddress + " not found.");
        }
        return result;
    }

    /**
     * Try and retrieve all {@link SynchronisationCongestionPoint} instances.
     */
    public RestResult findAllSynchronisationCongestionPoints() throws IOException {
        RestResult result = RestResultFactory.getJsonRestResult();

        List<SynchronisationCongestionPointDto> dtoList = new ArrayList<>();
        synchronisationCongestionPointRepository.findAll().stream().forEach(a -> {
            SynchronisationCongestionPointDto congestionPointDto = new SynchronisationCongestionPointDto(a.getId(), a.getEntityAddress(), DateTimeUtil
                    .printDateTime(a.getLastModificationTime(),JSON_DATE_TIME_FORMAT));

            a.getConnections().stream().forEach(b->{
                SynchronisationConnectionDto connectionDto = new SynchronisationConnectionDto();
                connectionDto.setId(b.getId());
                connectionDto.setEntityAddress(b.getEntityAddress());

                congestionPointDto.getConnections().add(connectionDto);
            });
            dtoList.add(congestionPointDto);
        });
        result.setCode(HttpResponseCodes.SC_OK);
        result.setBody(createJsonText(dtoList));

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
     * Process a batch of {@Link SynchronisationCongestionPoint} updates.
     *
     * @param jsonText a json {@link String} containing a batch of {@Link SynchronisationCongestionPoint} updates.
     * @return a {@Link Response} message containing batch update results
     */

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<RestResult> processSynchronisationCongestionPointBatch(String jsonText) throws IOException, ProcessingException {
        LOGGER.info("Start processing Synchronisation CongestionPoint batch.");
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(jsonText);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Map<Integer, RestResult> resultMap = new HashMap<>();

        JsonUtil.validateNodeSyntax("/congestion-point-schema.json", root, resultMap);

        if (!resultMap.containsKey(ROOT_KEY)) {
            LOGGER.error("Valid Json message");
            List<CongestionPointActionDto> actions = objectMapper.readValue(jsonText, new TypeReference<List<CongestionPointActionDto>>() {
            });

            List<CommonReferenceOperator> existingCommonReferenceOperators = commonReferenceOperatorRepository.findAll();


            // Now process all the actions that have the correct syntax.
            for (int entry = 0; entry < actions.size(); entry++) {
                if (!resultMap.containsKey(entry)) {
                    resultMap.put(entry, processSynchronisationCongestionPointNode(actions.get(entry), existingCommonReferenceOperators));
                }
            }
        }

        return resultMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(a -> a.getValue()).collect(Collectors.toList());
    }
    /**
     * Process a batch of {@Link CommonReferenceOperator} updates.
     *
     * @param jsonText a json {@link String} containing a batch of {@Link CommonReferenceOperator} updates.
     * @return a {@Link Response} message containing batch update results
     */

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<RestResult> processCommonReferenceOperatorBatch(String jsonText) throws IOException, ProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(jsonText);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Map<Integer, RestResult> resultMap = new HashMap<>();
        JsonUtil.validateNodeSyntax("/participant-schema.json", root, resultMap);

        if (!resultMap.containsKey(ROOT_KEY)) {
            LOGGER.error("Valid Json message");
            List<ParticipantActionDto> actions = objectMapper.readValue(jsonText, new TypeReference<List<ParticipantActionDto>>() {
            });

            List<SynchronisationCongestionPoint> existingSynchronisationCongestionPoints = synchronisationCongestionPointRepository.findAll();

            // Now process all the actions that have the correct syntax.
            for (int entry = 0; entry < actions.size(); entry++) {
                if (!resultMap.containsKey(entry)) {
                    resultMap.put(entry, processCommonReferenceOperatorNode(actions.get(entry), existingSynchronisationCongestionPoints));
                }
            }
        }

        return resultMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(a -> a.getValue()).collect(Collectors.toList());
    }

    private RestResult processCommonReferenceOperatorNode(ParticipantActionDto action, List<SynchronisationCongestionPoint> existingSynchronisationCongestionPoints) throws IOException {
        String method = action.getMethod();
        String domain = action.getDomain();
        RestResult result = new RestResult();
        result.setCode(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR);

        switch (method) {
        case HttpMethod.GET:
            result = findAllCommonReferenceOperators();
            break;
        case HttpMethod.POST:
            result = createCommonReferenceOperator(domain, existingSynchronisationCongestionPoints);
            break;
        case HttpMethod.DELETE:
            result = deleteCommonReferenceOperator(domain);
            break;
        default:
            result = JsonUtil.notSupported(method, "CommonReferenceOperator");
        }
        return result;
    }

    private RestResult createCommonReferenceOperator(String domain, List<SynchronisationCongestionPoint> existingSynchronisationCongestionPoints) {
        RestResult result = RestResultFactory.getJsonRestResult();
        try {
            validationService.checkDuplicateCommonReferenceOperatorDomain(domain);

            CommonReferenceOperator participant = new CommonReferenceOperator();
            participant.setDomain(domain);
            commonReferenceOperatorRepository.persist(participant);

            existingSynchronisationCongestionPoints.forEach(synchronisationConnection -> {
                SynchronisationCongestionPointStatus status = new SynchronisationCongestionPointStatus();
                status.setSynchronisationCongestionPoint(synchronisationConnection);
                status.setCommonReferenceOperator(participant);
                status.setStatus(SynchronisationConnectionStatusType.MODIFIED);
                synchronisationCongestionPointStatusRepository.persist(status);
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
            CommonReferenceOperator cro = commonReferenceOperatorRepository.findByDomain(domain);
            synchronisationCongestionPointStatusRepository.deleteFor(cro);

            commonReferenceOperatorRepository.delete(cro);
            result.setCode(HttpResponseCodes.SC_OK);
            LOGGER.info("Common Reference Operator {} deleted", domain);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add(e.getMessage());
            LOGGER.info("Common Reference Operator {} not found", domain);
        }
        return result;
    }

    private RestResult processSynchronisationCongestionPointNode(CongestionPointActionDto action, List<CommonReferenceOperator> existingCommonReferenceOperators) throws IOException {
        String method = action.getMethod();
        String entityAddress = action.getEntityAddress();
        RestResult result = new RestResult();
        result.setCode(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR);

        switch (method) {
        case HttpMethod.GET:
            result = findAllSynchronisationCongestionPoints();
            break;
        case HttpMethod.POST:
            result = createSynchronisationCongestionPoint(action, existingCommonReferenceOperators);
            break;
        case HttpMethod.PUT:
            result = updateSynchronisationCongestionPoint(action, existingCommonReferenceOperators);
            break;
        case HttpMethod.DELETE:
            result = deleteSynchronisationCongestionPoint(entityAddress);
            break;
        default:
            result = JsonUtil.notSupported(method, "SynchronisationCongestionPoint");
        }
        return result;
    }

     private RestResult createSynchronisationCongestionPoint(CongestionPointActionDto action, List<CommonReferenceOperator> existingCommonReferenceOperators) {
        RestResult result = RestResultFactory.getJsonRestResult();
        String entityAddress = action.getEntityAddress();
        try {
            validationService.checkDuplicateSynchronisationCongestionPoint(entityAddress);
            validationService.checkDuplicateSynchronisationConnections(action.getConnections().stream().map(e -> e.getEntityAddress()).collect(Collectors.toList()));

            SynchronisationCongestionPoint synchronisationCongestionPoint = new SynchronisationCongestionPoint();
            synchronisationCongestionPoint.setEntityAddress(entityAddress);
            synchronisationCongestionPoint.setLastModificationTime(DateTimeUtil.getCurrentDateTime());
            synchronisationCongestionPointRepository.persist(synchronisationCongestionPoint);

            createChildren(action, existingCommonReferenceOperators, synchronisationCongestionPoint);
            result.setCode(HttpResponseCodes.SC_CREATED);
            LOGGER.info("Synchronisation Congestion Point {} created", entityAddress);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_CONFLICT);
            result.getErrors().add(e.getMessage());
            LOGGER.info("Duplicate Synchronisation Congestion Point {} not created", entityAddress);
        }
        return result;
    }

    private RestResult updateSynchronisationCongestionPoint(CongestionPointActionDto action, List<CommonReferenceOperator> existingCommonReferenceOperators) {
        RestResult result = RestResultFactory.getJsonRestResult();
        String entityAddress = action.getEntityAddress();
        try {
            validationService.checkExistingSynchronisationCongestionPoint(entityAddress);
            SynchronisationCongestionPoint synchronisationCongestionPoint = synchronisationCongestionPointRepository.findByEntityAddress(entityAddress);

            synchronisationCongestionPoint.setLastModificationTime(DateTimeUtil.getCurrentDateTime());

            // Remove existing synchronisationConnections and synchronisationCongestionStatuses
            synchronisationConnectionRepository.deleteFor(synchronisationCongestionPoint);
            synchronisationCongestionPointStatusRepository.deleteFor(synchronisationCongestionPoint);

            validationService.checkDuplicateSynchronisationConnections(action.getConnections().stream().map(e -> e.getEntityAddress()).collect(Collectors.toList()));

            createChildren(action, existingCommonReferenceOperators, synchronisationCongestionPoint);

            result.setCode(HttpResponseCodes.SC_OK);
            LOGGER.info("Synchronisation Congestion Point {} updated", entityAddress);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add(e.getMessage());
            LOGGER.info("uplicate Synchronisation Congestion Point {} not found", entityAddress);
        }
        return result;
    }

    private void createChildren(CongestionPointActionDto action, List<CommonReferenceOperator> existingCommonReferenceOperators,
            SynchronisationCongestionPoint synchronisationCongestionPoint) {
        // Create new synchronisationConnections and synchronisationCongestionStatuses
        action.getConnections().stream().forEach(scDto -> {
            SynchronisationConnection sc = new SynchronisationConnection();
            sc.setCongestionPoint(synchronisationCongestionPoint);
            sc.setEntityAddress(scDto.getEntityAddress());
            synchronisationConnectionRepository.persist(sc);
        });

        existingCommonReferenceOperators.forEach(cro -> {
            SynchronisationCongestionPointStatus status = new SynchronisationCongestionPointStatus();
            status.setSynchronisationCongestionPoint(synchronisationCongestionPoint);
            status.setCommonReferenceOperator(cro);
            status.setStatus(SynchronisationConnectionStatusType.MODIFIED);
            synchronisationCongestionPointStatusRepository.persist(status);
        });
    }

    private RestResult deleteSynchronisationCongestionPoint(String entityAddress) {
        RestResult result = RestResultFactory.getJsonRestResult();
        try {
            validationService.checkExistingSynchronisationCongestionPoint(entityAddress);

            SynchronisationCongestionPoint scp = synchronisationCongestionPointRepository.findByEntityAddress(entityAddress);

            synchronisationConnectionRepository.deleteFor(scp);
            synchronisationCongestionPointStatusRepository.deleteFor(scp);
            synchronisationCongestionPointRepository.delete(scp);
            result.setCode(HttpResponseCodes.SC_OK);
            LOGGER.info("Synchronisation Congestion Point {} deleted", entityAddress);
        } catch (BusinessValidationException e) {
            result.setCode(HttpResponseCodes.SC_NOT_FOUND);
            result.getErrors().add(e.getMessage());
            LOGGER.info("Synchronisation Congestion Point {} not found", entityAddress);
        }
        return result;
    }
}
