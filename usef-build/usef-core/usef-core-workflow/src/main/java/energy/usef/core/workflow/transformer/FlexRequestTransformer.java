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

import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.model.PtuFlexRequest;
import energy.usef.core.workflow.dto.FlexRequestDto;
import energy.usef.core.workflow.dto.PtuFlexRequestDto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transformer class for handling Flex related transformations.
 */
public class FlexRequestTransformer {

    private FlexRequestTransformer() {
        // private constructor for transformer
    }

    /**
     * Transform a {@link PtuFlexRequest} from the database model to a {@link
     * PtuFlexRequestDto}.
     *
     * @param flexRequest - The {@link PtuFlexRequest} to transform.
     * @return a {@link PtuFlexRequestDto} to use in the workflows.
     */
    public static PtuFlexRequestDto transform(PtuFlexRequest flexRequest) {
        PtuFlexRequestDto ptuFlexRequestDto = new PtuFlexRequestDto();
        if (flexRequest.getPower() != null) {
            ptuFlexRequestDto.setPower(flexRequest.getPower());
        }
        ptuFlexRequestDto.setDisposition(DispositionTransformer.transform(flexRequest.getDisposition()));
        ptuFlexRequestDto.setPtuIndex(BigInteger.valueOf(flexRequest.getPtuContainer().getPtuIndex()));
        return ptuFlexRequestDto;
    }

    /**
     * Transforms a list of {@link PtuFlexRequest} to a unique {@link FlexRequestDto} object.
     *
     * @param ptuFlexRequests {@link java.util.List} of {@link PtuFlexRequest}.
     * @return a {@link FlexRequestDto} or <code>null</code> if the input parameter is <code>null</code>
     * or empty.
     */
    public static FlexRequestDto transformFlexRequest(List<PtuFlexRequest> ptuFlexRequests) {
        if (ptuFlexRequests == null || ptuFlexRequests.isEmpty()) {
            return null;
        }
        FlexRequestDto flexRequestDto = new FlexRequestDto();
        PtuFlexRequest firstPtuFlexRequest = ptuFlexRequests.get(0);
        flexRequestDto.setConnectionGroupEntityAddress(firstPtuFlexRequest.getConnectionGroup().getUsefIdentifier());
        flexRequestDto.setParticipantDomain(firstPtuFlexRequest.getParticipantDomain());
        flexRequestDto.setPeriod(firstPtuFlexRequest.getPtuContainer().getPtuDate());
        flexRequestDto.setSequenceNumber(firstPtuFlexRequest.getSequence());
        flexRequestDto.setPrognosisSequenceNumber(firstPtuFlexRequest.getPrognosisSequence());
        ptuFlexRequests.stream().forEach(ptuFlexRequest -> flexRequestDto.getPtus().add(transform(ptuFlexRequest)));
        return flexRequestDto;
    }

    /**
     * Transforms a PTU of a Flex Request from the DTO format to the XML format as specified by the XSD specification.
     *
     * @param ptuFlexRequestDto {@link PtuFlexRequestDto}.
     * @return a {@link PTU}.
     */
    public static PTU transformPtuToXml(PtuFlexRequestDto ptuFlexRequestDto) {
        if (ptuFlexRequestDto == null) {
            return null;
        }
        PTU ptu = new PTU();
        ptu.setDuration(BigInteger.ONE);
        ptu.setPower(ptuFlexRequestDto.getPower());
        ptu.setStart(ptuFlexRequestDto.getPtuIndex());
        ptu.setDisposition(DispositionTransformer.transformToXml(ptuFlexRequestDto.getDisposition()));
        return ptu;
    }

    /**
     * Transforms a {@link List} of {@link PtuFlexRequestDto} to a list of {@link PTU}.
     *
     * @param ptuFlexRequestDtos {@link List} of {@link PtuFlexRequestDto}.
     * @return a {@link List} of {@link PTU} in the same order.
     */
    public static List<PTU> transformPtusToXml(List<PtuFlexRequestDto> ptuFlexRequestDtos) {
        if (ptuFlexRequestDtos == null || ptuFlexRequestDtos.isEmpty()) {
            return new ArrayList<>();
        }
        return ptuFlexRequestDtos.stream().map(FlexRequestTransformer::transformPtuToXml).collect(Collectors.toList());
    }
}
