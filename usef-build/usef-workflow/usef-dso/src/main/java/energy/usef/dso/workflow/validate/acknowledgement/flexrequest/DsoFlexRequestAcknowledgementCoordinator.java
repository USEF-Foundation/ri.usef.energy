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

package energy.usef.dso.workflow.validate.acknowledgement.flexrequest;

import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_FINISHED_HANDLING_EVENT;
import static energy.usef.core.constant.USEFConstants.LOG_COORDINATOR_START_HANDLING_EVENT;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import energy.usef.dso.config.ConfigDso;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flexibility request Acknowledgement workflow coordinator.
 */
@Stateless
public class DsoFlexRequestAcknowledgementCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DsoFlexRequestAcknowledgementCoordinator.class);
    @Inject
    private ConfigDso config;

    public DsoFlexRequestAcknowledgementCoordinator() {
    }

    public DsoFlexRequestAcknowledgementCoordinator(ConfigDso config) {
        this.config = config;
    }

    /**
     * {@inheritDoc}
     */
    @Asynchronous
    public void handleEvent(@Observes FlexRequestAcknowledgementEvent event) {
        LOGGER.info(LOG_COORDINATOR_START_HANDLING_EVENT, event);

        String endpoint = config.getProperties().getProperty("FLEX_REQUEST_ENDPOINT");
        if (StringUtils.isEmpty(endpoint)) {
            LOGGER.error("Configuration for flex request endpoint ('FLEX_REQUEST_ENDPOINT' is missing");
        } else {
            sendAcknowledgement(event, endpoint);
        }

        LOGGER.info(LOG_COORDINATOR_FINISHED_HANDLING_EVENT, event);
    }

    private void sendAcknowledgement(FlexRequestAcknowledgementEvent event, String endpoint) {
        try {
            Unirest.post(endpoint + "/api/flexrequests/{id}/response")
                    .routeParam("id", String.valueOf(event.getSequence()))
                    .field("aggregatorName", event.getAggregatorDomain())
                    .field("status", event.getAcknowledgementStatus().toString()).asJson();
        } catch (UnirestException e) {
            LOGGER.warn("Unable to send acknowledgement to the flex-service: {}", endpoint);
        }
    }

}
