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

import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PrognosisTypeDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;

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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test for the Prognosis transformer.
 */
@RunWith(PowerMockRunner.class)
public class PrognosisTransformerTest {

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, PrognosisTransformer.class.getDeclaredConstructors().length);
        Constructor<PrognosisTransformer> constructor = PrognosisTransformer.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    @Test
    public void transformTest() {
        PtuContainer ptuContainer = new PtuContainer();
        ptuContainer.setPtuIndex(9);
        ptuContainer.setPtuDate(new LocalDate(2015, 2, 2));
        PtuPrognosis ptuPrognosis = new PtuPrognosis();
        ptuPrognosis.setPtuContainer(ptuContainer);
        ptuPrognosis.setType(PrognosisType.D_PROGNOSIS);
        ptuPrognosis.setPower(BigInteger.valueOf(10));

        PtuPrognosisDto ptuPrognosisDto = PrognosisTransformer.transform(ptuPrognosis);
        Assert.assertNotNull(ptuPrognosisDto);
        Assert.assertEquals(BigInteger.TEN, ptuPrognosisDto.getPower());
        Assert.assertEquals(BigInteger.valueOf(9), ptuPrognosisDto.getPtuIndex());
    }

    @Test
    public void testMapToPrognosis() {
        Mockito.when(corePlanboardBusinessService.findSinglePlanboardMessage(Mockito.anyLong(), Mockito.eq(DocumentType.D_PROGNOSIS),
                Mockito.anyString()))
                .thenReturn(new PlanboardMessage(DocumentType.D_PROGNOSIS, null, null, null, null, null, null, null));

        PrognosisDto prognosis = PrognosisTransformer.mapToPrognosis(buildPtuPrognosisList());
        Assert.assertNotNull(prognosis);
        Assert.assertEquals("dso.usef-example.com", prognosis.getParticipantDomain());
        Assert.assertEquals(1l, prognosis.getSequenceNumber().longValue());
        Assert.assertEquals(DateTimeUtil.parseDate("2015-03-03"), prognosis.getPeriod());
        Assert.assertEquals(96, prognosis.getPtus().size());
        Assert.assertEquals("ean.123456789012345678", prognosis.getConnectionGroupEntityAddress());
        Assert.assertEquals(PrognosisTypeDto.D_PROGNOSIS, prognosis.getType());
        Assert.assertTrue(prognosis.isSubstitute());
        IntStream.rangeClosed(1, 96).forEach(index -> {
            Assert.assertEquals(BigInteger.valueOf(index), prognosis.getPtus().get(index - 1).getPtuIndex());
            Assert.assertEquals(BigInteger.TEN, prognosis.getPtus().get(index - 1).getPower());
        });

        Assert.assertNull(PrognosisTransformer.mapToPrognosis(null));
        Assert.assertNull(PrognosisTransformer.mapToPrognosis(new ArrayList<>()));
    }

    private List<PtuPrognosis> buildPtuPrognosisList() {
        ConnectionGroup congestionPoint = new CongestionPointConnectionGroup();
        congestionPoint.setUsefIdentifier("ean.123456789012345678");
        final LocalDate period = DateTimeUtil.parseDate("2015-03-03");

        return IntStream.rangeClosed(1, 96).mapToObj(index -> {
            PtuContainer ptuContainer = new PtuContainer();
            ptuContainer.setPtuDate(period);
            ptuContainer.setPtuIndex(index);
            PtuPrognosis ptuPrognosis = new PtuPrognosis();
            ptuPrognosis.setConnectionGroup(congestionPoint);
            ptuPrognosis.setParticipantDomain("dso.usef-example.com");
            ptuPrognosis.setPower(BigInteger.TEN);
            ptuPrognosis.setPtuContainer(ptuContainer);
            ptuPrognosis.setSequence(1l);
            ptuPrognosis.setType(PrognosisType.D_PROGNOSIS);
            ptuPrognosis.setSubstitute(true);
            return ptuPrognosis;
        }).collect(Collectors.toList());
    }
}
