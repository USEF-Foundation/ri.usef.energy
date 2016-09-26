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

package energy.usef.mdc.repository;

import energy.usef.core.repository.BaseRepository;
import energy.usef.mdc.model.CommonReferenceOperator;

import java.util.List;

import javax.ejb.Stateless;

/**
 * Repository class for the {@link CommonReferenceOperatorRepository} entities.
 */
@Stateless
public class CommonReferenceOperatorRepository extends BaseRepository<CommonReferenceOperator> {

    /**
     * Finds all the registered CommonReferenceOperator entities.
     *
     * @return a {@link java.util.List} of {@link CommonReferenceOperator}.
     */
    public List<CommonReferenceOperator> findAll() {
        return getEntityManager().createQuery("SELECT cro FROM CommonReferenceOperator cro ", CommonReferenceOperator.class)
                .getResultList();
    }

    /**
     * Deletes {@Link CommonReferenceOperator} entity by its domain.
     *
     * @param domain commonReferenceOperator domain
     */
    @SuppressWarnings("unchecked")
    public void deleteByDomain(String domain) {
        CommonReferenceOperator participant = find(domain);
        if (participant != null) {
            entityManager.remove(participant);
        }
    }


    /**
     * Finds or creates a Common Reference Operator given its domain name.
     *
     * @param croDomain {@link String} domain name.
     * @return a {@link CommonReferenceOperator} entity.
     */
    public CommonReferenceOperator findOrCreate(String croDomain) {
        if (croDomain == null) {
            throw new IllegalArgumentException("CRO domain cannot be null for this query");
        }
        CommonReferenceOperator cro = find(croDomain);
        if (cro == null) {
            cro = new CommonReferenceOperator(croDomain);
            persist(cro);
        }
        return cro;
    }

}
