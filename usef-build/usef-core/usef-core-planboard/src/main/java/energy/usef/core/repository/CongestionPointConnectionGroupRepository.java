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

import energy.usef.core.model.CongestionPointConnectionGroup;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.LockModeType;
import javax.persistence.TemporalType;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.joda.time.LocalDate;

/**
 * This repository is used to manage {@link CongestionPointConnectionGroup}.
 */
@Stateless
public class CongestionPointConnectionGroupRepository extends BaseRepository<CongestionPointConnectionGroup> {

    /**
     * Finds all congestion point connection groups.
     *
     * @return list of all congestion point connection groups
     */
    public List<CongestionPointConnectionGroup> findAll() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT cpcg ");
        sql.append("FROM CongestionPointConnectionGroup cpcg ");
        return getEntityManager().createQuery(sql.toString(), CongestionPointConnectionGroup.class).getResultList();
    }

    /**
     * Creates or finds the {@link CongestionPointConnectionGroup} in a seperate transaction.
     *
     * @param congestionPointEntityAddress {@link String} entity address of the congestion point.
     * @param dsoDomain                    {@link String} domain name of the related DSO.
     * @return a {@link CongestionPointConnectionGroup}
     */
    @Transactional(value = TxType.REQUIRES_NEW)
    public CongestionPointConnectionGroup findOrCreate(String congestionPointEntityAddress, String dsoDomain) {
        CongestionPointConnectionGroup group = getEntityManager().find(super.clazz, congestionPointEntityAddress,
                LockModeType.PESSIMISTIC_WRITE);
        if (group == null) {
            group = new CongestionPointConnectionGroup();
            group.setUsefIdentifier(congestionPointEntityAddress);
            group.setDsoDomain(dsoDomain);
            persist(group);
        }
        return group;
    }

    /**
     * Finds all {@link CongestionPointConnectionGroup} active for the specific time.
     *
     * @param date date time
     * @return {@link CongestionPointConnectionGroup} list
     */
    public List<CongestionPointConnectionGroup> findActiveCongestionPointConnectionGroup(LocalDate date) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT cg ");
        sql.append("FROM ConnectionGroupState cgs ");
        sql.append("  JOIN cgs.connectionGroup cg ");
        sql.append("WHERE cgs.validFrom <= :date ");
        sql.append("  AND cgs.validUntil > :date ");
        sql.append("  AND TYPE(cg) = CongestionPointConnectionGroup ");

        return (List<CongestionPointConnectionGroup>) entityManager.createQuery(sql.toString())
                .setParameter("date", date.toDateMidnight().toDate(), TemporalType.DATE)
                .getResultList();
    }
}
