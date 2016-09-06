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
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuFlexOffer;
import energy.usef.core.workflow.dto.FlexOfferDto;
import energy.usef.core.workflow.dto.PtuFlexOfferDto;

import java.math.BigInteger;
import java.util.List;

/**
 * Transformer class in charge of the mapping of {@link PtuFlexOffer} from the database to {@link
 * PtuFlexOfferDto} objects and {@link FlexOfferDto} objects.
 */
public class FlexOfferTransformer {

    private FlexOfferTransformer() {
        // private constructor, do nothing.
    }

    /**
     * Transforms a {@link PlanboardMessage} to a {@link FlexOfferDto}.
     *
     * @param offer
     * @return
     */
    public static FlexOfferDto transform(PlanboardMessage offer) {
        if (offer == null) {
            return null;
        }
        FlexOfferDto flexOfferDto = new FlexOfferDto();
        flexOfferDto.setPeriod(offer.getPeriod());
        flexOfferDto.setParticipantDomain(offer.getParticipantDomain());
        flexOfferDto.setConnectionGroupEntityAddress(offer.getConnectionGroup().getUsefIdentifier());
        flexOfferDto.setSequenceNumber(offer.getSequence());
        flexOfferDto.setFlexRequestSequenceNumber(offer.getOriginSequence());
        flexOfferDto.setExpirationDateTime(offer.getExpirationDate());
        return flexOfferDto;
    }

    /**
     * Transforms a {@link PtuFlexOffer} to its corresponding DTO object.
     *
     * @param ptuFlexOffer {@link PtuFlexOffer} database object.
     * @return a {@link PtuFlexOfferDto} DTO object.
     */
    public static PtuFlexOfferDto transformPtuFlexOffer(PtuFlexOffer ptuFlexOffer) {
        if (ptuFlexOffer == null) {
            return null;
        }
        PtuFlexOfferDto ptuFlexOfferDto = new PtuFlexOfferDto();
        ptuFlexOfferDto.setPower(ptuFlexOffer.getPower());
        ptuFlexOfferDto.setPrice(ptuFlexOffer.getPrice());
        ptuFlexOfferDto.setPtuIndex(BigInteger.valueOf(ptuFlexOffer.getPtuContainer().getPtuIndex()));
        return ptuFlexOfferDto;
    }

    /**
     * Transforms a complete list of {@link PtuFlexOffer} to a {@link FlexOfferDto}.
     *
     * @param ptuFlexOffers a {@link java.util.List} of {@link PtuFlexOffer}.
     * @return a {@link FlexOfferDto}.
     */
    public static FlexOfferDto transformPtuFlexOffers(List<PtuFlexOffer> ptuFlexOffers) {
        if (ptuFlexOffers == null || ptuFlexOffers.isEmpty()) {
            return null;
        }
        PtuFlexOffer firstPtuFlexOffer = ptuFlexOffers.get(0);
        FlexOfferDto flexOfferDto = new FlexOfferDto();
        flexOfferDto.setParticipantDomain(firstPtuFlexOffer.getParticipantDomain());
        flexOfferDto.setConnectionGroupEntityAddress(firstPtuFlexOffer.getConnectionGroup().getUsefIdentifier());
        flexOfferDto.setPeriod(firstPtuFlexOffer.getPtuContainer().getPtuDate());
        flexOfferDto.setFlexRequestSequenceNumber(firstPtuFlexOffer.getFlexRequestSequence());
        flexOfferDto.setSequenceNumber(firstPtuFlexOffer.getSequence());
        ptuFlexOffers.stream().forEach(ptuFlexOffer -> flexOfferDto.getPtus().add(transformPtuFlexOffer(ptuFlexOffer)));
        return flexOfferDto;
    }

    /**
     * Transforms {@link PtuFlexOfferDto} to a {@link PTU}.
     *
     * @param ptuFlexOfferDto
     * @return
     */
    public static PTU transformToPTU(PtuFlexOfferDto ptuFlexOfferDto) {
        if (ptuFlexOfferDto == null) {
            return null;
        }
        PTU ptu = new PTU();
        ptu.setStart(ptuFlexOfferDto.getPtuIndex());
        ptu.setPower(ptuFlexOfferDto.getPower());
        ptu.setPrice(ptuFlexOfferDto.getPrice());
        return ptu;
    }
}
