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

package energy.usef.agr.service.business;

import energy.usef.agr.dto.ElementDto;
import energy.usef.agr.model.Element;
import energy.usef.agr.repository.ElementRepository;
import energy.usef.agr.transformer.ElementTransformer;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.LocalDate;

/**
 * Business Service to manage logice related to the Element store.
 */
public class AgrElementBusinessService {

    @Inject
    private ElementRepository elementRepository;

    /**
     * Finds The active Elements for that period and transforms them to DTOs.
     *
     * @param period The relevant period.
     * @return A list of {@link ElementDto}
     */
    public List<ElementDto> findElementDtos(LocalDate period) {
        List<Element> elements = elementRepository.findActiveElementsForPeriod(period);
        return ElementTransformer.transformToDtoList(elements);
    }

    /**
     * Persist a list of {@link Element}'s.
     *
     * @param elementList the list of {@link Element} objects to be persisted.
     */
    public void createElements(List<Element> elementList) {
        elementRepository.deleteAllElements();
        elementList.forEach(element -> elementRepository.persist(element));
    }
}
