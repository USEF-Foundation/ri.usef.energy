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

package energy.usef.brp.workflow.plan.connection.forecast;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.core.data.xml.bean.message.MessagePrecedence.TRANSACTIONAL;

import energy.usef.brp.config.ConfigBrp;
import energy.usef.brp.config.ConfigBrpParam;
import energy.usef.brp.model.CommonReferenceOperator;
import energy.usef.brp.service.business.BrpBusinessService;
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
 * Coordinator class for the BRP common reference query process.
 */
@Singleton
public class BrpCommonReferenceQueryCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrpCommonReferenceQueryCoordinator.class);

    @Inject
    private JMSHelperService jmsHelperService;

    @Inject
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Inject
    private BrpBusinessService brpBusinessService;

    @Inject
    private Config config;

    @Inject
    private ConfigBrp configBrp;

    /**
     * Handles a {@link CommonReferenceQueryEvent}. This will create a common reference query message for each registered Common
     * Reference Operator in the BRP database.
     * 
     * @param event {@link CommonReferenceQueryEvent}.
     */
    public void handleEvent(@Observes CommonReferenceQueryEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        List<CommonReferenceOperator> commonReferenceOperators = brpBusinessService.findAllCommonReferenceOperators();
        for (CommonReferenceOperator commonReferenceOperator : commonReferenceOperators) {
            LOGGER.debug("Common Reference Query message will be sent to CRO [{}].", commonReferenceOperator.getDomain());
            jmsHelperService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(buildCommonReferenceQuery(commonReferenceOperator
                    .getDomain())));
        }

        initializePtuContainers();
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    /**
     * Initialize the PTU containers.
     */
    private void initializePtuContainers() {
        // initialize the PTU containers
        Integer initializationDelay = 1;
        Integer initializationDuration = configBrp.getIntegerProperty(ConfigBrpParam.BRP_INITIALIZE_PLANBOARD_DAYS_INTERVAL);
        LocalDate initializationDate = DateTimeUtil.getCurrentDate().plusDays(initializationDelay);
        for (int i = 0; i < initializationDuration; ++i) {
            corePlanboardBusinessService.findOrCreatePtuContainersForPeriod(initializationDate.plusDays(i));
        }
    }

    /**
     * Builds the common reference query message for a BRP participant (recipient is a CRO participant).
     * 
     * @param recipientDomain {@link String} Domain name of the CRO.
     * @return a {@link CommonReferenceQuery}.
     */
    private CommonReferenceQuery buildCommonReferenceQuery(String recipientDomain) {
        CommonReferenceQuery query = new CommonReferenceQuery();
        query.setEntity(CommonReferenceEntityType.AGGREGATOR);

        MessageMetadata messageMetadata = MessageMetadataBuilder.build(recipientDomain, USEFRole.CRO,
                config.getProperty(ConfigParam.HOST_DOMAIN), USEFRole.BRP, TRANSACTIONAL).build();
        query.setMessageMetadata(messageMetadata);
        return query;
    }
}
