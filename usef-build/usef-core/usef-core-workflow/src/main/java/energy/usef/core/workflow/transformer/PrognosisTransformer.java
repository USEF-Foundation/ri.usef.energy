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

import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.workflow.dto.PrognosisDto;
import energy.usef.core.workflow.dto.PrognosisTypeDto;
import energy.usef.core.workflow.dto.PtuPrognosisDto;

import java.math.BigInteger;
import java.util.List;

/**
 * Transformer class for for transforming prognosis related objects.
 */
public class PrognosisTransformer {

    private PrognosisTransformer() {
        // private method for transformer
    }

    /**
     * Transform a {@link PtuPrognosis} to its DTO format.
     *
     * @param prognosis a {@link PtuPrognosis} to be transformed.
     * @return a new {@link PtuPrognosisDto}.
     */
    public static PtuPrognosisDto transform(PtuPrognosis prognosis) {
        PtuPrognosisDto ptuPrognosisDto = new PtuPrognosisDto();
        ptuPrognosisDto.setPtuIndex(BigInteger.valueOf(prognosis.getPtuContainer().getPtuIndex()));
        ptuPrognosisDto.setPower(prognosis.getPower());
        return ptuPrognosisDto;
    }

    /**
     * Maps a list of {@link PtuPrognosis} to a {PtuPrognosisDto} object.
     *
     * @param ptuPrognoses {@link List} of {@link PtuPrognosis} which should all have the same
     * period, connection group and sequence number.
     * @return a {@link PrognosisDto}.
     */
    public static PrognosisDto mapToPrognosis(List<PtuPrognosis> ptuPrognoses) {
        if (ptuPrognoses == null || ptuPrognoses.isEmpty()) {
            return null;
        }

        PtuPrognosis firstPtuPrognosis = ptuPrognoses.get(0);

        PrognosisDto prognosisDto = new PrognosisDto();
        // get one of the PtuPrognoses to have the common data (participant domain, PTU info, sequence number, ...)
        PrognosisTypeDto prognosisType = firstPtuPrognosis.getType() == PrognosisType.A_PLAN ?
                PrognosisTypeDto.A_PLAN :
                PrognosisTypeDto.D_PROGNOSIS;

        // set the values in the DTO
        prognosisDto.setConnectionGroupEntityAddress(firstPtuPrognosis.getConnectionGroup().getUsefIdentifier());
        prognosisDto.setPeriod(firstPtuPrognosis.getPtuContainer().getPtuDate());
        prognosisDto.setType(prognosisType);
        prognosisDto.setParticipantDomain(firstPtuPrognosis.getParticipantDomain());
        prognosisDto.setSequenceNumber(firstPtuPrognosis.getSequence());
        prognosisDto.setSubstitute(firstPtuPrognosis.isSubstitute());

        // set the values per PTU
        ptuPrognoses.stream().forEach(ptuPrognosis -> prognosisDto.getPtus().add(transform(ptuPrognosis)));
        return prognosisDto;
    }
}
