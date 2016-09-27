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

package energy.usef.agr.repository;

import energy.usef.agr.model.CommonReferenceOperator;
import energy.usef.core.repository.BaseRepository;

import java.util.List;

import javax.ejb.Stateless;

/**
 * Repository class for the {@link CommonReferenceOperator} entity. This class provides methods to interact with the BRP database.
 */
@Stateless
public class CommonReferenceOperatorRepository extends BaseRepository<CommonReferenceOperator> {
    /**
     * Gets Common Reference Operator entity by its domain.
     *
     * @param domain commonReferenceOperator domain
     *
     * @return CommonReferenceOperator entity
     */
    @SuppressWarnings("unchecked")
    public CommonReferenceOperator findByDomain(String domain) {

        List<CommonReferenceOperator> result = entityManager
                .createQuery(
                        "SELECT cro FROM CommonReferenceOperator cro WHERE cro.domain = :domain")
                .setParameter("domain", domain).getResultList();
        if (result == null || result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Deletes {@Link CommonReferenceOperator} entity by its domain.
     *
     * @param domain commonReferenceOperator domain
     */
    @SuppressWarnings("unchecked")
    public void deleteByDomain(String domain) {
        CommonReferenceOperator commonReferenceOperator = findByDomain(domain);
        if (commonReferenceOperator != null) {
            entityManager.remove(commonReferenceOperator);
        }
    }

    /**
     * Gets the entire list of {@link CommonReferenceOperator} known by this Balance Responsible Party.
     * 
     * @return {@link List} of {@link CommonReferenceOperator}
     */
    @SuppressWarnings("unchecked")
    public List<CommonReferenceOperator> findAll() {
        return getEntityManager().createQuery("SELECT operator FROM CommonReferenceOperator operator").getResultList();
    }

}
