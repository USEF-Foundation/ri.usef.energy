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

package energy.usef.mdc.workflow;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.core.data.xml.bean.message.MessagePrecedence.TRANSACTIONAL;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.CommonReferenceEntityType;
import energy.usef.core.data.xml.bean.message.CommonReferenceQuery;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.util.XMLUtil;
import energy.usef.mdc.service.business.MdcCoreBusinessService;

import java.util.List;

import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinator class in charge of handling the Common Reference Query creation for the MDC participant.
 */
@Singleton
public class MdcCommonReferenceQueryCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MdcCommonReferenceQueryCoordinator.class);

    @Inject
    private MdcCoreBusinessService mdcCoreBusinessService;
    @Inject
    private JMSHelperService jmsHelperService;
    @Inject
    private Config config;

    /**
     * Handle the event triggering the Common Reference Query workflow.
     *
     * @param event a {@link CommonReferenceQueryEvent}.
     */
    @Asynchronous
    @Lock(LockType.WRITE)
    public void handleEvent(@Observes CommonReferenceQueryEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        List<String> commonReferenceOperatorDomains = mdcCoreBusinessService.findAllCommonReferenceOperatorDomains();
        List<String> connectionEntityAddresses = mdcCoreBusinessService.findAllConnectionEntityAddresses();
        commonReferenceOperatorDomains.stream()
                .forEach(domain -> sendCommonReferenceQuery(buildCommonReferenceQuery(domain, connectionEntityAddresses)));

        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    /**
     * Builds the common reference query message with the given CRO domain and the given list of connection addresses.
     *
     * @param croDomain {@link String} CRO domain name.
     * @param connectionEntityAddresses {@link java.util.List} of {@link String} containing the connection entity addresses.
     * @return a {@link CommonReferenceQuery} message ready to be sent.
     */
    private CommonReferenceQuery buildCommonReferenceQuery(String croDomain, List<String> connectionEntityAddresses) {
        LOGGER.debug("Building Common Reference Query for CRO [{}].", croDomain);
        CommonReferenceQuery commonReferenceQuery = new CommonReferenceQuery();
        commonReferenceQuery.setMessageMetadata(new MessageMetadataBuilder().messageID()
                .conversationID()
                .timeStamp()
                .recipientDomain(croDomain)
                .recipientRole(USEFRole.CRO)
                .senderDomain(config.getProperty(ConfigParam.HOST_DOMAIN))
                .senderRole(USEFRole.MDC)
                .precedence(TRANSACTIONAL)
                .build());
        commonReferenceQuery.setEntity(CommonReferenceEntityType.AGGREGATOR);
        commonReferenceQuery.getConnectionEntityAddress().addAll(connectionEntityAddresses);
        return commonReferenceQuery;
    }

    private void sendCommonReferenceQuery(CommonReferenceQuery commonReferenceQuery) {
        jmsHelperService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(commonReferenceQuery));
    }

}
