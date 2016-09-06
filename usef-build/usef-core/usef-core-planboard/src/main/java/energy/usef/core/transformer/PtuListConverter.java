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

import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.util.BigDecimalUtil;
import energy.usef.core.util.BigIntegerUtil;
import energy.usef.core.util.PtuUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to convert Ptu's which are in the XML Message with a specific duration to a extended list of PTU's.
 */
public class PtuListConverter {

    private PtuListConverter() {
        // empty constructor.
    }

    /**
     * Based on the duration field in a PTU, a PTU is duplicated and added to the returning list. The duration field is set to 1 in
     * this case. A PTU with duration 4 means that 4 PTU's are consecutive. To normalize the duration to 1 for every PTU, it is
     * easier to handle the PTU in the business logic (else you need to consider the duration every time a PTU is accessed).
     * 
     * @param ptus a list of XML PTU elements.
     * @return the normalized list of PTU's.
     */
    public static List<PTU> normalize(List<PTU> ptus) {
        PtuUtil.orderByStart(ptus);

        List<PTU> normalizedPtus = new ArrayList<>();
        for (PTU ptu : ptus) {
            int duration = ptu.getDuration().intValue();
            if (duration <= 1) {
                normalizedPtus.add(ptu);
            } else {
                for (int i = 0; i < duration; i++) {
                    PTU newPtu = new PTU();
                    newPtu.setStart(BigInteger.valueOf(ptu.getStart().longValue() + i));
                    newPtu.setDuration(BigInteger.ONE);
                    newPtu.setPower(ptu.getPower());
                    newPtu.setPrice(ptu.getPrice());
                    newPtu.setDisposition(ptu.getDisposition());
                    normalizedPtus.add(newPtu);
                }
            }
        }
        return normalizedPtus;
    }

    /**
     * Based on the duration field in a PTU, when the PTU are the same, they can be grouped and the duration is increased. So, when
     * 4 individual consecutive units have the same power, price and disposition, they can be grouped together and the duration is
     * summed. In case, the PTU's are grouped, the start of a PTU is not consecutive.
     * 
     * @param ptus a list of XML PTU elements.
     * @return the compacted list of PTU's.
     */
    public static List<PTU> compact(List<PTU> ptus) {
        PtuUtil.orderByStart(ptus);

        List<PTU> compactedPtus = new ArrayList<>();
        PTU lastKnownPTU = null;

        for (PTU ptu : ptus) {
            if (lastKnownPTU == null) {
                compactedPtus.add(ptu);
                lastKnownPTU = ptu;
            } else {
                if (arePTUsCompactable(lastKnownPTU, ptu)) {
                    lastKnownPTU.setDuration(
                            BigInteger.valueOf(lastKnownPTU.getDuration().longValue() + ptu.getDuration().longValue()));
                } else {
                    compactedPtus.add(ptu);
                    lastKnownPTU = ptu;
                }
            }
        }
        return compactedPtus;
    }

    private static boolean arePTUsCompactable(PTU ptu1, PTU ptu2) {
        return ptu1.getDisposition() == ptu2.getDisposition() && BigIntegerUtil.identical(ptu1.getPower(),ptu2.getPower()) &&
                BigDecimalUtil.identical (ptu1.getPrice(),ptu2.getPrice());
    }
}
