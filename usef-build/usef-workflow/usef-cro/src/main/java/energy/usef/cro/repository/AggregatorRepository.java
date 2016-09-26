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
import energy.usef.cro.model.Aggregator;

import java.util.List;

import javax.ejb.Stateless;

/**
 * Aggregator Repository for CRO.
 */
@Stateless
public class AggregatorRepository extends BaseRepository<Aggregator> {
    /**
     * Gets Aggregator entity by its domain.
     *
     * @param domain aggregator domain
     *
     * @return Aggregator entity
     */
    @SuppressWarnings("unchecked")
    public Aggregator findByDomain(String domain) {

        List<Aggregator> result = entityManager
                .createQuery(
                        "SELECT agr FROM Aggregator agr WHERE agr.domain = :domain")
                .setParameter("domain", domain).getResultList();
        if (result == null || result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Deletes {@Link Aggregator} entity by its domain.
     *
     * @param domain aggregator domain
     */
    @SuppressWarnings("unchecked")
    public void deleteByDomain(String domain) {
        Aggregator aggregator = findByDomain(domain);
        if (aggregator != null) {
            entityManager.remove(aggregator);
        }
    }

    /**
     * Gets the entire list of {@link Aggregator} known objects by this Common Reference Oparetor.
     *
     * @return {@link List} of {@link Aggregator}
     */
    @SuppressWarnings("unchecked")
    public List<Aggregator> findAll() {
        return getEntityManager().createQuery("SELECT participant FROM Aggregator participant").getResultList();
    }

}
