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

import energy.usef.brp.repository.CommonReferenceOperatorRepository;
import energy.usef.brp.repository.SynchronisationConnectionRepository;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.exception.RestError;

import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * This service class implements the business logic related to the CRO part of the common reference query.
 *
 */
@Stateless
public class BalanceResponsiblePartyValidationBusinessService {

    @Inject
    CommonReferenceOperatorRepository commonReferenceOperatorRepository;

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
     * Throws a {@link BusinessValidationException} if entityAddress is a duplicate SynchronisationConnection entityAddress.
     *
     * @param entityAddress an entityAddress
     * @throws BusinessValidationException
     */
    public void checkDuplicateSynchronisationConnection(String entityAddress) throws BusinessValidationException {
        if (synchronisationConnectionRepository.findByEntityAddress(entityAddress) != null) {
            throw new BusinessValidationException(RestError.DUPLICATE, "SynchronisationConnection", entityAddress);
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if domain is a non-existent SynchronisationConnection entityAddress.
     *
     * @param entityAddress domain an entityAddress
     * @throws BusinessValidationException
     */
    public void checkExistingSynchronisationConnection(String entityAddress) throws BusinessValidationException {
        if (synchronisationConnectionRepository.findByEntityAddress(entityAddress) == null) {
            throw new BusinessValidationException(RestError.NOT_FOUND, "SynchronisationConnection", entityAddress);
        }
    }
}
