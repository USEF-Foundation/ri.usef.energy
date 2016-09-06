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

package energy.usef.cro.controller;

import static energy.usef.core.data.xml.bean.message.MessagePrecedence.ROUTINE;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.controller.BaseIncomingMessageController;
import energy.usef.core.data.xml.bean.message.Aggregator;
import energy.usef.core.data.xml.bean.message.CommonReferenceEntityType;
import energy.usef.core.data.xml.bean.message.CommonReferenceQuery;
import energy.usef.core.data.xml.bean.message.CommonReferenceQueryResponse;
import energy.usef.core.data.xml.bean.message.CongestionPoint;
import energy.usef.core.data.xml.bean.message.DispositionSuccessFailure;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.Message;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.XMLUtil;
import energy.usef.cro.config.ConfigCro;
import energy.usef.cro.config.ConfigCroParam;
import energy.usef.cro.model.Connection;
import energy.usef.cro.service.business.CommonReferenceMode;
import energy.usef.cro.service.business.CommonReferenceUpdateBusinessService;
import energy.usef.cro.service.business.CommonReferenceQueryBusinessService;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes incoming CommonReferenceQuery messages.
 */
@Stateless
public class CommonReferenceQueryController extends BaseIncomingMessageController<CommonReferenceQuery> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonReferenceQueryController.class);

    @Inject
    private JMSHelperService jmsHelperService;

    @Inject
    private CommonReferenceUpdateBusinessService commonReferenceUpdateBusinessService;

    @Inject
    private CommonReferenceQueryBusinessService commonReferenceQueryBusinessService;

    @Inject
    private Config config;

    @Inject
    private ConfigCro configCro;

    /**
     * {@inheritDoc}
     */
    public void action(CommonReferenceQuery request, Message savedMessage) throws BusinessException {
        CommonReferenceQueryResponse response = new CommonReferenceQueryResponse();

        MessageMetadata requestMetadata = request.getMessageMetadata();
        MessageMetadataBuilder messageMetadataBuilder = MessageMetadataBuilder.build(requestMetadata.getSenderDomain(),
                requestMetadata.getSenderRole(), config.getProperty(ConfigParam.HOST_DOMAIN), USEFRole.CRO,
                ROUTINE).conversationID(requestMetadata.getConversationID());

        response.setMessageMetadata(messageMetadataBuilder.build());

        boolean closedMode = CommonReferenceMode.CLOSED.value().equals(configCro.getProperty(ConfigCroParam.COMMON_REFERENCE_MODE));

        LOGGER.debug("Start processing CommonReferenceQuery request of Type {} from {} in {} mode", request.getEntity(),
                request.getMessageMetadata().getSenderRole(), configCro.getProperty(ConfigCroParam.COMMON_REFERENCE_MODE));

        if (CommonReferenceEntityType.AGGREGATOR.equals(request.getEntity())) {
            // BRP/MDC -> AGGREGATOR QUERY = Connections with AGR Domain
            handleAggregatorQuery(request, response, closedMode);
        } else if (CommonReferenceEntityType.CONGESTION_POINT.equals(request.getEntity())) {
            // DSO -> CONGESTION_POINT QUERY = CongestionPoints with Aggregators
            // AGR -> CONGESTION_POINT QUERY = CongestionPoint with it's connections and the DSO per congestionPoint
            handleCongestionPointQuery(request, response, closedMode);
        } else if (CommonReferenceEntityType.BRP.equals(request.getEntity())) {
            // AGR -> BRP QUERY = Connections with BRP Domain
            handleBRPQuery(request, response, closedMode);
        }

        LOGGER.debug("CommonReferenceQuery request processed, sending message to out queue");
        jmsHelperService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(response));
    }

    private void handleBRPQuery(CommonReferenceQuery request, CommonReferenceQueryResponse response, boolean closedMode) {
        if (USEFRole.AGR == request.getMessageMetadata().getSenderRole()) {
            processAGRBRPRequest(request, response, closedMode);
        } else {
            roleNotSupportedResponse(response);
        }
    }

    private void handleCongestionPointQuery(CommonReferenceQuery request, CommonReferenceQueryResponse response,
            boolean closedMode) {
        if (USEFRole.DSO.equals(request.getMessageMetadata().getSenderRole())) {
            processDSOCongestionPointRequest(request, response, closedMode);
        } else if (USEFRole.AGR.equals(request.getMessageMetadata().getSenderRole())) {
            processAGRCongestionPointRequest(request, response, closedMode);
        } else {
            roleNotSupportedResponse(response);
        }
    }

    private void handleAggregatorQuery(CommonReferenceQuery request, CommonReferenceQueryResponse response, boolean closedMode) {
        if (USEFRole.BRP.equals(request.getMessageMetadata().getSenderRole())) {
            processBRPAggregatorRequest(request, response, closedMode);
        } else if (USEFRole.MDC.equals(request.getMessageMetadata().getSenderRole())) {
            processMDCAggregatorRequest(request, response, closedMode);
        } else {
            roleNotSupportedResponse(response);
        }
    }

    private void processAGRBRPRequest(CommonReferenceQuery request, CommonReferenceQueryResponse response, boolean closedMode) {
        if (!closedMode || validateRegistrationOfAgrSender(request, response)) {
            List<Connection> connections = commonReferenceQueryBusinessService.findAllConnectionsForAggregatorDomain(
                    request.getMessageMetadata().getSenderDomain());
            // Build success response
            buildBrpConnections(response, connections);
            response.setResult(DispositionSuccessFailure.SUCCESS);
        }
    }

    private void processAGRCongestionPointRequest(CommonReferenceQuery request, CommonReferenceQueryResponse response,
            boolean closedMode) {
        if (!closedMode || validateRegistrationOfAgrSender(request, response)) {
            // Retrieve data
            Map<energy.usef.cro.model.CongestionPoint, Set<Connection>> results = commonReferenceUpdateBusinessService
                    .findConnectionsForCongestionPointsByAGR(
                            request.getMessageMetadata().getSenderDomain(), request.getEntityAddress());

            // Build success response
            buildResponseToAGR(response, results);
            response.setResult(DispositionSuccessFailure.SUCCESS);
        }
    }

    private void processDSOCongestionPointRequest(CommonReferenceQuery request, CommonReferenceQueryResponse response,
            boolean closedMode) {
        if (closedMode && commonReferenceUpdateBusinessService.findDistributionSystemOperatorByDomain(
                request.getMessageMetadata().getSenderDomain()) == null) {
            // DSO not registered, failure
            response.setResult(DispositionSuccessFailure.FAILURE);
            response.setMessage("DSO is not registered");
            LOGGER.error(response.getMessage());
        } else {
            // Retrieve data
            Map<energy.usef.cro.model.CongestionPoint, Map<energy.usef.cro.model.Aggregator, Long>> results =
                    commonReferenceUpdateBusinessService
                            .findCongestionPointsWithAggregatorsByDSO(request.getMessageMetadata().getSenderDomain(),
                                    request.getEntityAddress());

            // Build success response
            buildResponseToDSO(response, results);
            response.setResult(DispositionSuccessFailure.SUCCESS);
        }
    }

    private void processBRPAggregatorRequest(CommonReferenceQuery request, CommonReferenceQueryResponse response,
            boolean closedMode) {
        if (!closedMode || commonReferenceUpdateBusinessService.findBRP(request.getMessageMetadata().getSenderDomain()) != null) {
            // Retrieve data
            List<Connection> connections = commonReferenceQueryBusinessService.findAllConnectionsForBRPDomain(
                    request.getMessageMetadata().getSenderDomain());

            // Build success response
            buildAgrConnections(response, connections);
            response.setResult(DispositionSuccessFailure.SUCCESS);
        }
    }

    private void processMDCAggregatorRequest(CommonReferenceQuery request, CommonReferenceQueryResponse response,
            boolean closedMode) {
        if (!closedMode || commonReferenceUpdateBusinessService.findMDC(request.getMessageMetadata().getSenderDomain()) != null) {

            // Retrieve data
            List<Connection> connections = commonReferenceQueryBusinessService.findAllConnectionsForMDCDomain().stream()
                    .filter(p -> request.getConnectionEntityAddress().contains(p.getEntityAddress())).collect(Collectors.toList());

            // Build success response
            buildMdcConnections(response, connections);
            response.setResult(DispositionSuccessFailure.SUCCESS);
        }
    }

    private void buildAgrConnections(CommonReferenceQueryResponse response, List<Connection> connections) {
        for (Connection connection : connections) {
            energy.usef.core.data.xml.bean.message.Connection connectionDto = new energy.usef.core.data.xml.bean.message.Connection();
            connectionDto.setEntityAddress(connection.getEntityAddress());
            connectionDto.setAGRDomain(connection.getAggregator() == null ? null : connection.getAggregator().getDomain());
            response.getConnection().add(connectionDto);
        }
    }

    private void buildMdcConnections(CommonReferenceQueryResponse response, List<Connection> connections) {
        for (Connection connection : connections) {
            energy.usef.core.data.xml.bean.message.Connection connectionDto = new energy.usef.core.data.xml.bean.message.Connection();
            connectionDto.setEntityAddress(connection.getEntityAddress());
            connectionDto.setAGRDomain(connection.getAggregator() == null ? null : connection.getAggregator().getDomain());
            connectionDto.setBRPDomain(connection.getBalanceResponsibleParty() == null ? null : connection.getBalanceResponsibleParty().getDomain());
            response.getConnection().add(connectionDto);
        }
    }
    private void buildBrpConnections(CommonReferenceQueryResponse response, List<Connection> connections) {
        for (Connection connection : connections) {
            energy.usef.core.data.xml.bean.message.Connection connectionDto = new energy.usef.core.data.xml.bean.message.Connection();
            connectionDto.setEntityAddress(connection.getEntityAddress());
            connectionDto.setBRPDomain(connection.getBalanceResponsibleParty() == null ? null : connection.getBalanceResponsibleParty().getDomain());
            response.getConnection().add(connectionDto);
        }
    }
    /*
     * Maps data to DSO response object
     */
    private void buildResponseToDSO(CommonReferenceQueryResponse response,
            Map<energy.usef.cro.model.CongestionPoint, Map<energy.usef.cro.model.Aggregator, Long>> results) {
        for (Entry<energy.usef.cro.model.CongestionPoint, Map<energy.usef.cro.model.Aggregator, Long>> congestionPointEntry : results
                .entrySet()) {
            // loop over congestionpoints
            CongestionPoint congestionPointDTO = new CongestionPoint();
            congestionPointDTO.setEntityAddress(congestionPointEntry.getKey().getEntityAddress());
            congestionPointDTO.setDSODomain(congestionPointEntry.getKey().getDistributionSystemOperator().getDomain());

            for (Connection connection : congestionPointEntry.getKey().getConnections()) {
                energy.usef.core.data.xml.bean.message.Connection dtoConnection = new energy.usef.core.data.xml.bean.message.Connection();
                dtoConnection.setEntityAddress(connection.getEntityAddress());
                congestionPointDTO.getConnection().add(dtoConnection);
            }

            for (Entry<energy.usef.cro.model.Aggregator, Long> aggregator : congestionPointEntry.getValue().entrySet()) {
                // loop over aggregators for every congestionpoint
                Aggregator aggregatorDto = new Aggregator();
                aggregatorDto.setDomain(aggregator.getKey() == null ? null : aggregator.getKey().getDomain());
                aggregatorDto.setConnectionCount(
                        aggregator.getKey() == null ? BigInteger.ZERO : BigInteger.valueOf(aggregator.getValue()));
                congestionPointDTO.getAggregator().add(aggregatorDto);
            }
            response.getCongestionPoint().add(congestionPointDTO);
        }
    }

    /*
     * Maps data to AGR response object
     */
    private void buildResponseToAGR(CommonReferenceQueryResponse response,
            Map<energy.usef.cro.model.CongestionPoint, Set<Connection>> results) {

        response.getMessageMetadata().setTimeStamp(DateTimeUtil.getCurrentDateTime());
        for (Entry<energy.usef.cro.model.CongestionPoint, Set<Connection>> congestionPointEntry : results.entrySet()) {
            // loop over congestion points
            CongestionPoint congestionPointDTO = new CongestionPoint();
            congestionPointDTO.setEntityAddress(congestionPointEntry.getKey().getEntityAddress());
            congestionPointDTO.setDSODomain(congestionPointEntry.getKey().getDistributionSystemOperator().getDomain());

            for (Connection connection : congestionPointEntry.getValue()) {
                // loop over connections for every congestion point
                energy.usef.core.data.xml.bean.message.Connection connectionDto = new energy.usef.core.data.xml.bean.message.Connection();
                connectionDto.setEntityAddress(connection.getEntityAddress());
                congestionPointDTO.getConnection().add(connectionDto);
            }
            response.getCongestionPoint().add(congestionPointDTO);
        }
    }

    private boolean validateRegistrationOfAgrSender(CommonReferenceQuery request, CommonReferenceQueryResponse response) {
        if (commonReferenceUpdateBusinessService.getAggregatorByDomain(request.getMessageMetadata().getSenderDomain()) == null) {
            // AGR not registered, failure
            response.setResult(DispositionSuccessFailure.FAILURE);
            response.setMessage("AGR is not registered");
            LOGGER.error(response.getMessage());
            return false;
        }
        return true;
    }

    private void roleNotSupportedResponse(CommonReferenceQueryResponse response) {
        // Role is not supported for this functionality
        response.setResult(DispositionSuccessFailure.FAILURE);
        response.setMessage("Role is not supported!");
        LOGGER.error(response.getMessage());
    }
}
