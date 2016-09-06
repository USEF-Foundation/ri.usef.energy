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

package energy.usef.mdc.workflow.meterdata;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.mdc.workflow.meterdata.MeterDataQueryStepParameter.IN.CONNECTIONS;
import static energy.usef.mdc.workflow.meterdata.MeterDataQueryStepParameter.IN.DATE_RANGE_END;
import static energy.usef.mdc.workflow.meterdata.MeterDataQueryStepParameter.IN.DATE_RANGE_START;
import static energy.usef.mdc.workflow.meterdata.MeterDataQueryStepParameter.IN.META_DATA_QUERY_TYPE;
import static energy.usef.mdc.workflow.meterdata.MeterDataQueryStepParameter.IN.PTU_DURATION;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.Connections;
import energy.usef.core.data.xml.bean.message.DispositionSuccessFailure;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.MeterDataQueryResponse;
import energy.usef.core.data.xml.bean.message.MeterDataQueryType;
import energy.usef.core.data.xml.bean.message.MeterDataSet;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.util.XMLUtil;
import energy.usef.core.workflow.DefaultWorkflowContext;
import energy.usef.core.workflow.WorkflowContext;
import energy.usef.core.workflow.dto.MeterDataQueryTypeDto;
import energy.usef.core.workflow.step.WorkflowStepExecuter;
import energy.usef.core.workflow.util.WorkflowUtil;
import energy.usef.mdc.dto.ConnectionMeterDataDto;
import energy.usef.mdc.dto.MeterDataDto;
import energy.usef.mdc.dto.PtuMeterDataDto;
import energy.usef.mdc.service.business.MdcCoreBusinessService;
import energy.usef.mdc.transformer.MeterDataTransformer;
import energy.usef.mdc.workflow.MdcWorkflowStep;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Coordinator that handles the {@link MeterDataQueryEvent}.
 */
@Stateless
@Transactional(value = Transactional.TxType.REQUIRED)
public class MdcMeterDataQueryCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MdcMeterDataQueryCoordinator.class);


    @Inject
    private JMSHelperService jmsHelperService;

    @Inject
    private Config config;

    @Inject
    private WorkflowStepExecuter workflowStepExecuter;

    @Inject
    private MdcCoreBusinessService mdcCoreBusinessService;

    /**
     * Handle the {@link MeterDataQueryEvent} and send a response.
     *
     * @param event The actual event.
     */
    public void handleEvent(@Observes MeterDataQueryEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);

        // build response
        MeterDataQueryResponse meterDataQueryResponse = new MeterDataQueryResponse();
        // build response messageMetadata
        MessageMetadata messageMetadata = event.getMeterDataQuery().getMessageMetadata();
        MessageMetadata responseMessageMetadata = MessageMetadataBuilder.build(messageMetadata.getSenderDomain(),
                messageMetadata.getSenderRole(), config.getProperty(ConfigParam.HOST_DOMAIN), USEFRole.MDC,
                messageMetadata.getPrecedence()).conversationID(messageMetadata.getConversationID()).build();
        meterDataQueryResponse.setMessageMetadata(responseMessageMetadata);

        meterDataQueryResponse.setResult(DispositionSuccessFailure.FAILURE);
        meterDataQueryResponse.setDateRangeStart(event.getMeterDataQuery().getDateRangeStart());
        meterDataQueryResponse.setDateRangeEnd(event.getMeterDataQuery().getDateRangeEnd());
        meterDataQueryResponse.setQueryType(event.getMeterDataQuery().getQueryType());

        // check sender
        if ((USEFRole.DSO.equals(messageMetadata.getSenderRole())
                && mdcCoreBusinessService.findDistributionSystemOperator(messageMetadata.getSenderDomain()) != null) || (
                USEFRole.BRP.equals(messageMetadata.getSenderRole())
                        && mdcCoreBusinessService.findBalanceResponsibleParty(messageMetadata.getSenderDomain()) != null)) {
            // invoke PBC
            Map<MeterDataSet, List<MeterDataDto>> meterDataDtos = invokePBCMeterData(event);
            if (!meterDataDtos.values().stream().flatMap(Collection::stream).collect(Collectors.toList()).isEmpty()) {
                meterDataQueryResponse.setResult(DispositionSuccessFailure.SUCCESS);
                fillMeterDataQueryResponse(meterDataQueryResponse, meterDataDtos);
            } else {
                meterDataQueryResponse.setMessage("No data available for any of the days requested.");
            }
        } else {
            meterDataQueryResponse.setMessage("Sender is not a configured customer.");
        }

        // send response
        jmsHelperService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(meterDataQueryResponse));

        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private void fillMeterDataQueryResponse(MeterDataQueryResponse response,
            Map<MeterDataSet, List<MeterDataDto>> meterDataDtosPerConnectionGroup) {
        for (MeterDataSet connectionGroup : meterDataDtosPerConnectionGroup.keySet()) {
            connectionGroup.getMeterData()
                    .addAll(MeterDataTransformer.transformListToXml(meterDataDtosPerConnectionGroup.get(connectionGroup)));
            response.getMeterDataSet().add(connectionGroup);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<MeterDataSet, List<MeterDataDto>> invokePBCMeterData(MeterDataQueryEvent event) {
        WorkflowContext context = new DefaultWorkflowContext();
        Map<MeterDataSet, List<MeterDataDto>> resultMap = new HashMap<>();

        for (Connections connectionGroup : event.getMeterDataQuery().getConnections()) {
            context.setValue(PTU_DURATION.name(), config.getIntegerProperty(ConfigParam.PTU_DURATION));
            context.setValue(DATE_RANGE_START.name(), event.getMeterDataQuery().getDateRangeStart());
            context.setValue(DATE_RANGE_END.name(), event.getMeterDataQuery().getDateRangeEnd());
            context.setValue(CONNECTIONS.name(), connectionGroup.getConnection());
            context.setValue(META_DATA_QUERY_TYPE.name(), convertToMeterDataQueryTypeDto(event.getMeterDataQuery().getQueryType()));
            // invoke PBC
            context = workflowStepExecuter.invoke(MdcWorkflowStep.MDC_METER_DATA_QUERY.name(), context);
            WorkflowUtil.validateContext(MdcWorkflowStep.MDC_METER_DATA_QUERY.name(), context,
                    MeterDataQueryStepParameter.OUT.values());
            List<MeterDataDto> meterDataDtos = context.get(MeterDataQueryStepParameter.OUT.METER_DATA.name(), List.class);
            MeterDataSet meterDataSet = mapToMeterDataSet(connectionGroup);
            if (event.getMeterDataQuery().getQueryType() == MeterDataQueryType.USAGE) {
                // regroup
                resultMap.put(meterDataSet, reGroupByAggregator(connectionGroup.getConnection(), meterDataDtos));
            } else {
                resultMap.put(meterDataSet, meterDataDtos);
            }
        }

        return resultMap;
    }

    private MeterDataQueryTypeDto convertToMeterDataQueryTypeDto(MeterDataQueryType type) {
        if (type == MeterDataQueryType.USAGE) {
            return MeterDataQueryTypeDto.USAGE;
        } else if (type == MeterDataQueryType.EVENTS) {
            return MeterDataQueryTypeDto.EVENTS;
        }
        return MeterDataQueryTypeDto.ANY;
    }

    private List<MeterDataDto> reGroupByAggregator(List<String> connections, List<MeterDataDto> meterDataDtos) {
        List<MeterDataDto> result = new ArrayList<>(meterDataDtos.size());
        // for each meterData
        for (MeterDataDto meterDataDto : meterDataDtos) {

            // find entityAddres -> aggregator map.
            Map<String, String> connectionAggregatorMap = mdcCoreBusinessService.findConnectionState(meterDataDto.getPeriod(),
                    connections);

            // set aggregator
            meterDataDto.getConnectionMeterDataDtos()
                    .forEach(connectionMeterData -> connectionMeterData.setAgrDomain(
                            connectionAggregatorMap.get(connectionMeterData.getEntityAddress())));

            // re-group by aggregator
            List<ConnectionMeterDataDto> connectionMeterDataPerAggregator = meterDataDto.getConnectionMeterDataDtos()
                    .stream()
                    .filter(connectionMeterData -> connectionMeterData.getAgrDomain() != null)
                    .collect(Collectors.groupingBy(ConnectionMeterDataDto::getAgrDomain))
                    .entrySet()
                    .stream()
                    .map(entry -> {

                        // map to one ConnectionMeterData
                        ConnectionMeterDataDto connectionMeterDataDto = new ConnectionMeterDataDto();
                        connectionMeterDataDto.setAgrDomain(entry.getKey());
                        connectionMeterDataDto.setEntityCount(BigInteger.valueOf(entry.getValue().size()));

                        // merge ptuMeterData
                        mergePtuMeterData(entry.getValue(), connectionMeterDataDto);

                        return connectionMeterDataDto;
                    })
                    .collect(Collectors.toList());

            // replace old List
            meterDataDto.getConnectionMeterDataDtos().clear();
            meterDataDto.getConnectionMeterDataDtos().addAll(connectionMeterDataPerAggregator);

            // add to result if there is still data
            if (!meterDataDto.getConnectionMeterDataDtos().isEmpty()) {
                result.add(meterDataDto);
            }
        }
        return result;
    }

    private void mergePtuMeterData(List<ConnectionMeterDataDto> connectionMeterDataList,
            ConnectionMeterDataDto connectionMeterData) {
        connectionMeterData.getPtuMeterDataDtos()
                .addAll(connectionMeterDataList.stream().flatMap(value -> value.getPtuMeterDataDtos().stream())
                        // group into <PTUIndex, PTUMeterData>
                        .collect(Collectors.groupingBy(PtuMeterDataDto::getStart)).entrySet().stream().map(ptuSet -> {
                            // map to one PTUMeterData
                            PtuMeterDataDto ptuMeterDataDto = new PtuMeterDataDto();
                            ptuMeterDataDto.setStart(ptuSet.getKey());
                            ptuMeterDataDto.setDuration(BigInteger.ONE);
                            ptuMeterDataDto.setPower(
                                    ptuSet.getValue().stream().map(PtuMeterDataDto::getPower).reduce(BigInteger::add).get());
                            return ptuMeterDataDto;
                        }).collect(Collectors.toList()));
    }

    private MeterDataSet mapToMeterDataSet(Connections connectionGroup) {
        MeterDataSet group = new MeterDataSet();
        group.setEntityAddress(connectionGroup.getParent());
        return group;
    }

}
