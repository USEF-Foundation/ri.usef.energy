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
import energy.usef.mdc.model.BalanceResponsibleParty;

import javax.ejb.Stateless;
import java.util.List;

/**
 * BalanceResponsibleParty Repository for BRP.
 */
@Stateless
public class BalanceResponsiblePartyRepository extends BaseRepository<BalanceResponsibleParty> {
    /**
     * Deletes {@Link BalanceResponsibleParty} entity by its domain.
     *
     * @param domain BalanceResponsibleParty domain
     */
    @SuppressWarnings("unchecked")
    public void deleteByDomain(String domain) {
        BalanceResponsibleParty balanceResponsibleParty = find(domain);
        if (balanceResponsibleParty != null) {
            entityManager.remove(balanceResponsibleParty);
        }
    }

    /**
     * Gets the entire list of {@link BalanceResponsibleParty} known objects by this Common Refernce Oparetor.
     *
     * @return {@link List} of {@link BalanceResponsibleParty}
     */
    @SuppressWarnings("unchecked")
    public List<BalanceResponsibleParty> findAll() {
        return getEntityManager().createQuery("SELECT participant FROM BalanceResponsibleParty participant").getResultList();
    }

}
