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
import energy.usef.mdc.model.Connection;

import javax.ejb.Stateless;
import java.util.List;

/**
 * Repository class for the {@link Connection} entity.
 */
@Stateless
public class MdcConnectionRepository extends BaseRepository<Connection> {

    /**
     * Finds all the registered Connection entities in the MDC database.
     *
     * @return a {@link java.util.List} of {@link Connection}.
     */
    public List<Connection> findAllConnections() {
        String sql = "SELECT c FROM Connection c ";
        return getEntityManager().createQuery(sql, Connection.class).getResultList();
    }

    /**
     * Deletes {@Link Connection} entity by its entityAddress.
     *
     * @param entityAddress connection entityAddress
     */
    @SuppressWarnings("unchecked")
    public void deleteByEntityAddress(String entityAddress) {
        Connection connection = find(entityAddress);
        if (connection != null) {
            entityManager.remove(connection);
        }
    }

}
