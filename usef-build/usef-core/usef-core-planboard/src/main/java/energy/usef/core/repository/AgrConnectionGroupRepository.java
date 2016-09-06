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

package energy.usef.core.repository;

import energy.usef.core.model.AgrConnectionGroup;

import javax.ejb.Stateless;
import javax.persistence.LockModeType;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

/**
 * Repository class in charge of the operations related to the {@link AgrConnectionGroup} entity.
 */
@Stateless
public class AgrConnectionGroupRepository extends BaseRepository<AgrConnectionGroup> {

    /**
     * Creates or finds the {@link AgrConnectionGroup} in a seperate transaction.
     * 
     * @param agrDomain AGR domain
     * @return AGR connection group
     */
    @Transactional(value = TxType.REQUIRES_NEW)
    public AgrConnectionGroup findOrCreate(String agrDomain) {
        AgrConnectionGroup group = getEntityManager().find(super.clazz, agrDomain,
                LockModeType.PESSIMISTIC_WRITE);
        if (group == null) {
            group = new AgrConnectionGroup();
            group.setUsefIdentifier(agrDomain);
            group.setAggregatorDomain(agrDomain);
            persist(group);
        }
        return group;
    }

}
