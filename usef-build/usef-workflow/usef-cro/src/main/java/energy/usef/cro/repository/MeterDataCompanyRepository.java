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

package energy.usef.cro.repository;

import energy.usef.core.repository.BaseRepository;
import energy.usef.cro.model.DistributionSystemOperator;
import energy.usef.cro.model.MeterDataCompany;

import java.util.List;

import javax.ejb.Stateless;

/**
 * MeterDataCompany Repository for CRO.
 */
@Stateless
public class MeterDataCompanyRepository extends BaseRepository<MeterDataCompany> {
    /**
     * Gets MeterDataCompany entity by its domain.
     *
     * @param domain MeterDataCompany domain
     *
     * @return MeterDataCompany entity
     */
    public MeterDataCompany findByDomain(String domain) {

        List<MeterDataCompany> result = entityManager
                .createQuery("SELECT mdc FROM MeterDataCompany mdc WHERE mdc.domain = :domain", MeterDataCompany.class)
                .setParameter("domain", domain).getResultList();
        if (result == null || result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Deletes {@Link MeterDataCompany} entity by its domain.
     *
     * @param domain MeterDataCompany domain
     */
    @SuppressWarnings("unchecked")
    public void deleteByDomain(String domain) {
        MeterDataCompany meterDataCompany = findByDomain(domain);
        if (meterDataCompany != null) {
            entityManager.remove(meterDataCompany);
        }
    }

    /**
     * Gets the entire list of {@link DistributionSystemOperator} known objects by this Common Refernce Oparetor.
     *
     * @return {@link List} of {@link DistributionSystemOperator}
     */
    @SuppressWarnings("unchecked")
    public List<MeterDataCompany> findAll() {
        return getEntityManager().createQuery("SELECT participant FROM MeterDataCompany participant").getResultList();
    }

}
