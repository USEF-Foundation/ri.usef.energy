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

package energy.usef.cro.service.business;

import energy.usef.cro.model.CongestionPoint;
import energy.usef.cro.model.Connection;
import energy.usef.cro.repository.CongestionPointRepository;
import energy.usef.cro.repository.ConnectionRepository;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * This service class implements the business logic related to the CRO part of the common reference query.
 * 
 */
@Stateless
public class CommonReferenceQueryBusinessService {

    @Inject
    private CongestionPointRepository congestionPointRepository;

    @Inject
    private ConnectionRepository connectionRepository;

    /**
     * Finds the connections for the AGR domain.
     * 
     * @param agrDomain
     * @return
     */
    public List<Connection> findAllConnectionsForAggregatorDomain(String agrDomain) {
        return connectionRepository.findConnectionsForAggregator(agrDomain);
    }

    /**
     * Finds the congestionpoint for the AGR domain.
     * 
     * @param senderDomain
     * @return
     */
    public List<CongestionPoint> findAllCongestionPointsForAggregatorDomain(String senderDomain) {
        return congestionPointRepository.findCongestionPointsForAggregator(senderDomain);
    }

    /**
     * Finds the connections for the BRP domain.
     * 
     * @param brpDomain
     * @return
     */
    public List<Connection> findAllConnectionsForBRPDomain(String brpDomain) {
        return connectionRepository.findConnectionsForBRP(brpDomain);
    }

    /**
     * Finds the connections for the MDC domain.
     * 
     * @return
     */
    public List<Connection> findAllConnectionsForMDCDomain() {
        return connectionRepository.findConnectionsForMDC();
    }

}
