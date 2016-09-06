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

package energy.usef.brp.pbcfeederimpl;

import energy.usef.pbcfeeder.PbcFeederClient;
import energy.usef.pbcfeeder.dto.PbcStubDataDto;
import org.joda.time.LocalDate;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is the entry point for all data that is being 'fed' to the Aggregator PBCs.
 */
public class PbcFeederService {

    @Inject
    private PbcFeederClient pbcFeederClient;

    /**
     * This method sets the uncontrolled load on the ConnectionDto. This method can return data for multiple dates, so the ptu
     * index in the map is not a ptu index for one single day!
     *
     * @param date
     * @param startPtuIndex
     * @param amountOfPtus
     * @return
     */
    public Map<Integer, BigDecimal> retrieveApxPrices(LocalDate date, int startPtuIndex, int amountOfPtus) {
        List<PbcStubDataDto> pbcStubDataDtoList = pbcFeederClient.getPbcStubDataList(date, startPtuIndex, amountOfPtus);

        Map<Integer, BigDecimal> apxPrices = new HashMap<>();

        // if this method is used, the ptu index will be increased every ptu over multiple days
        for (int i = 0; i < pbcStubDataDtoList.size(); i++) {
            apxPrices.put(i + 1, BigDecimal.valueOf(pbcStubDataDtoList.get(i).getApx()));
        }

        return apxPrices;
    }

}
