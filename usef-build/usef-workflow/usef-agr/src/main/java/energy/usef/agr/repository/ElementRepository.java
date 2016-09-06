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

import energy.usef.agr.model.Element;
import energy.usef.core.model.Connection;
import energy.usef.core.repository.BaseRepository;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.TemporalType;

import org.joda.time.LocalDate;

/**
 * Repository class to manage the {@link Element} objects.
 */
@Stateless
public class ElementRepository extends BaseRepository<Element> {

    /**
     * Deletes all Elements including the ElementDtuData from the database.
     */
    public void deleteAllElements() {
        entityManager.createQuery("DELETE FROM ElementDtuData").executeUpdate();
        entityManager.createQuery("DELETE FROM Element").executeUpdate();
    }

    /**
     * Finds the Elements which are related to active {@link Connection}.
     *
     * @param period
     * @return
     */
    public List<Element> findActiveElementsForPeriod(LocalDate period) {
        StringBuilder queryString = new StringBuilder();
        queryString.append("SELECT DISTINCT e FROM Element e, ConnectionGroupState cgs ");
        queryString.append("WHERE e.connectionEntityAddress = cgs.connection.entityAddress ");
        queryString.append("AND cgs.validFrom <= :period AND cgs.validUntil > :period ");

        return  entityManager.createQuery(queryString.toString(), Element.class)
                .setParameter("period", period.toDateMidnight().toDate(), TemporalType.DATE)
                .getResultList();
    }
}
