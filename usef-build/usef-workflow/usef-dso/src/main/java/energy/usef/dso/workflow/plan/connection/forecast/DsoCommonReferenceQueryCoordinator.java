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

package energy.usef.dso.workflow.plan.connection.forecast;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.core.data.xml.bean.message.MessagePrecedence.TRANSACTIONAL;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.CommonReferenceEntityType;
import energy.usef.core.data.xml.bean.message.CommonReferenceQuery;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.XMLUtil;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.model.CommonReferenceOperator;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;

import java.util.UUID;

import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DSO coordinator class for the DSO Non Aggregator Connection Forecast workflow.
 */
@Singleton
public class DsoCommonReferenceQueryCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DsoCommonReferenceQueryCoordinator.class);

    @Inject
    private Config config;

    @Inject
    private ConfigDso configDso;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private JMSHelperService jmsHelperService;

    @Inject
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;


    /**
     * {@inheritDoc}
     */
    @Asynchronous
    @Lock(LockType.WRITE)
    public void handleEvent(@Observes CommonReferenceQueryEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        for (CommonReferenceOperator commonReferenceOperator : dsoPlanboardBusinessService.findAllCommonReferenceOperators()) {
            jmsHelperService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(buildCommonReferenceQuery(commonReferenceOperator
                    .getDomain())));
        }
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }



    private CommonReferenceQuery buildCommonReferenceQuery(String croDomain) {
        CommonReferenceQuery query = new CommonReferenceQuery();
        query.setEntity(CommonReferenceEntityType.CONGESTION_POINT);
        query.setMessageMetadata(buildCommonReferenceQueryMetadata(croDomain));
        return query;
    }

    private MessageMetadata buildCommonReferenceQueryMetadata(String croDomain) {
        MessageMetadata metadata = new MessageMetadata();
        metadata.setSenderDomain(config.getProperty(ConfigParam.HOST_DOMAIN));
        metadata.setSenderRole(USEFRole.DSO);
        metadata.setRecipientDomain(croDomain);
        metadata.setRecipientRole(USEFRole.CRO);
        metadata.setMessageID(UUID.randomUUID().toString());
        metadata.setConversationID(metadata.getMessageID());
        metadata.setPrecedence(TRANSACTIONAL);
        metadata.setTimeStamp(DateTimeUtil.getCurrentDateTime());
        return metadata;
    }

}
