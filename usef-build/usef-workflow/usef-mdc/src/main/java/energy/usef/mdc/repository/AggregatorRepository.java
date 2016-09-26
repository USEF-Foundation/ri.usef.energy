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
import energy.usef.mdc.model.Aggregator;

import javax.ejb.Stateless;
import java.util.List;

/**
 * Repository class for the {@link Aggregator} entity.
 */
@Stateless
public class AggregatorRepository extends BaseRepository<Aggregator> {

    /**
     * Gets the entire list of {@link Aggregator} known objects by this Meter Data Company
     *
     * @return {@link List} of {@link Aggregator}
     */
    @SuppressWarnings("unchecked")
    public List<Aggregator> findAll() {
        return getEntityManager().createQuery("SELECT participant FROM Aggregator participant").getResultList();
    }

    /**
     * Finds or creates an aggregator given the his domain name.
     *
     * @param aggregatorDomain {@link String} domain name.
     * @return a {@link Aggregator}.
     */
    public Aggregator findOrCreate(String aggregatorDomain) {
        if (aggregatorDomain == null) {
            throw new IllegalArgumentException("Aggregator domain cannot be null for this query");
        }
        Aggregator aggregator = find(aggregatorDomain);
        if (aggregator == null) {
            aggregator = new Aggregator(aggregatorDomain);
            persist(aggregator);
        }
        return aggregator;
    }
}
