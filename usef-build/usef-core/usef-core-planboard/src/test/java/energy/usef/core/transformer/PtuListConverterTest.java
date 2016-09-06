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

package energy.usef.core.transformer;

import static org.junit.Assert.assertEquals;

import energy.usef.core.data.xml.bean.message.DispositionAvailableRequested;
import energy.usef.core.data.xml.bean.message.PTU;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test case to test the conversion of a list of PTU's.
 */
public class PtuListConverterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PtuListConverterTest.class);

    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, PtuListConverter.class.getDeclaredConstructors().length);
        Constructor<PtuListConverter> constructor = PtuListConverter.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    @Test
    public void testSortOnStartOfList() {
        List<PTU> list = new ArrayList<>();
        list.add(createPTU(2, 1, 350, 0.08 / 1000));
        list.add(createPTU(1, 1, 350, 0.08 / 1000));
        
        List<PTU> normalized = PtuListConverter.normalize(list);
        assertEquals(2, normalized.size());
        
        assertEquals(BigInteger.valueOf(1), normalized.get(0).getStart());
        assertEquals(BigInteger.valueOf(2), normalized.get(1).getStart());
    }

    @Test
    public void testNormalizationOfPTUListWithDuration1() {
        List<PTU> list = new ArrayList<>();
        list.add(createPTU(2, 1, 450, 0.09 / 1000));
        list.add(createPTU(1, 1, 350, 0.08 / 1000));
        list.add(createPTU(3, 1, 550, 0.10 / 1000));
        
        List<PTU> normalized = PtuListConverter.normalize(list);
        assertEquals(3, normalized.size());
        
        assertEquals(BigInteger.valueOf(1), normalized.get(0).getStart());
        assertEquals(BigInteger.valueOf(2), normalized.get(1).getStart());
        assertEquals(BigInteger.valueOf(3), normalized.get(2).getStart());

        assertEquals(BigInteger.valueOf(350), normalized.get(0).getPower());
        assertEquals(BigInteger.valueOf(450), normalized.get(1).getPower());
        assertEquals(BigInteger.valueOf(550), normalized.get(2).getPower());

        assertEquals(BigDecimal.valueOf(0.08 / 1000), normalized.get(0).getPrice());
        assertEquals(BigDecimal.valueOf(0.09 / 1000), normalized.get(1).getPrice());
        assertEquals(BigDecimal.valueOf(0.10 / 1000), normalized.get(2).getPrice());
    }

    @Test
    public void testNormalizationOfPTUList() {
        List<PTU> list = new ArrayList<>();
        list.add(createPTU(1, 5, 350, 0.08 / 1000));
        list.add(createPTU(6, 2, 450, 0.09 / 1000));
        
        List<PTU> normalized = PtuListConverter.normalize(list);
        assertEquals(7, normalized.size());
        
        assertEquals(BigInteger.valueOf(1), normalized.get(0).getStart());
        assertEquals(BigInteger.valueOf(7), normalized.get(6).getStart());

        for (int i = 0; i < normalized.size(); i++) {
            assertEquals(BigInteger.valueOf(i + 1), normalized.get(i).getStart());
            assertEquals(BigInteger.ONE, normalized.get(i).getDuration());
            
            if (i < 5) {
                assertEquals(BigInteger.valueOf(350), normalized.get(i).getPower());
                assertEquals(BigDecimal.valueOf(0.08 / 1000), normalized.get(i).getPrice());
            } else {
                assertEquals(BigInteger.valueOf(450), normalized.get(i).getPower());
                assertEquals(BigDecimal.valueOf(0.09 / 1000), normalized.get(i).getPrice());
            }
        }
        
        // only used for debugging!
        for (PTU ptu : normalized) {
            LOGGER.debug("PTU " + ptu.getStart() + ": " + ptu.getDuration() + " " + ptu.getPower() + " " + ptu.getPrice());
        }
    }

    @Test
    public void testCompactOfPTUListDefault() {
        List<PTU> list = new ArrayList<>();
        list.add(createPTU(2, 1, 450, 0.09 / 1000));
        list.add(createPTU(1, 1, 350, 0.08 / 1000));
        list.add(createPTU(3, 1, 550, 0.10 / 1000));
        
        List<PTU> compacted = PtuListConverter.compact(list);
        assertEquals(3, compacted.size());

        for (int i = 0; i < compacted.size(); i++) {
            assertEquals(BigInteger.valueOf(i + 1), compacted.get(i).getStart());
            assertEquals(BigInteger.ONE, compacted.get(i).getDuration());
        }
        
        assertEquals(BigInteger.valueOf(350), compacted.get(0).getPower());
        assertEquals(BigInteger.valueOf(450), compacted.get(1).getPower());
        assertEquals(BigInteger.valueOf(550), compacted.get(2).getPower());

        assertEquals(BigDecimal.valueOf(0.08 / 1000), compacted.get(0).getPrice());
        assertEquals(BigDecimal.valueOf(0.09 / 1000), compacted.get(1).getPrice());
        assertEquals(BigDecimal.valueOf(0.10 / 1000), compacted.get(2).getPrice());
    }
    
    @Test
    public void testCompactOfPTUList() {
        List<PTU> list = new ArrayList<>();
        list.add(createPTU(1, 1, 350, 0.08 / 1000));
        list.add(createPTU(2, 1, 450, 0.09 / 1000));
        list.add(createPTU(3, 2, 450, 0.09 / 1000));
        list.add(createPTU(4, 1, 450, 0.09 / 1000));
        list.add(createPTU(5, 1, 550, 0.10 / 1000));
        list.add(createPTU(6, 1, 550, 0.10 / 1000));
        list.add(createPTU(7, 1, 550, 0.10 / 1000));
        list.add(createPTU(8, 1, 550, 0.10 / 1000));
        
        List<PTU> compacted = PtuListConverter.compact(list);
        assertEquals(3, compacted.size());
        
        assertEquals(BigInteger.valueOf(1), compacted.get(0).getStart());
        assertEquals(BigInteger.valueOf(2), compacted.get(1).getStart());
        assertEquals(BigInteger.valueOf(5), compacted.get(2).getStart());

        assertEquals(BigInteger.valueOf(1), compacted.get(0).getDuration());
        assertEquals(BigInteger.valueOf(4), compacted.get(1).getDuration());
        assertEquals(BigInteger.valueOf(4), compacted.get(2).getDuration());

        assertEquals(BigInteger.valueOf(350), compacted.get(0).getPower());
        assertEquals(BigInteger.valueOf(450), compacted.get(1).getPower());
        assertEquals(BigInteger.valueOf(550), compacted.get(2).getPower());

        assertEquals(BigDecimal.valueOf(0.08 / 1000), compacted.get(0).getPrice());
        assertEquals(BigDecimal.valueOf(0.09 / 1000), compacted.get(1).getPrice());
        assertEquals(BigDecimal.valueOf(0.10 / 1000), compacted.get(2).getPrice());
    }

    @Test
    public void testBothConversionMethods() {
        List<PTU> list = new ArrayList<>();
        list.add(createPTU(1, 5, 350, 0.08 / 1000));
        list.add(createPTU(6, 2, 450, 0.09 / 1000));
        
        List<PTU> normalized = PtuListConverter.normalize(list);
        List<PTU> compacted = PtuListConverter.compact(normalized);
        
        assertEquals(2, compacted.size());
        
        assertEquals(BigInteger.valueOf(1), compacted.get(0).getStart());
        assertEquals(BigInteger.valueOf(6), compacted.get(1).getStart());

        assertEquals(BigInteger.valueOf(5), compacted.get(0).getDuration());
        assertEquals(BigInteger.valueOf(2), compacted.get(1).getDuration());

        assertEquals(BigInteger.valueOf(350), compacted.get(0).getPower());
        assertEquals(BigInteger.valueOf(450), compacted.get(1).getPower());

        assertEquals(BigDecimal.valueOf(0.08 / 1000), compacted.get(0).getPrice());
        assertEquals(BigDecimal.valueOf(0.09 / 1000), compacted.get(1).getPrice());
        
    }
    
    private PTU createPTU(int start, int duration, int power, double price) {
        return createPTU(DispositionAvailableRequested.REQUESTED, start, duration, power, price);
    }
    
    private PTU createPTU(DispositionAvailableRequested disposition, int start, int duration, int power, double price) {
        PTU ptu = new PTU();
        ptu.setDisposition(disposition);
        ptu.setDuration(BigInteger.valueOf(duration));
        ptu.setStart(BigInteger.valueOf(start));
        ptu.setPower(BigInteger.valueOf(power));
        ptu.setPrice(BigDecimal.valueOf(price));
        return ptu;
    }
    
}
