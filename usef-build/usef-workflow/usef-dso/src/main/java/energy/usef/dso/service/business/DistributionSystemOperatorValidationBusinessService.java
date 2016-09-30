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
package energy.usef.dso.service.business;

import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.exception.RestError;
import energy.usef.dso.model.SynchronisationConnection;
import energy.usef.dso.repository.CommonReferenceOperatorRepository;
import energy.usef.dso.repository.SynchronisationCongestionPointRepository;
import energy.usef.dso.repository.SynchronisationConnectionRepository;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;


/**
 * This service class implements the business logic related to the CRO part of the common reference query.
 *
 */
@Stateless
public class DistributionSystemOperatorValidationBusinessService {

    @Inject
    CommonReferenceOperatorRepository commonReferenceOperatorRepository;

    @Inject
    SynchronisationCongestionPointRepository synchronisationCongestionPointRepository;

    @Inject
    SynchronisationConnectionRepository synchronisationConnectionRepository;

    /**
     * Throws a {@link BusinessValidationException} if domain is a duplicate common reference operator domain.
     *
     * @param domain domain name
     * @throws BusinessValidationException
     */
    public void checkDuplicateCommonReferenceOperatorDomain(String domain) throws BusinessValidationException {
        if (commonReferenceOperatorRepository.findByDomain(domain) != null) {
            throw new BusinessValidationException(RestError.DUPLICATE, "Common Reference Operator", domain);
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if domain is a non-existent common reference operator domain.
     *
     * @param domain domain name
     * @throws BusinessValidationException
     */
    public void checkExistingCommonReferenceOperatorDomain(String domain) throws BusinessValidationException {
        if (commonReferenceOperatorRepository.findByDomain(domain) == null) {
            throw new BusinessValidationException(RestError.NOT_FOUND, "Common Reference Operator", domain);
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if entityAddress is a duplicate common reference operator entityAddress.
     *
     * @param entityAddress domain name
     * @throws BusinessValidationException
     */
    public void checkDuplicateSynchronisationCongestionPoint(String entityAddress) throws BusinessValidationException {
        if (synchronisationCongestionPointRepository.findByEntityAddress(entityAddress) != null) {
            throw new BusinessValidationException(RestError.DUPLICATE, "Synchronisation Congestion Point", entityAddress);
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if entityAddress is a non-existent common reference operator entityAddress.
     *
     * @param entityAddress entityAddress name
     * @throws BusinessValidationException
     */
    public void checkExistingSynchronisationCongestionPoint(String entityAddress) throws BusinessValidationException {
        if (synchronisationCongestionPointRepository.findByEntityAddress(entityAddress) == null) {
            throw new BusinessValidationException(RestError.NOT_FOUND, "Synchronisation Congestion Point", entityAddress);
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if any entityAddress is a duplicate SynchronisationConnection entityAddress.
     *
     * @param entityAddresses a list of entityAddresses
     * @throws BusinessValidationException
     */
    public void checkDuplicateSynchronisationConnections(List<String> entityAddresses) throws BusinessValidationException {
        List<SynchronisationConnection> synchronisationConnections = synchronisationConnectionRepository.findByEntityAddresses(entityAddresses);

        if ( !synchronisationConnections.isEmpty()) {
            throw new BusinessValidationException(RestError.DUPLICATE, "Synchronisation Connection(s)", synchronisationConnections.stream().map(e -> e.getEntityAddress()).collect(Collectors.toList()));
        }
    }
}
