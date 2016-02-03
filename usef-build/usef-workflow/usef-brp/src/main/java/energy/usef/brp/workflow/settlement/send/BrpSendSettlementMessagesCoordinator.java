/*
 * Copyright 2015 USEF Foundation
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

package energy.usef.brp.workflow.settlement.send;

import static energy.usef.brp.service.business.BrpDefaultSettlementMessageContent.ORDER_SETTLEMENT_ORDER_REFERENCE;
import static energy.usef.brp.service.business.BrpDefaultSettlementMessageContent.PTU_SETTLEMENT_ACTUAL_POWER;
import static energy.usef.brp.service.business.BrpDefaultSettlementMessageContent.PTU_SETTLEMENT_DELIVERED_FLEX_POWER;
import static energy.usef.brp.service.business.BrpDefaultSettlementMessageContent.PTU_SETTLEMENT_NET_SETTLEMENT;
import static energy.usef.brp.service.business.BrpDefaultSettlementMessageContent.PTU_SETTLEMENT_ORDERED_FLEX_POWER;
import static energy.usef.brp.service.business.BrpDefaultSettlementMessageContent.PTU_SETTLEMENT_PRICE;
import static energy.usef.brp.service.business.BrpDefaultSettlementMessageContent.PTU_SETTLEMENT_PROGNOSIS_POWER;
import static energy.usef.brp.service.business.BrpDefaultSettlementMessageContent.PTU_SETTLEMENT_START;
import static energy.usef.core.data.xml.bean.message.MessagePrecedence.TRANSACTIONAL;

import energy.usef.brp.config.ConfigBrp;
import energy.usef.brp.config.ConfigBrpParam;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.FlexOrderSettlement;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.PTUSettlement;
import energy.usef.core.data.xml.bean.message.SettlementMessage;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.model.AgrConnectionGroup;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.util.XMLUtil;
import energy.usef.core.workflow.settlement.CoreSettlementBusinessService;
import energy.usef.core.workflow.transformer.SettlementTransformer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.joda.time.Months;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This coordinator class is in charge of the workflow sending Settlement messages to aggregators.
 */
@Stateless
public class BrpSendSettlementMessagesCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrpSendSettlementMessagesCoordinator.class);

    @Inject
    private Config config;
    @Inject
    private ConfigBrp configBrp;
    @Inject
    private JMSHelperService jmsHelperService;
    @Inject
    private CorePlanboardBusinessService planboardBusinessService;
    @Inject
    private CoreSettlementBusinessService coreSettlementBusinessService;
    @Inject
    private SequenceGeneratorService sequenceGeneratorService;

    /**
     * This method starts the workflow when triggered by an event.
     *
     * @param event {@link SendSettlementMessageEvent} event which starts the workflow.
     */
    public void invokeWorkflow(@Observes SendSettlementMessageEvent event) {
        LocalDate dateFrom = new LocalDate(event.getYear(), event.getMonth(), 1);
        LocalDate dateUntil = dateFrom.plus(Months.ONE).minusDays(1);

        LOGGER.debug("SendSettlementMessageEvent for {} until {}.", dateFrom, dateUntil);

        // Fetch all aggregators having active connections in the period defined by [dateFrom, dateUntil] .
        List<String> aggregators = planboardBusinessService
                .findConnectionGroupWithConnectionsWithOverlappingValidity(dateFrom, dateUntil)
                .values().stream().flatMap(map -> map.keySet().stream())
                .map(connectionGroup -> ((AgrConnectionGroup) connectionGroup).getAggregatorDomain())
                .distinct()
                .collect(Collectors.toList());

        // Fetch all FlexOrderSettlement for the period
        Map<String, List<energy.usef.core.model.FlexOrderSettlement>> flexOrderSettlementPerAggregator = coreSettlementBusinessService
                .findFlexOrderSettlementsForPeriod(dateFrom, dateUntil, Optional.empty(), Optional.empty()).stream()
                .collect(Collectors.groupingBy(flexOrderSettlement -> flexOrderSettlement.getFlexOrder().getParticipantDomain()));

        if (aggregators.isEmpty()) {
            LOGGER.error("SendSettlementMessageEvent triggered while there are no aggregators eligible for settlement.");
            return;
        }

        for (String aggregator : aggregators) {
            SettlementMessage settlementMessage = buildSettlementMessage(flexOrderSettlementPerAggregator.get(aggregator),
                    dateFrom);
            populateSettlementMessageData(settlementMessage, aggregator, dateFrom, dateUntil);
            storeSettlementMessage(aggregator, flexOrderSettlementPerAggregator.get(aggregator));
            jmsHelperService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(settlementMessage));
        }
    }

    private SettlementMessage buildSettlementMessage(List<energy.usef.core.model.FlexOrderSettlement> flexOrderSettlements,
            LocalDate dateFrom) {
        if (flexOrderSettlements == null || flexOrderSettlements.isEmpty()) {
            return buildDefaultSettlementMessage(dateFrom);
        }
        SettlementMessage settlementMessage = new SettlementMessage();
        for (energy.usef.core.model.FlexOrderSettlement flexOrderSettlement : flexOrderSettlements) {
            settlementMessage.getFlexOrderSettlement().add(SettlementTransformer.transformToXml(flexOrderSettlement));
        }
        return settlementMessage;
    }

    private void populateSettlementMessageData(SettlementMessage settlementMessage, String aggregatorDomain, LocalDate dateFrom,
            LocalDate dateUntil) {
        MessageMetadata messageMetadata = new MessageMetadataBuilder().conversationID().messageID().timeStamp()
                .senderDomain(config.getProperty(ConfigParam.HOST_DOMAIN)).senderRole(USEFRole.BRP)
                .recipientDomain(aggregatorDomain).recipientRole(USEFRole.AGR)
                .precedence(TRANSACTIONAL).build();
        settlementMessage.setMessageMetadata(messageMetadata);
        settlementMessage.setCurrency(config.getProperty(ConfigParam.CURRENCY));
        settlementMessage.setPeriodStart(dateFrom);
        settlementMessage.setPeriodEnd(dateUntil);
        settlementMessage.setPTUDuration(Period.minutes(config.getIntegerProperty(ConfigParam.PTU_DURATION)));
        settlementMessage.setTimeZone(config.getProperty(ConfigParam.TIME_ZONE));
        settlementMessage.setReference(config.getProperty(ConfigParam.HOST_DOMAIN) + sequenceGeneratorService.next());
    }

    private void storeSettlementMessage(String participantDomain, List<energy.usef.core.model.FlexOrderSettlement> flexOrderSettlements) {
        planboardBusinessService.storeFlexOrderSettlementsPlanboardMessage(flexOrderSettlements,
                configBrp.getIntegerProperty(ConfigBrpParam.BRP_SETTLEMENT_RESPONSE_WAITING_DURATION), DocumentStatus.SENT,
                participantDomain, null);
    }

    private SettlementMessage buildDefaultSettlementMessage(LocalDate dateFrom) {
        SettlementMessage settlementMessage = new SettlementMessage();

        FlexOrderSettlement defaultFlexOrderSettlement = new FlexOrderSettlement();
        defaultFlexOrderSettlement.setPeriod(dateFrom);
        defaultFlexOrderSettlement.setOrderReference(ORDER_SETTLEMENT_ORDER_REFERENCE.getValue());
        settlementMessage.getFlexOrderSettlement().add(defaultFlexOrderSettlement);

        PTUSettlement defaultPtuSettlement = new PTUSettlement();
        defaultPtuSettlement.setActualPower(new BigInteger(PTU_SETTLEMENT_ACTUAL_POWER.getValue()));
        defaultPtuSettlement.setDeliveredFlexPower(new BigInteger(PTU_SETTLEMENT_DELIVERED_FLEX_POWER.getValue()));
        defaultPtuSettlement.setNetSettlement(new BigDecimal(PTU_SETTLEMENT_NET_SETTLEMENT.getValue()));
        defaultPtuSettlement.setOrderedFlexPower(new BigInteger(PTU_SETTLEMENT_ORDERED_FLEX_POWER.getValue()));
        defaultPtuSettlement.setPrognosisPower(new BigInteger(PTU_SETTLEMENT_PROGNOSIS_POWER.getValue()));
        defaultPtuSettlement.setPrice(new BigDecimal(PTU_SETTLEMENT_PRICE.getValue()));
        defaultPtuSettlement.setStart(new BigInteger(PTU_SETTLEMENT_START.getValue()));
        defaultFlexOrderSettlement.getPTUSettlement().add(defaultPtuSettlement);

        return settlementMessage;
    }
}
