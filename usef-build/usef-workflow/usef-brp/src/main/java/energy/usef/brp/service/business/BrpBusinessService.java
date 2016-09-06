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

package energy.usef.brp.service.business;

import energy.usef.brp.model.CommonReferenceOperator;
import energy.usef.brp.model.MeterDataCompany;
import energy.usef.brp.model.SynchronisationConnection;
import energy.usef.brp.model.SynchronisationConnectionStatusType;
import energy.usef.brp.repository.CommonReferenceOperatorRepository;
import energy.usef.brp.repository.MeterDataCompanyRepository;
import energy.usef.brp.repository.SynchronisationConnectionRepository;
import energy.usef.brp.repository.SynchronisationConnectionStatusRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

/**
 * Business Service for all BRP specific methods.
 */
@Transactional(value = TxType.REQUIRED)
public class BrpBusinessService {

    @Inject
    private SynchronisationConnectionRepository synchronisationConnectionRepository;

    @Inject
    private SynchronisationConnectionStatusRepository synchronisationConnectionStatusRepository;

    @Inject
    private CommonReferenceOperatorRepository commonReferenceOperatorRepository;

    @Inject
    private MeterDataCompanyRepository meterDataCompanyRepository;

    /**
     * Finds all {@link CommonReferenceOperator}s and {@link SynchronisationConnection}s and put them in a Map, all
     * {@link SynchronisationConnection}s are send to all {@link CommonReferenceOperator}s.
     * 
     * @return
     */
    public Map<String, List<SynchronisationConnection>> findConnectionsPerCRO() {
        List<SynchronisationConnection> connections = synchronisationConnectionRepository.findAll();

        List<CommonReferenceOperator> cros = commonReferenceOperatorRepository.findAll();

        Map<String, List<SynchronisationConnection>> connectionsPerCRO = new HashMap<>();
        if (connections.isEmpty() || cros.isEmpty()) {
            return connectionsPerCRO;
        }
        for (CommonReferenceOperator cro : cros) {
            connectionsPerCRO.put(cro.getDomain(), connections);
        }
        return connectionsPerCRO;
    }

    /**
     *
     * @return
     */
    public List<CommonReferenceOperator> findAllCommonReferenceOperators() {
        return commonReferenceOperatorRepository.findAll();
    }

    /**
     * Updates the connection status for the CRO.
     *
     * @param croDomain
     */
    @Transactional(value = TxType.REQUIRES_NEW)
    public void updateConnectionStatusForCRO(String croDomain) {
        synchronisationConnectionRepository.updateConnectionStatusForCRO(croDomain);
    }

    /**
     * Clean's the synchronization table's if synchronization is complete.
     */
    public void cleanSynchronization() {
        long count = synchronisationConnectionStatusRepository
                .countSynchronisationConnectionStatusWithStatus(SynchronisationConnectionStatusType.MODIFIED);
        if (count == 0L) {
            // everything is synchronized, time to remove everything.
            synchronisationConnectionStatusRepository.deleteAll();
            synchronisationConnectionRepository.deleteAll();
        }
    }

    /**
     * Return a list of all MeterDataCompany entities.
     *
     * @return List of MeterDataCompany entities
     */
    public List<MeterDataCompany> findAllMDCs() {
        return meterDataCompanyRepository.findAll();
    }
}
