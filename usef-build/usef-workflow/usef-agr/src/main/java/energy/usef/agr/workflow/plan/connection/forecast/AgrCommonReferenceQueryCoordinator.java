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

package energy.usef.agr.workflow.plan.connection.forecast;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.core.data.xml.bean.message.MessagePrecedence.TRANSACTIONAL;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.model.CommonReferenceOperator;
import energy.usef.agr.service.business.AgrPlanboardBusinessService;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.CommonReferenceEntityType;
import energy.usef.core.data.xml.bean.message.CommonReferenceQuery;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.XMLUtil;

import java.util.List;

import javax.ejb.Singleton;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AGR coordinator class for the AGR Create N-Day-Ahead Forecasts workflow.
 */
@Singleton
public class AgrCommonReferenceQueryCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrCommonReferenceQueryCoordinator.class);

    @Inject
    private JMSHelperService jmsHelperService;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private AgrPlanboardBusinessService agrPlanboardBusinessService;

    @Inject
    private Config config;

    @Inject
    private ConfigAgr configAgr;

    /**
     * {@inheritDoc}
     */
    public void handleEvent(@Observes CommonReferenceQueryEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        List<CommonReferenceOperator> commonReferenceOperators = agrPlanboardBusinessService.findAll();
        for (CommonReferenceOperator commonReferenceOperator : commonReferenceOperators) {
            jmsHelperService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(buildCommonReferenceQuery(commonReferenceOperator
                    .getDomain(), CommonReferenceEntityType.CONGESTION_POINT)));
            jmsHelperService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(buildCommonReferenceQuery(commonReferenceOperator
                    .getDomain(), CommonReferenceEntityType.BRP)));
        }

        initializePtuContainers();
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    /**
     * Initialize the PTU containers and PTU Reoptimization.
     */
    private void initializePtuContainers() {
        Integer initializationDelay = 1;
        Integer initializationDuration = configAgr.getIntegerProperty(ConfigAgrParam.AGR_INITIALIZE_PLANBOARD_DAYS_INTERVAL);
        LocalDate initializationDate = DateTimeUtil.getCurrentDate().plusDays(initializationDelay);
        for (int i = 0; i < initializationDuration; ++i) {
            corePlanboardBusinessService.findOrCreatePtuContainersForPeriod(initializationDate.plusDays(i));
        }
    }

    private CommonReferenceQuery buildCommonReferenceQuery(String recipientDomain, CommonReferenceEntityType congestionPoint) {
        CommonReferenceQuery query = new CommonReferenceQuery();
        query.setEntity(congestionPoint);

        MessageMetadata messageMetadata = MessageMetadataBuilder.build(recipientDomain, USEFRole.CRO,
                config.getProperty(ConfigParam.HOST_DOMAIN), USEFRole.AGR,
                TRANSACTIONAL).build();
        query.setMessageMetadata(messageMetadata);
        return query;
    }

}
