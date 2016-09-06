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

import energy.usef.core.model.BrpConnectionGroup;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.joda.time.LocalDate;

/**
 * This repository is used to manage {@link BrpConnectionGroup}.
 */
@Stateless
public class BrpConnectionGroupRepository extends BaseRepository<BrpConnectionGroup> {
    /**
     * Finds all {@link BrpConnectionGroup} active for the specific time.
     *
     * @param date date time
     * @return {@link BrpConnectionGroup} list
     */
    public List<BrpConnectionGroup> findActiveBrpConnectionGroups(LocalDate date) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT cgs.connectionGroup ");
        sql.append("FROM ConnectionGroupState cgs JOIN TREAT(cgs.connectionGroup AS BrpConnectionGroup) ");
        sql.append("WHERE cgs.validFrom <= :date ");
        sql.append("AND cgs.validUntil > :date ");
        Query query = entityManager.createQuery(sql.toString());
        query.setParameter("date", date.toDateMidnight().toDate());

        @SuppressWarnings("unchecked")
        List<BrpConnectionGroup> results = query.getResultList();
        if (results == null) {
            results = new ArrayList<>();
        }
        return results;
    }

    /**
     * Creates or finds the {@link BrpConnectionGroup} in a seperate transaction.
     * 
     * @param brpDomain BRP domain
     * @return BRP connection group
     */
    @Transactional(value = TxType.REQUIRES_NEW)
    public BrpConnectionGroup findOrCreate(String brpDomain) {
        BrpConnectionGroup group = getEntityManager().find(super.clazz, brpDomain,
                LockModeType.PESSIMISTIC_WRITE);
        if (group == null) {
            group = new BrpConnectionGroup();
            group.setUsefIdentifier(brpDomain);
            group.setBrpDomain(brpDomain);
            persist(group);
        }
        return group;
    }
}
