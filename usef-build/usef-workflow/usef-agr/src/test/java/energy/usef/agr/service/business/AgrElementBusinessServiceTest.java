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

import static org.junit.Assert.assertEquals;

import energy.usef.agr.dto.ElementDto;
import energy.usef.agr.model.Element;
import energy.usef.agr.model.ElementType;
import energy.usef.agr.repository.ElementRepository;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Element Business Service Test
 */
@RunWith(PowerMockRunner.class)
public class AgrElementBusinessServiceTest {

    @Mock
    private ElementRepository elementRepository;

    private AgrElementBusinessService agrElementBusinessService;

    @Before
    public void before() {
        agrElementBusinessService = new AgrElementBusinessService();
        Whitebox.setInternalState(agrElementBusinessService, elementRepository);
    }

    @Test
    public void testFindElementDtos() throws Exception {
        LocalDate period = new LocalDate("2015-09-29");
        List<Element> elementList = buildTestElementList();

        Mockito.when(elementRepository.findActiveElementsForPeriod(period))
                .thenReturn(elementList);
        List<ElementDto> resultDtos = agrElementBusinessService.findElementDtos(period);

        Mockito.verify(elementRepository, Mockito.times(1)).findActiveElementsForPeriod(period);

        assertEquals(elementList.size(),resultDtos.size() );
    }

    @Test
    public void testCreateElements() throws Exception {
        List<Element> elementList = buildTestElementList();

        agrElementBusinessService.createElements(elementList);

        Mockito.verify(elementRepository, Mockito.times(1)).deleteAllElements();
        Mockito.verify(elementRepository, Mockito.times(1)).persist(Matchers.any(Element.class));
    }

    public List<Element> buildTestElementList() {
        Element element = new Element();
        element.setId("ADS1");
        element.setDtuDuration(15);
        element.setConnectionEntityAddress("conn.1");
        element.setElementType(ElementType.MANAGED_DEVICE);
        element.setProfile("PBCFeeder");

        List<Element> elementList = new ArrayList<>();
        elementList.add(element);
        return elementList;
    }

}
