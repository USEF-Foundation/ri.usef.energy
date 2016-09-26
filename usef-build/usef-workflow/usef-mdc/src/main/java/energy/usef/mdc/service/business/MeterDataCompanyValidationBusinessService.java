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
package energy.usef.mdc.service.business;

import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.exception.RestError;
import energy.usef.mdc.repository.BalanceResponsiblePartyRepository;
import energy.usef.mdc.repository.CommonReferenceOperatorRepository;
import energy.usef.mdc.repository.DistributionSystemOperatorRepository;
import energy.usef.mdc.repository.MdcConnectionRepository;

import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * This service class implements the business logic related to validations for the MeterDataCompany.
 *
 */
@Stateless
public class  MeterDataCompanyValidationBusinessService {


    @Inject
    MdcConnectionRepository connectionRepository;

    @Inject
    BalanceResponsiblePartyRepository balanceResponsiblePartyRepository;

    @Inject
    DistributionSystemOperatorRepository distributionSystemOperatorRepository;

    @Inject
    CommonReferenceOperatorRepository commonReferenceOperatorRepository;

    /**
     * Throws a {@link BusinessValidationException} if entityAddress is a duplicate Connection entityAddress.
     *
     * @param entityAddress entity Address
     * @throws BusinessValidationException
     */
    public void checkDuplicateConnection(String entityAddress) throws BusinessValidationException {
        if (connectionRepository.find(entityAddress) != null) {
            throw new BusinessValidationException(RestError.DUPLICATE, "Connection", entityAddress);
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if entityAddress is a non-existent entityAddress.
     *
     * @param entityAddress entity Address
     * @throws BusinessValidationException
     */
    public void checkExistingConnection(String entityAddress) throws BusinessValidationException {
        if (connectionRepository.find(entityAddress) == null) {
            throw new BusinessValidationException(RestError.NOT_FOUND, "Connection", entityAddress);
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if domain is a duplicate BalanceResponsibleParty domain.
     *
     * @param domain domain name
     * @throws BusinessValidationException
     */
    public void checkDuplicateBalanceResponsiblePartyDomain(String domain) throws BusinessValidationException {
        if (balanceResponsiblePartyRepository.find(domain) != null) {
            throw new BusinessValidationException(RestError.DUPLICATE, "BalanceResponsibleParty", domain);
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if domain is a non-existent BalanceResponsibleParty domain.
     *
     * @param domain domain name
     * @throws BusinessValidationException
     */
    public void checkExistingBalanceResponsiblePartyDomain(String domain) throws BusinessValidationException {
        if (balanceResponsiblePartyRepository.find(domain) == null) {
            throw new BusinessValidationException(RestError.NOT_FOUND, "BalanceResponsibleParty", domain);
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if domain is a duplicate DistributionSystemOperator domain.
     *
     * @param domain domain name
     * @throws BusinessValidationException
     */
    public void checkDuplicateDistributionSystemOperatorDomain(String domain) throws BusinessValidationException {
        if (distributionSystemOperatorRepository.find(domain) != null) {
            throw new BusinessValidationException(RestError.DUPLICATE, "DistributionSystemOperator", domain);
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if domain is a non-existent DistributionSystemOperator system operator domain.
     *
     * @param domain domain name
     * @throws BusinessValidationException
     */
    public void checkExistingDistributionSystemOperatorDomain(String domain) throws BusinessValidationException {
        if (distributionSystemOperatorRepository.find(domain) == null) {
            throw new BusinessValidationException(RestError.NOT_FOUND, "DistributionSystemOperator", domain);
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if domain is a duplicate CommonReferenceOperator domain.
     *
     * @param domain domain name
     * @throws BusinessValidationException
     */
    public void checkDuplicateCommonReferenceOperatorDomain(String domain) throws BusinessValidationException {
        if (commonReferenceOperatorRepository.find(domain) != null) {
            throw new BusinessValidationException(RestError.DUPLICATE, "CommonReferenceOperator", domain);
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if domain is a non-existent CommonReferenceOperator domain.
     *
     * @param domain domain name
     * @throws BusinessValidationException
     */
    public void checkExistingCommonReferenceOperatorDomain(String domain) throws BusinessValidationException {
        if (commonReferenceOperatorRepository.find(domain) == null) {
            throw new BusinessValidationException(RestError.NOT_FOUND, "CommonReferenceOperator", domain);
        }
    }


}
