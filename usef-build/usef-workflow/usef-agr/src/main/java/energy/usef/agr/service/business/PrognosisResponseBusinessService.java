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

import energy.usef.core.data.xml.bean.message.FlexOrderStatus;
import energy.usef.core.data.xml.bean.message.PrognosisResponse;

import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.Stateless;

/**
 * Service class in charge of operations regarding handling of PrognosisResponse message.
 */
@Stateless
public class PrognosisResponseBusinessService {

    /**
     * Determine the list of FlexOrders which could not be validated by the DSO.
     * 
     * @param response PrognosisResponse message
     * @return list of all non-validated sequences
     */
    public List<Long> getNotValidatedSequences(PrognosisResponse response) {
        return response.getFlexOrderStatus().stream()
                .filter(status -> !status.isIsValidated())
                .map(FlexOrderStatus::getSequence)
                .collect(Collectors.toList());
    }
}
