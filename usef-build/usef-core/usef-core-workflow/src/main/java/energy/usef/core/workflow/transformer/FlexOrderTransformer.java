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
import energy.usef.core.model.PtuFlexOrder;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.dto.PtuFlexOrderDto;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transformer class in charge of the transformation of {@link PtuFlexOrder} to
 * {@link FlexOrderDto}.
 */
public class FlexOrderTransformer {

    /**
     * Empty constructor preventing instantiations.
     */
    private FlexOrderTransformer() {
        // constructor preventing instantiation.
    }

    /**
     * Transform the PtuFlexOrder -> PtuFlexOrderDto.
     *
     * @param ptuFlexOrder
     * @return
     */
    public static PtuFlexOrderDto transform(PtuFlexOrder ptuFlexOrder) {
        if (ptuFlexOrder == null) {
            return null;
        }
        PtuFlexOrderDto ptuFlexOrderDto = new PtuFlexOrderDto();
        ptuFlexOrderDto.setPtuIndex(BigInteger.valueOf(ptuFlexOrder.getPtuContainer().getPtuIndex()));
        return ptuFlexOrderDto;
    }

    /**
     * Transforms a list of PtuFlexOrder from the database model to a related {@link FlexOrderDto}.
     *
     * @param ptuFlexOrders
     * @return FlexOrderDto
     */
    public static FlexOrderDto transformPtuFlexOrders(List<PtuFlexOrder> ptuFlexOrders) {
        if (ptuFlexOrders == null || ptuFlexOrders.isEmpty()) {
            return new FlexOrderDto();
        }

        FlexOrderDto flexOrderDto = new FlexOrderDto();
        PtuFlexOrder firsPtuFlexOrder = ptuFlexOrders.get(0);
        flexOrderDto.setPeriod(firsPtuFlexOrder.getPtuContainer().getPtuDate());
        flexOrderDto.setConnectionGroupEntityAddress(firsPtuFlexOrder.getConnectionGroup().getUsefIdentifier());
        flexOrderDto.setFlexOfferSequenceNumber(firsPtuFlexOrder.getFlexOfferSequence());
        flexOrderDto.setSequenceNumber(firsPtuFlexOrder.getSequence());
        flexOrderDto.setParticipantDomain(firsPtuFlexOrder.getParticipantDomain());
        flexOrderDto.setAcknowledgementStatus(
                AcknoledgementStatusTransformer.transform(firsPtuFlexOrder.getAcknowledgementStatus()));

        flexOrderDto.getPtus().addAll(
                ptuFlexOrders.stream().map(FlexOrderTransformer::transform).collect(Collectors.toList()));
        return flexOrderDto;
    }

    /**
     * Transforms a {@link PtuFlexOrderDto} to the XML version of a PTU. Power, Price, Duration and Start will be set. Disposition
     * will not be filled in.
     *
     * @param ptuFlexOrderDto a {@link PtuFlexOrderDto}.
     * @return a {@link PTU} according to the XSD specification.
     */
    public static PTU transformPtuFlexOrderDtoToPtu(PtuFlexOrderDto ptuFlexOrderDto) {
        if (ptuFlexOrderDto == null) {
            return null;
        }
        PTU ptu = new PTU();
        ptu.setPower(ptuFlexOrderDto.getPower());
        ptu.setStart(ptuFlexOrderDto.getPtuIndex());
        ptu.setPrice(ptuFlexOrderDto.getPrice());
        ptu.setDuration(BigInteger.ONE);
        return ptu;
    }

}
