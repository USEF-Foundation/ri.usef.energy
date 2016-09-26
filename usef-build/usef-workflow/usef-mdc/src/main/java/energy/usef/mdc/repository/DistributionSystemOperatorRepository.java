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
import energy.usef.mdc.model.DistributionSystemOperator;

import javax.ejb.Stateless;
import java.util.List;

/**
 * Repository to handle all DistributionSystemOperator queries.
 */
@Stateless
public class DistributionSystemOperatorRepository extends BaseRepository<DistributionSystemOperator> {
    /**
     * Deletes {@Link DistributionSystemOperator} entity by its domain.
     *
     * @param domain DistributionSystemOperator domain
     */
    @SuppressWarnings("unchecked")
    public void deleteByDomain(String domain) {
        DistributionSystemOperator distributionSystemOperator = find(domain);
        if (distributionSystemOperator != null) {
            entityManager.remove(distributionSystemOperator);
        }
    }

    /**
     * Gets the entire list of {@link DistributionSystemOperator} known objects by this Common Refernce Oparetor.
     *
     * @return {@link List} of {@link DistributionSystemOperator}
     */
    @SuppressWarnings("unchecked")
    public List<DistributionSystemOperator> findAll() {
        return getEntityManager().createQuery("SELECT participant FROM DistributionSystemOperator participant").getResultList();
    }

}
