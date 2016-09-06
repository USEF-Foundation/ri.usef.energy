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

package energy.usef.agr.workflow.plan.commonreferenceupdate;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;
import static energy.usef.core.data.xml.bean.message.MessagePrecedence.ROUTINE;

import energy.usef.agr.model.SynchronisationConnection;
import energy.usef.agr.model.SynchronisationConnectionStatusType;
import energy.usef.agr.service.business.AgrPlanboardBusinessService;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.CommonReferenceEntityType;
import energy.usef.core.data.xml.bean.message.CommonReferenceUpdate;
import energy.usef.core.data.xml.bean.message.Connection;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.util.XMLUtil;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ejb.Singleton;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the base logic for handling the CommonReferenceUpdate logic.
 */
@Singleton
public class AgrCommonReferenceUpdateCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgrCommonReferenceUpdateCoordinator.class);

    @Inject
    private AgrPlanboardBusinessService businessService;

    @Inject
    private Config config;

    @Inject
    private JMSHelperService jmsService;

    /**
     * Handle the logic, to execute a {@link CommonReferenceUpdate}s.
     *
     * @param event
     */
    public void handleEvent(@Observes CommonReferenceUpdateEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);
        Map<String, List<SynchronisationConnection>> connectionsPerCRO = businessService.findConnectionsPerCRO();
        for (Entry<String, List<SynchronisationConnection>> entry : connectionsPerCRO.entrySet()) {
            // send a cro message per cro.
            CommonReferenceUpdate commonReferenceUpdate = buildMessage(entry.getKey(), entry.getValue());
            if (commonReferenceUpdate != null) {
                jmsService.sendMessageToOutQueue(XMLUtil.messageObjectToXml(commonReferenceUpdate));
            }
            // update connection date
            for (SynchronisationConnection connection : entry.getValue()) {
                connection.setLastSynchronisationTime(DateTimeUtil.getCurrentDateTime());
            }
        }
        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private CommonReferenceUpdate buildMessage(String croDomain, List<SynchronisationConnection> connections) {
        CommonReferenceUpdate message = new CommonReferenceUpdate();
        message.setEntity(CommonReferenceEntityType.AGGREGATOR);
        message.setMessageMetadata(MessageMetadataBuilder.build(croDomain, USEFRole.CRO,
                config.getProperty(ConfigParam.HOST_DOMAIN), USEFRole.AGR, ROUTINE).build());
        for (SynchronisationConnection connection : connections) {
            SynchronisationConnectionStatusType connectionStatus = connection.findStatusForCRO(croDomain);
            if (!SynchronisationConnectionStatusType.SYNCHRONIZED.equals(connectionStatus)) {
                Connection dtoConnection = new Connection();
                dtoConnection.setEntityAddress(connection.getEntityAddress());
                dtoConnection.setIsCustomer(connection.isCustomer());
                message.getConnection().add(dtoConnection);
            }
        }
        if (message.getConnection().isEmpty()) {
            // no use to send a message
            return null;
        }
        return message;
    }
}
