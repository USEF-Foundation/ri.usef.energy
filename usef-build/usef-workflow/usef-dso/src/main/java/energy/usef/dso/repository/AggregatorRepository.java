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

package energy.usef.dso.repository;

import energy.usef.core.repository.BaseRepository;
import energy.usef.dso.model.Aggregator;

/**
 * Repository class for the {@link Aggregator} entity.
 */
public class AggregatorRepository extends BaseRepository<Aggregator> {

    /**
     * Find or creates a new {@link Aggregator} identified with.
     *
     * @param aggregatorDomain {@link String} domain name of the aggregator.
     * @return a {@link Aggregator}.
     */
    public Aggregator findOrCreate(String aggregatorDomain) {
        Aggregator aggregator = this.find(aggregatorDomain);
        if (aggregator == null) {
            aggregator = new Aggregator();
            aggregator.setDomain(aggregatorDomain);
            persist(aggregator);
        }
        return aggregator;
    }

}
