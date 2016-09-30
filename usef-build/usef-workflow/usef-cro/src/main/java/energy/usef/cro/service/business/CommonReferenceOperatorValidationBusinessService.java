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

import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.exception.RestError;
import energy.usef.cro.repository.AggregatorRepository;
import energy.usef.cro.repository.BalanceResponsiblePartyRepository;
import energy.usef.cro.repository.DistributionSystemOperatorRepository;
import energy.usef.cro.repository.MeterDataCompanyRepository;

import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * This service class implements the business logic related to the CRO part of the common reference query.
 *
 */
@Stateless
public class CommonReferenceOperatorValidationBusinessService {


    @Inject
    AggregatorRepository aggregatorRepository;

    @Inject
    BalanceResponsiblePartyRepository balanceResponsiblePartyRepository;

    @Inject
    DistributionSystemOperatorRepository distributionSystemOperatorRepository;

    @Inject
    MeterDataCompanyRepository meterDataCompanyRepository;

    /**
     * Throws a {@link BusinessValidationException} if domain is a duplicate aggregator domain.
     *
     * @param domain domain name
     * @throws BusinessValidationException
     */
    public void checkDuplicateAggregatorDomain(String domain) throws BusinessValidationException {
        if (aggregatorRepository.findByDomain(domain) != null) {
            throw new BusinessValidationException(RestError.DUPLICATE, "Aggregator", domain);
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if domain is a non-existent aggregator domain.
     *
     * @param domain domain name
     * @throws BusinessValidationException
     */
    public void checkExistingAggregatorDomain(String domain) throws BusinessValidationException {
        if (aggregatorRepository.findByDomain(domain) == null) {
            throw new BusinessValidationException(RestError.NOT_FOUND, "Aggregator", domain);
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if domain is a duplicate balance responsible party domain.
     *
     * @param domain domain name
     * @throws BusinessValidationException
     */
    public void checkDuplicateBalanceResponsiblePartyDomain(String domain) throws BusinessValidationException {
        if (balanceResponsiblePartyRepository.findByDomain(domain) != null) {
            throw new BusinessValidationException(RestError.DUPLICATE, "BalanceResponsibleParty", domain);
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if domain is a non-existent balance responsible party domain.
     *
     * @param domain domain name
     * @throws BusinessValidationException
     */
    public void checkExistingBalanceResponsiblePartyDomain(String domain) throws BusinessValidationException {
        if (balanceResponsiblePartyRepository.findByDomain(domain) == null) {
            throw new BusinessValidationException(RestError.NOT_FOUND, "BalanceResponsibleParty", domain);
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if domain is a duplicate distribution system operator domain.
     *
     * @param domain domain name
     * @throws BusinessValidationException
     */
    public void checkDuplicateDistributionSystemOperatorDomain(String domain) throws BusinessValidationException {
        if (distributionSystemOperatorRepository.findByDomain(domain) != null) {
            throw new BusinessValidationException(RestError.DUPLICATE, "DistributionSystemOperator", domain);
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if domain is a non-existent distribution system operator domain.
     *
     * @param domain domain name
     * @throws BusinessValidationException
     */
    public void checkExistingDistributionSystemOperatorDomain(String domain) throws BusinessValidationException {
        if (distributionSystemOperatorRepository.findByDomain(domain) == null) {
            throw new BusinessValidationException(RestError.NOT_FOUND, "DistributionSystemOperator", domain);
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if domain is a duplicate aggregator domain.
     *
     * @param domain domain name
     * @throws BusinessValidationException
     */
    public void checkDuplicateMeterDataCompanyDomain(String domain) throws BusinessValidationException {
        if (meterDataCompanyRepository.findByDomain(domain) != null) {
            throw new BusinessValidationException(RestError.DUPLICATE, "MeterDataCompany", domain);
        }
    }

    /**
     * Throws a {@link BusinessValidationException} if domain is a non-existent aggregator domain.
     *
     * @param domain domain name
     * @throws BusinessValidationException
     */
    public void checkExistingMeterDataCompanyDomain(String domain) throws BusinessValidationException {
        if (meterDataCompanyRepository.findByDomain(domain) == null) {
            throw new BusinessValidationException(RestError.NOT_FOUND, "MeterDataCompany", domain);
        }
    }


}
