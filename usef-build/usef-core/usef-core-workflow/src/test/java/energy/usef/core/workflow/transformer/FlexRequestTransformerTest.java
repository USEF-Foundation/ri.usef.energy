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

package energy.usef.core.workflow.transformer;

import energy.usef.core.data.xml.bean.message.DispositionAvailableRequested;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuFlexRequest;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.dto.DispositionTypeDto;
import energy.usef.core.workflow.dto.FlexRequestDto;
import energy.usef.core.workflow.dto.PtuFlexRequestDto;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class in charge of the unit tests related to the {@link FlexRequestTransformer} class.
 */
public class FlexRequestTransformerTest {

    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, FlexRequestTransformer.class.getDeclaredConstructors().length);
        Constructor<FlexRequestTransformer> constructor = FlexRequestTransformer.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    @Test
    public void testTransformIsSuccessful() {
        PtuFlexRequestDto dto = FlexRequestTransformer.transform(buildPtuFlexRequest());
        Assert.assertNotNull(dto);
        Assert.assertEquals(DispositionTypeDto.AVAILABLE, dto.getDisposition());
        Assert.assertEquals(BigInteger.TEN, dto.getPower());
        Assert.assertEquals(BigInteger.valueOf(9), dto.getPtuIndex());
    }

    @Test
    public void testTransformListIsSuccessful() {
        FlexRequestDto flexRequestDto = FlexRequestTransformer.transformFlexRequest(buildPtuFlexRequestList());
        Assert.assertNotNull(flexRequestDto);
        Assert.assertEquals("ean.012345678901234567", flexRequestDto.getConnectionGroupEntityAddress());
        Assert.assertEquals("agr.usef-example.com", flexRequestDto.getParticipantDomain());
        Assert.assertEquals(new LocalDate(2015, 2, 2), flexRequestDto.getPeriod());
        Assert.assertEquals(1l, flexRequestDto.getPrognosisSequenceNumber().longValue());
        Assert.assertEquals(2l, flexRequestDto.getSequenceNumber().longValue());
        Assert.assertEquals(96, flexRequestDto.getPtus().size());

        Assert.assertNull(FlexRequestTransformer.transformFlexRequest(null));
    }

    @Test
    public void testTransformPtuToXml() {
        PtuFlexRequestDto dto = new PtuFlexRequestDto();
        dto.setDisposition(DispositionTypeDto.AVAILABLE);
        dto.setPtuIndex(BigInteger.valueOf(0L));
        dto.setPower(BigInteger.valueOf(120L));

        PTU ptu = FlexRequestTransformer.transformPtuToXml(dto);
        Assert.assertNotNull(ptu);

        Assert.assertEquals(BigInteger.valueOf(120L), ptu.getPower());
        Assert.assertEquals(DispositionAvailableRequested.AVAILABLE, ptu.getDisposition());

        Assert.assertNull(FlexRequestTransformer.transformPtuToXml(null));
    }

    @Test
    public void testTransformPtusToXmlList() {
        List<PtuFlexRequestDto> ptuFlexRequestDtos = new ArrayList<>();

        Assert.assertNotNull(FlexRequestTransformer.transformPtusToXml(ptuFlexRequestDtos));
        Assert.assertEquals(0, FlexRequestTransformer.transformPtusToXml(ptuFlexRequestDtos).size());

        PtuFlexRequestDto dto = new PtuFlexRequestDto();
        dto.setDisposition(DispositionTypeDto.AVAILABLE);
        dto.setPtuIndex(BigInteger.valueOf(0L));
        dto.setPower(BigInteger.valueOf(120L));

        ptuFlexRequestDtos.add(dto);

        Assert.assertNotNull(FlexRequestTransformer.transformPtusToXml(ptuFlexRequestDtos));
        Assert.assertEquals(1, FlexRequestTransformer.transformPtusToXml(ptuFlexRequestDtos).size());
    }


    private List<PtuFlexRequest> buildPtuFlexRequestList() {
        CongestionPointConnectionGroup congestionPointConnectionGroup = new CongestionPointConnectionGroup();
        congestionPointConnectionGroup.setUsefIdentifier("ean.012345678901234567");
        return IntStream.rangeClosed(1, 96).mapToObj(index -> {
            PtuContainer ptuContainer = new PtuContainer();
            ptuContainer.setPtuIndex(index);
            ptuContainer.setPtuDate(DateTimeUtil.parseDate("2015-02-02"));
            PtuFlexRequest ptuFlexRequest = new PtuFlexRequest();
            ptuFlexRequest.setPtuContainer(ptuContainer);
            ptuFlexRequest.setPower(BigInteger.valueOf(999));
            ptuFlexRequest.setDisposition(energy.usef.core.model.DispositionAvailableRequested.AVAILABLE);
            ptuFlexRequest.setParticipantDomain("agr.usef-example.com");
            ptuFlexRequest.setPrognosisSequence(1l);
            ptuFlexRequest.setConnectionGroup(congestionPointConnectionGroup);
            ptuFlexRequest.setSequence(2l);
            return ptuFlexRequest;
        }).collect(Collectors.toList());
    }

    private PtuFlexRequest buildPtuFlexRequest() {
        PtuContainer ptuContainer = new PtuContainer();
        ptuContainer.setPtuDate(new LocalDate(2015, 2, 2));
        ptuContainer.setPtuIndex(9);
        PtuFlexRequest ptuFlexRequest = new PtuFlexRequest();
        ptuFlexRequest.setPower(BigInteger.TEN);
        ptuFlexRequest.setPtuContainer(ptuContainer);
        ptuFlexRequest.setDisposition(energy.usef.core.model.DispositionAvailableRequested.AVAILABLE);
        return ptuFlexRequest;
    }

}
