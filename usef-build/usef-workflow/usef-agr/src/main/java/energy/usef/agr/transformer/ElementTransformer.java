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

package energy.usef.agr.transformer;

import energy.usef.agr.dto.ElementDto;
import energy.usef.agr.dto.ElementDtuDataDto;
import energy.usef.agr.dto.ElementTypeDto;
import energy.usef.agr.model.Element;
import energy.usef.agr.model.ElementDtuData;
import energy.usef.agr.model.ElementType;

import java.util.ArrayList;
import java.util.List;

/**
 * Transformer class for transforming Element related objects.
 */
public class ElementTransformer {

    private ElementTransformer() {
        // private constructor
    }

    /**
     * Transform an {@link Element} into an {@link ElementDto}.
     *
     * @param element the {@link Element} to transform.
     * @return an {@link ElementDto}
     */
    public static ElementDto transform(Element element) {
        ElementDto elementDto = new ElementDto();
        elementDto.setId(element.getId());
        elementDto.setConnectionEntityAddress(element.getConnectionEntityAddress());
        elementDto.setDtuDuration(element.getDtuDuration());
        elementDto.setElementType(ElementTypeDto.valueOf(element.getElementType().name()));
        elementDto.setProfile(element.getProfile());

        element.getElementDtuData().stream().map(ElementTransformer::transform)
                .forEach(elementDtuDataDto -> elementDto.getElementDtuData().add(elementDtuDataDto));

        return elementDto;
    }

    /**
     * Transform an {@link ElementDto} into an {@link Element}.
     *
     * @param elementDto the {@link ElementDto} to transform.
     * @return an {@link Element}
     */
    public static Element transform(ElementDto elementDto) {
        Element element = new Element();
        element.setId(elementDto.getId());
        element.setConnectionEntityAddress(elementDto.getConnectionEntityAddress());
        element.setDtuDuration(elementDto.getDtuDuration());
        element.setElementType(ElementType.valueOf(elementDto.getElementType().name()));
        element.setProfile(elementDto.getProfile());

        elementDto.getElementDtuData().stream().map(elementDtuDataDto -> transform(elementDtuDataDto, element))
                .forEach(elementDtuData -> element.getElementDtuData().add(elementDtuData));

        return element;
    }

    /**
     * Transform a {@list} of {@link Element}s into a {@List} of {@link ElementDto}s.
     *
     * @param elementList the {@list} of {@link Element}s to transform.
     * @return a {@List} of {@link ElementDto}s
     */
    public static List<ElementDto> transformToDtoList(List<Element> elementList) {
        List<ElementDto> elementDtoList = new ArrayList<>();
        elementList.stream().map(ElementTransformer::transform).forEach(element -> elementDtoList.add(element));

        return elementDtoList;
    }

    /**
     * Transform a {@list} of {@link ElementDto}s into a {@List} of {@link Element}s.
     *
     * @param elementDtoList the {@list} of {@link ElementDto}s to transform.
     * @return a {@List} of {@link Element}s
     */
    public static List<Element> transformToModelList(List<ElementDto> elementDtoList) {
        List<Element> elementList = new ArrayList<>();
        elementDtoList.stream().map(ElementTransformer::transform).forEach(elementDto -> elementList.add(elementDto));

        return elementList;
    }

    private static ElementDtuDataDto transform(ElementDtuData elementDtuData) {
        ElementDtuDataDto elementDtuDataDto = new ElementDtuDataDto();
        elementDtuDataDto.setId(elementDtuData.getId());
        elementDtuDataDto.setDtuIndex(elementDtuData.getDtuIndex());
        elementDtuDataDto.setProfileUncontrolledLoad(elementDtuData.getProfileUncontrolledLoad());
        elementDtuDataDto.setProfileAverageConsumption(elementDtuData.getProfileAverageConsumption());
        elementDtuDataDto.setProfileAverageProduction(elementDtuData.getProfileAverageProduction());
        elementDtuDataDto.setProfilePotentialFlexConsumption(elementDtuData.getProfilePotentialFlexConsumption());
        elementDtuDataDto.setProfilePotentialFlexProduction(elementDtuData.getProfilePotentialFlexProduction());

        return elementDtuDataDto;
    }

    private static ElementDtuData transform(ElementDtuDataDto elementDtuDataDto, Element element) {
        ElementDtuData elementDtuData = new ElementDtuData();
        elementDtuData.setId(elementDtuDataDto.getId());
        elementDtuData.setDtuIndex(elementDtuDataDto.getDtuIndex());
        elementDtuData.setProfileUncontrolledLoad(elementDtuDataDto.getProfileUncontrolledLoad());
        elementDtuData.setProfileAverageConsumption(elementDtuDataDto.getProfileAverageConsumption());
        elementDtuData.setProfileAverageProduction(elementDtuDataDto.getProfileAverageProduction());
        elementDtuData.setProfilePotentialFlexConsumption(elementDtuDataDto.getProfilePotentialFlexConsumption());
        elementDtuData.setProfilePotentialFlexProduction(elementDtuDataDto.getProfilePotentialFlexProduction());
        elementDtuData.setElement(element);

        return elementDtuData;
    }
}
