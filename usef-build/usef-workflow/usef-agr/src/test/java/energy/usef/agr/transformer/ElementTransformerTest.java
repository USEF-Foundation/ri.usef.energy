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

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;

import junit.framework.TestCase;

/**
 * Test class for the {@link ElementTransformer}.
 */
public class ElementTransformerTest extends TestCase {

    public void testTransformElementToModel() throws Exception {
        ElementDto elementDto = new ElementDto();
        elementDto.setId("1");
        elementDto.setElementType(ElementTypeDto.MANAGED_DEVICE);
        elementDto.setConnectionEntityAddress("ean.1");
        elementDto.setDtuDuration(15);
        elementDto.setProfile("profile");
        elementDto.getElementDtuData().addAll(buildElementDtuDataDto());

        Element element = ElementTransformer.transform(elementDto);

        Assert.assertEquals(elementDto.getId(), element.getId());
        Assert.assertEquals(elementDto.getProfile(), element.getProfile());
        Assert.assertEquals(elementDto.getConnectionEntityAddress(), element.getConnectionEntityAddress());
        Assert.assertEquals(elementDto.getDtuDuration(), element.getDtuDuration());
        Assert.assertEquals(elementDto.getElementType(), ElementTypeDto.MANAGED_DEVICE);
        Assert.assertEquals(elementDto.getElementDtuData().size(), element.getElementDtuData().size());
        element.getElementDtuData().forEach(elementDtuData -> {
            Assert.assertEquals(BigInteger.valueOf(10 * elementDtuData.getDtuIndex().intValue()),
                    elementDtuData.getProfilePotentialFlexProduction());
            Assert.assertEquals(BigInteger.valueOf(20 * elementDtuData.getDtuIndex().intValue()),
                    elementDtuData.getProfilePotentialFlexConsumption());
            Assert.assertEquals(BigInteger.valueOf(30 * elementDtuData.getDtuIndex().intValue()),
                    elementDtuData.getProfileAverageProduction());
            Assert.assertEquals(BigInteger.valueOf(40 * elementDtuData.getDtuIndex().intValue()),
                    elementDtuData.getProfileAverageConsumption());
            Assert.assertEquals(BigInteger.valueOf(50 * elementDtuData.getDtuIndex().intValue()),
                    elementDtuData.getProfileUncontrolledLoad());
        });
    }

    public void testTransformModelToElement() throws Exception {
        Element element = new Element();
        element.setId("1");
        element.setElementType(ElementType.MANAGED_DEVICE);
        element.setConnectionEntityAddress("ean.1");
        element.setDtuDuration(15);
        element.setProfile("profile");
        element.getElementDtuData().addAll(buildElementDtuData());

        ElementDto elementDto = ElementTransformer.transform(element);

        Assert.assertEquals(element.getId(), elementDto.getId());
        Assert.assertEquals(element.getProfile(), elementDto.getProfile());
        Assert.assertEquals(element.getConnectionEntityAddress(), elementDto.getConnectionEntityAddress());
        Assert.assertEquals(element.getDtuDuration(), elementDto.getDtuDuration());
        Assert.assertEquals(element.getElementType(), ElementType.MANAGED_DEVICE);
        Assert.assertEquals(element.getElementDtuData().size(), elementDto.getElementDtuData().size());
        element.getElementDtuData().forEach(elementDtuData -> {
            Assert.assertEquals(BigInteger.valueOf(10 * elementDtuData.getDtuIndex().intValue()),
                    elementDtuData.getProfilePotentialFlexProduction());
            Assert.assertEquals(BigInteger.valueOf(20 * elementDtuData.getDtuIndex().intValue()),
                    elementDtuData.getProfilePotentialFlexConsumption());
            Assert.assertEquals(BigInteger.valueOf(30 * elementDtuData.getDtuIndex().intValue()),
                    elementDtuData.getProfileAverageProduction());
            Assert.assertEquals(BigInteger.valueOf(40 * elementDtuData.getDtuIndex().intValue()),
                    elementDtuData.getProfileAverageConsumption());
            Assert.assertEquals(BigInteger.valueOf(50 * elementDtuData.getDtuIndex().intValue()),
                    elementDtuData.getProfileUncontrolledLoad());
        });
    }

    private List<ElementDtuDataDto> buildElementDtuDataDto() {
        return IntStream.rangeClosed(1, 96).mapToObj(value -> {
            ElementDtuDataDto elementDtuDataDto = new ElementDtuDataDto();
            elementDtuDataDto.setId(Long.valueOf(value));
            elementDtuDataDto.setDtuIndex(value);
            elementDtuDataDto.setProfilePotentialFlexProduction(BigInteger.valueOf(10 * value));
            elementDtuDataDto.setProfilePotentialFlexConsumption(BigInteger.valueOf(20 * value));
            elementDtuDataDto.setProfileAverageProduction(BigInteger.valueOf(30 * value));
            elementDtuDataDto.setProfileAverageConsumption(BigInteger.valueOf(40 * value));
            elementDtuDataDto.setProfileUncontrolledLoad(BigInteger.valueOf(50 * value));
            return elementDtuDataDto;
        }).collect(Collectors.toList());
    }

    private List<ElementDtuData> buildElementDtuData() {
        return IntStream.rangeClosed(1, 96).mapToObj(value -> {
            ElementDtuData elementDtuData = new ElementDtuData();
            elementDtuData.setId(Long.valueOf(value));
            elementDtuData.setDtuIndex(value);
            elementDtuData.setProfilePotentialFlexProduction(BigInteger.valueOf(10 * value));
            elementDtuData.setProfilePotentialFlexConsumption(BigInteger.valueOf(20 * value));
            elementDtuData.setProfileAverageProduction(BigInteger.valueOf(30 * value));
            elementDtuData.setProfileAverageConsumption(BigInteger.valueOf(40 * value));
            elementDtuData.setProfileUncontrolledLoad(BigInteger.valueOf(50 * value));
            return elementDtuData;
        }).collect(Collectors.toList());
    }

}
