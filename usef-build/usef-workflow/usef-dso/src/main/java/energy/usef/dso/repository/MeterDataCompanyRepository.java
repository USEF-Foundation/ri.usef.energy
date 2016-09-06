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
import energy.usef.dso.model.MeterDataCompany;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;

/**
 * MeterDataCompany Repository for DSO.
 */
@Stateless
public class MeterDataCompanyRepository extends BaseRepository<MeterDataCompany> {

    /**
     * Return a list of all MeterDataCompany entities.
     * 
     * @return List of MeterDataCompany entities
     */
    public List<MeterDataCompany> findAll() {
        List<MeterDataCompany> result = entityManager.createQuery("SELECT mdc FROM MeterDataCompany mdc", MeterDataCompany.class)
                .getResultList();

        if (result == null) {
            result = new ArrayList<>();
        }

        return result;
    }

}
