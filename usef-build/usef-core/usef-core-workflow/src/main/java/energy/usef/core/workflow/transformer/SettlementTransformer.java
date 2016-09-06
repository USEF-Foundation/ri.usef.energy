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

import energy.usef.core.data.xml.bean.message.PTUSettlement;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.FlexOrderSettlement;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuSettlement;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.dto.FlexOrderSettlementDto;
import energy.usef.core.workflow.dto.PtuSettlementDto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Transformer class for Settlement-related items.
 */
public final class SettlementTransformer {

    private SettlementTransformer() {
        // prevents instantiation
    }

    /**
     * Transforms one ptu settlement DTO object to its related model version.
     *
     * @param ptuSettlementDto {@link PtuSettlementDto} PTU settlement DTO.
     * @param ptuContainer {@link PtuContainer} related PTU Container.
     * @param flexOrderSettlement {@link FlexOrderSettlement} parent Flex Order Settlement item.
     * @return a {@link PtuSettlement} object ready to be persisted.
     */
    public static PtuSettlement transformPtuSettlementDto(PtuSettlementDto ptuSettlementDto,
            PtuContainer ptuContainer, FlexOrderSettlement flexOrderSettlement) {
        PtuSettlement ptuSettlement = new PtuSettlement();
        ptuSettlement.setPtuContainer(ptuContainer);
        ptuSettlement.setActualPower(ptuSettlementDto.getActualPower());
        ptuSettlement.setConnectionGroup(flexOrderSettlement.getConnectionGroup());
        ptuSettlement.setDeliveredFlexPower(ptuSettlementDto.getDeliveredFlexPower());
        ptuSettlement.setSequence(flexOrderSettlement.getSequence());
        ptuSettlement.setFlexOrderSettlement(flexOrderSettlement);
        ptuSettlement.setOrderedFlexPower(ptuSettlementDto.getOrderedFlexPower());
        ptuSettlement.setPenalty(ptuSettlementDto.getPenalty());
        ptuSettlement.setPowerDeficiency(ptuSettlementDto.getPowerDeficiency());
        ptuSettlement.setPrice(ptuSettlementDto.getPrice());
        ptuSettlement.setPrognosisPower(ptuSettlementDto.getPrognosisPower());
        return ptuSettlement;
    }

    /**
     * Transforms a Flex Order Settlement model item to its DTO format.
     *
     * @param flexOrderSettlement a {@link FlexOrderSettlement} entity.
     * @return a {@link FlexOrderSettlementDto} object.
     */
    public static FlexOrderSettlementDto mapModelToDto(FlexOrderSettlement flexOrderSettlement) {
        FlexOrderSettlementDto flexOrderSettlementDto = new FlexOrderSettlementDto(flexOrderSettlement.getPeriod());
        FlexOrderDto flexOrderDto = new FlexOrderDto();
        flexOrderDto.setPeriod(flexOrderSettlement.getFlexOrder().getPeriod());
        flexOrderDto.setParticipantDomain(flexOrderSettlement.getFlexOrder().getParticipantDomain());
        flexOrderDto.setAcknowledgementStatus(null);
        flexOrderDto.setConnectionGroupEntityAddress(flexOrderSettlement.getFlexOrder().getConnectionGroup().getUsefIdentifier());
        flexOrderDto.setFlexOfferSequenceNumber(flexOrderSettlement.getFlexOrder().getOriginSequence());
        flexOrderDto.setSequenceNumber(flexOrderSettlement.getFlexOrder().getSequence());
        flexOrderSettlementDto.setFlexOrder(flexOrderDto);
        flexOrderSettlement.getPtuSettlements().stream()
                .map(SettlementTransformer::mapModelToDto)
                .forEach(ptuSettlementDto -> flexOrderSettlementDto.getPtuSettlementDtos().add(ptuSettlementDto));
        return flexOrderSettlementDto;
    }

    private static PtuSettlementDto mapModelToDto(PtuSettlement ptuSettlement) {
        PtuSettlementDto ptuSettlementDto = new PtuSettlementDto();
        ptuSettlementDto.setPtuIndex(BigInteger.valueOf(ptuSettlement.getPtuContainer().getPtuIndex()));
        ptuSettlementDto.setActualPower(ptuSettlement.getActualPower());
        ptuSettlementDto.setDeliveredFlexPower(ptuSettlement.getDeliveredFlexPower());
        ptuSettlementDto.setOrderedFlexPower(ptuSettlement.getOrderedFlexPower());
        ptuSettlementDto.setPowerDeficiency(ptuSettlement.getPowerDeficiency());
        ptuSettlementDto.setPrice(ptuSettlement.getPrice());
        ptuSettlementDto.setPrognosisPower(ptuSettlement.getPrognosisPower());
        return ptuSettlementDto;
    }

    /**
     * Transforms a Flex Order Settlement item as specified by the XSD specification into a Flex Order Settlement DTO object.
     * @param xmlflexOrderSettlement {@link energy.usef.core.data.xml.bean.message.FlexOrderSettlement} object.
     * @param participantDomain {@link String} participant domain name related to the settlement item.
     * @return a {@link FlexOrderSettlementDto}.
     */
    public static FlexOrderSettlementDto mapXmlToDto(
            energy.usef.core.data.xml.bean.message.FlexOrderSettlement xmlflexOrderSettlement, String participantDomain) {
        FlexOrderSettlementDto flexOrderSettlementDto = new FlexOrderSettlementDto(xmlflexOrderSettlement.getPeriod());
        FlexOrderDto flexOrderDto = new FlexOrderDto();
        flexOrderDto.setSequenceNumber(Long.valueOf(xmlflexOrderSettlement.getOrderReference()));
        flexOrderDto.setPeriod(xmlflexOrderSettlement.getPeriod());
        if (xmlflexOrderSettlement.getCongestionPoint() == null) {
            // BRP case
            flexOrderDto.setConnectionGroupEntityAddress(participantDomain);
        } else {
            // DSO case
            flexOrderDto.setConnectionGroupEntityAddress(xmlflexOrderSettlement.getCongestionPoint());
        }
        flexOrderSettlementDto.setFlexOrder(flexOrderDto);
        xmlflexOrderSettlement.getPTUSettlement()
                .stream()
                .map(SettlementTransformer::transformPtuXmlToDto)
                .forEach(ptus -> flexOrderSettlementDto.getPtuSettlementDtos().addAll(ptus));
        return flexOrderSettlementDto;
    }

    /**
     * Transforms one PTUSettlement items (specified by the XSD specification) to the DTO format (1 or more items).
     *
     * @param ptuSettlement a {@link PTUSettlement} item.
     * @return a {@link List} of {@link PtuSettlementDto} (multiple items if the duration of the ptuSettlement in input is
     * greater than 1).
     */
    public static List<PtuSettlementDto> transformPtuXmlToDto(PTUSettlement ptuSettlement) {
        List<PtuSettlementDto> result = new ArrayList<>();
        int duration = ptuSettlement.getDuration() == null ? 1 : ptuSettlement.getDuration().intValue();

        for (int i = 0; i < duration; ++i) {
            PtuSettlementDto ptuSettlementDto = new PtuSettlementDto();
            ptuSettlementDto.setPtuIndex(ptuSettlement.getStart().add(BigInteger.valueOf(i)));
            ptuSettlementDto.setActualPower(ptuSettlement.getActualPower());
            ptuSettlementDto.setDeliveredFlexPower(ptuSettlement.getDeliveredFlexPower());
            ptuSettlementDto.setOrderedFlexPower(ptuSettlement.getOrderedFlexPower());
            if (ptuSettlement.getPrice() != null && ptuSettlement.getPenalty() != null) {
                ptuSettlementDto.setNetSettlement(ptuSettlement.getPrice().subtract(ptuSettlement.getPenalty()));
            }
            ptuSettlementDto.setPowerDeficiency(ptuSettlement.getPowerDeficiency());
            ptuSettlementDto.setPenalty(ptuSettlement.getPenalty());
            ptuSettlementDto.setPrice(ptuSettlement.getPrice());
            ptuSettlementDto.setPrognosisPower(ptuSettlement.getPrognosisPower());
            result.add(ptuSettlementDto);
        }
        return result;
    }

    /**
     * Transforms a Flex Order Settlement entity to the format specified by the XSD specification.
     *
     * @param flexOrderSettlement a {@link FlexOrderSettlement} entity.
     * @return a {@link energy.usef.core.data.xml.bean.message.FlexOrderSettlement} object as specified by the XSD specification.
     */
    public static energy.usef.core.data.xml.bean.message.FlexOrderSettlement transformToXml(
            FlexOrderSettlement flexOrderSettlement) {
        energy.usef.core.data.xml.bean.message.FlexOrderSettlement flexOrderSettlementXml = new energy.usef.core.data.xml.bean
                .message.FlexOrderSettlement();
        flexOrderSettlementXml.setPeriod(flexOrderSettlement.getPeriod());
        flexOrderSettlementXml.setOrderReference(String.valueOf(flexOrderSettlement.getFlexOrder().getSequence()));
        if (flexOrderSettlement.getConnectionGroup() instanceof CongestionPointConnectionGroup) {
            flexOrderSettlementXml.setCongestionPoint(flexOrderSettlement.getConnectionGroup().getUsefIdentifier());
        }
        flexOrderSettlement.getPtuSettlements().stream()
                .map(SettlementTransformer::transformToXml)
                .forEach(ptuSettlementXml -> flexOrderSettlementXml.getPTUSettlement().add(ptuSettlementXml));
        return flexOrderSettlementXml;
    }

    /**
     * Transforms a PTU Settlement entity into the format specified by the XSD specification.
     *
     * @param ptuSettlement a {@link PtuSettlement} entity.
     * @return a {@link PTUSettlement} object as specified by the XSD specification.
     */
    public static PTUSettlement transformToXml(PtuSettlement ptuSettlement) {
        PTUSettlement ptuSettlementXml = new PTUSettlement();
        ptuSettlementXml.setActualPower(ptuSettlement.getActualPower());
        ptuSettlementXml.setDuration(BigInteger.valueOf(1L));
        ptuSettlementXml.setStart(BigInteger.valueOf(ptuSettlement.getPtuContainer().getPtuIndex()));
        ptuSettlementXml.setPrognosisPower(ptuSettlement.getPrognosisPower());
        ptuSettlementXml.setDeliveredFlexPower(ptuSettlement.getDeliveredFlexPower());
        ptuSettlementXml.setOrderedFlexPower(ptuSettlement.getOrderedFlexPower());
        ptuSettlementXml.setPowerDeficiency(ptuSettlement.getPowerDeficiency());
        ptuSettlementXml.setPenalty(ptuSettlement.getPenalty());
        ptuSettlementXml.setPrice(ptuSettlement.getPrice());
        ptuSettlementXml.setNetSettlement(ptuSettlement.getPrice().subtract(ptuSettlement.getPenalty()));
        return ptuSettlementXml;
    }

}
