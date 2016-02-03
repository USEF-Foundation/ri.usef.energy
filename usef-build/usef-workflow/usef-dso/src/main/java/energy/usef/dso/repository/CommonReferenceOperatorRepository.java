/*
 * Copyright 2015 USEF Foundation
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
import energy.usef.dso.model.CommonReferenceOperator;

import java.util.List;

import javax.ejb.Stateless;

/**
 * Repository class for the {@link CommonReferenceOperator} entity. This class provides methods to interact with the BRP database.
 */
@Stateless
public class CommonReferenceOperatorRepository extends BaseRepository<CommonReferenceOperator> {

    /**
     * Gets the entire list of {@link CommonReferenceOperator} known by this Balance Responsible Party.
     * 
     * @return {@link List} of {@link CommonReferenceOperator}
     */
    @SuppressWarnings("unchecked")
    public List<CommonReferenceOperator> findAll() {
        return getEntityManager().createQuery("SELECT operator FROM CommonReferenceOperator operator").getResultList();
    }

}
