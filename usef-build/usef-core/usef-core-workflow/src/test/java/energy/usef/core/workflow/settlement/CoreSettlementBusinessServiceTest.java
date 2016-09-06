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

package energy.usef.core.workflow.settlement;

import static org.powermock.reflect.Whitebox.setInternalState;

import energy.usef.core.model.BrpConnectionGroup;
import energy.usef.core.model.Connection;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.ConnectionGroupState;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.repository.ConnectionGroupRepository;
import energy.usef.core.repository.ConnectionGroupStateRepository;
import energy.usef.core.repository.FlexOrderSettlementRepository;
import energy.usef.core.repository.PlanboardMessageRepository;
import energy.usef.core.repository.PtuContainerRepository;
import energy.usef.core.repository.PtuFlexOfferRepository;
import energy.usef.core.repository.PtuFlexOrderRepository;
import energy.usef.core.repository.PtuFlexRequestRepository;
import energy.usef.core.repository.PtuPrognosisRepository;
import energy.usef.core.repository.PtuSettlementRepository;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.workflow.dto.AcknowledgementStatusDto;
import energy.usef.core.workflow.dto.FlexOrderDto;
import energy.usef.core.workflow.dto.FlexOrderSettlementDto;
import energy.usef.core.workflow.dto.PtuFlexOrderDto;
import energy.usef.core.workflow.dto.PtuSettlementDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import junit.framework.TestCase;

/**
 *
 */
@RunWith(PowerMockRunner.class)
public class CoreSettlementBusinessServiceTest extends TestCase {

    private CoreSettlementBusinessService service;

    @Mock
    private PtuContainerRepository ptuContainerRepository;
    @Mock
    private ConnectionGroupStateRepository connectionGroupStateRepository;
    @Mock
    private PlanboardMessageRepository planboardMessageRepository;
    @Mock
    private PtuPrognosisRepository ptuPrognosisRepository;
    @Mock
    private PtuFlexRequestRepository ptuFlexRequestRepository;
    @Mock
    private PtuFlexOfferRepository ptuFlexOfferRepository;
    @Mock
    private PtuFlexOrderRepository ptuFlexOrderRepository;
    @Mock
    private FlexOrderSettlementRepository flexOrderSettlementRepository;
    @Mock
    private PtuSettlementRepository ptuSettlementRepository;
    @Mock
    private ConnectionGroupRepository connectionGroupRepository;
    @Mock
    private SequenceGeneratorService sequenceGeneratorService;

    @Before
    public void init() {
        service = new CoreSettlementBusinessService();
        setInternalState(service, "ptuContainerRepository", ptuContainerRepository);
        setInternalState(service, "connectionGroupStateRepository", connectionGroupStateRepository);
        setInternalState(service, "planboardMessageRepository", planboardMessageRepository);
        setInternalState(service, "ptuPrognosisRepository", ptuPrognosisRepository);
        setInternalState(service, "ptuFlexRequestRepository", ptuFlexRequestRepository);
        setInternalState(service, "ptuFlexOfferRepository", ptuFlexOfferRepository);
        setInternalState(service, "ptuFlexOrderRepository", ptuFlexOrderRepository);
        setInternalState(service, "flexOrderSettlementRepository", flexOrderSettlementRepository);
        setInternalState(service, "ptuSettlementRepository", ptuSettlementRepository);
        setInternalState(service, "connectionGroupRepository", connectionGroupRepository);
        setInternalState(service, "sequenceGeneratorService", sequenceGeneratorService);
    }

    @Test
    public void testFindActiveConnectionGroupsWithConnections() throws Exception {
        LocalDate startDate = new LocalDate();
        LocalDate endDate = new LocalDate();
        List<ConnectionGroupState> connectionGroupStateList = new ArrayList<ConnectionGroupState>();

        ConnectionGroup cg1 = createConnectionGroup("brp1.com", "ea1.2015-12.1:0");
        connectionGroupStateList.add(createConnectionGroupState(cg1, cg1.getUsefIdentifier().replace(":0", ":1"), startDate, endDate));
        connectionGroupStateList.add(createConnectionGroupState(cg1, cg1.getUsefIdentifier().replace(":0", ":3"), startDate, endDate));
        connectionGroupStateList.add(createConnectionGroupState(cg1, cg1.getUsefIdentifier().replace(":0", ":5"), startDate, endDate));
        connectionGroupStateList.add(createConnectionGroupState(cg1, cg1.getUsefIdentifier().replace(":0", ":7"), startDate, endDate));
        connectionGroupStateList.add(createConnectionGroupState(cg1, cg1.getUsefIdentifier().replace(":0", ":9"), startDate, endDate));

        ConnectionGroup cg2 = createConnectionGroup("brp2.com", "ea1.2015-12.2:0");
        connectionGroupStateList.add(createConnectionGroupState(cg2, cg2.getUsefIdentifier().replace(":0", ":2"), startDate, endDate));
        connectionGroupStateList.add(createConnectionGroupState(cg2, cg2.getUsefIdentifier().replace(":0", ":4"), startDate, endDate));
        connectionGroupStateList.add(createConnectionGroupState(cg2, cg2.getUsefIdentifier().replace(":0", ":6"), startDate, endDate));
        connectionGroupStateList.add(createConnectionGroupState(cg2, cg2.getUsefIdentifier().replace(":0", ":8"), startDate, endDate));

        ConnectionGroup cg3 = createConnectionGroup("brp3.com", "ea1.2015-12.3:0");
        connectionGroupStateList.add(createConnectionGroupState(cg3, cg3.getUsefIdentifier().replace(":0", ":1"), startDate, endDate));
        connectionGroupStateList.add(createConnectionGroupState(cg3, cg3.getUsefIdentifier().replace(":0", ":3"), startDate, endDate));
        connectionGroupStateList.add(createConnectionGroupState(cg3, cg3.getUsefIdentifier().replace(":0", ":5"), startDate, endDate));
        connectionGroupStateList.add(createConnectionGroupState(cg3, cg3.getUsefIdentifier().replace(":0", ":7"), startDate, endDate));
        connectionGroupStateList.add(createConnectionGroupState(cg3, cg3.getUsefIdentifier().replace(":0", ":9"), startDate, endDate));

        ConnectionGroup cg4 = createConnectionGroup("brp4.com", "ea1.2015-12.4:0");
        connectionGroupStateList.add(createConnectionGroupState(cg4, cg4.getUsefIdentifier().replace(":0", ":1"), startDate, endDate));
        connectionGroupStateList.add(createConnectionGroupState(cg4, cg4.getUsefIdentifier().replace(":0", ":2"), startDate, endDate));
        connectionGroupStateList.add(createConnectionGroupState(cg4, cg4.getUsefIdentifier().replace(":0", ":3"), startDate, endDate));
        connectionGroupStateList.add(createConnectionGroupState(cg4, cg4.getUsefIdentifier().replace(":0", ":4"), startDate, endDate));
        connectionGroupStateList.add(createConnectionGroupState(cg4, cg4.getUsefIdentifier().replace(":0", ":5"), startDate, endDate));
        connectionGroupStateList.add(createConnectionGroupState(cg4, cg4.getUsefIdentifier().replace(":0", ":6"), startDate, endDate));
        connectionGroupStateList.add(createConnectionGroupState(cg4, cg4.getUsefIdentifier().replace(":0", ":7"), startDate, endDate));
        connectionGroupStateList.add(createConnectionGroupState(cg4, cg4.getUsefIdentifier().replace(":0", ":8"), startDate, endDate));
        connectionGroupStateList.add(createConnectionGroupState(cg4, cg4.getUsefIdentifier().replace(":0", ":9"), startDate, endDate));

        Mockito.when(connectionGroupStateRepository.findConnectionGroupStatesWithOverlappingValidity(startDate, endDate)).thenReturn(connectionGroupStateList);

        Map<ConnectionGroup, Set<Connection>> map = service.findActiveConnectionGroupsWithConnections(startDate, endDate);

        assertEquals(4, map.size());
        assertEquals(5, map.get(cg1).size());
        assertEquals(4, map.get(cg2).size());
        assertEquals(5, map.get(cg3).size());
        assertEquals(9, map.get(cg4).size());
    }

    @Test
    public void testFindRelevantPrognoses() throws Exception {
        LocalDate startDate = new LocalDate();
        LocalDate endDate = new LocalDate();

        service.findRelevantPrognoses(startDate, endDate);
        Mockito.verify(ptuPrognosisRepository, Mockito.times(1)).findPrognosesForSettlement(startDate, endDate);
    }

    @Test
    public void testFindRelevantFlexRequests() throws Exception {
        LocalDate startDate = new LocalDate();
        LocalDate endDate = new LocalDate();

        service.findRelevantFlexRequests(startDate, endDate);
        Mockito.verify(ptuFlexRequestRepository, Mockito.times(1)).findFlexRequestsForSettlement(startDate, endDate);
    }

    @Test
    public void testFindRelevantFlexOffers() throws Exception {
        LocalDate startDate = new LocalDate();
        LocalDate endDate = new LocalDate();

        service.findRelevantFlexOffers(startDate, endDate);
        Mockito.verify(ptuFlexOfferRepository, Mockito.times(1)).findFlexOffersForSettlement(startDate, endDate);
    }

    @Test
    public void testFindRelevantFlexOrders() throws Exception {
        LocalDate startDate = new LocalDate();
        LocalDate endDate = new LocalDate();

        service.findRelevantFlexOrders(startDate, endDate);
        Mockito.verify(ptuFlexOrderRepository, Mockito.times(1)).findFlexOrdersForSettlement(startDate, endDate);
    }

    @Test
    public void testCreateFlexOrderSettlements() throws Exception {

        String domainName = "brp2.com";
        String entityAddress = "ea1.2015-12.2:0";
        LocalDate period = new LocalDate();
        Map<Integer, PtuContainer> ptuContainersMap = new HashMap<Integer, PtuContainer>();

        ptuContainersMap.put(1, createPtuContainer(period, 1));
        ptuContainersMap.put(2, createPtuContainer(period, 2));
        ptuContainersMap.put(3, createPtuContainer(period, 3));
        ptuContainersMap.put(4, createPtuContainer(period, 4));

        Mockito.when(ptuContainerRepository.findPtuContainersMap(period)).thenReturn(ptuContainersMap);


        //lexOrderSettlementDto.getFlexOrder().getConnectionGroupEntityAddress(),
         //       flexOrderSettlementDto.getFlexOrder().getSequenceNumber(),
           //     flexOrderSettlementDto.getFlexOrder().getParticipantDomain()));

        BrpConnectionGroup connectionGroup = new BrpConnectionGroup(entityAddress);
        connectionGroup.setBrpDomain(domainName);

        PlanboardMessage pbm = new PlanboardMessage();
        pbm.setPeriod(period);

        pbm.setConnectionGroup(connectionGroup);
        pbm.setParticipantDomain(domainName);
        pbm.setDocumentStatus(DocumentStatus.ACCEPTED);
        pbm.setDocumentType(DocumentType.FLEX_ORDER);
        pbm.setSequence(1L);

        Mockito.when(planboardMessageRepository.findSinglePlanboardMessage(entityAddress, 1L, domainName)).thenReturn(pbm);

        List<FlexOrderSettlementDto> dtoList = new ArrayList<FlexOrderSettlementDto>();
        dtoList.add(createFlexOrderSettlementDto(period, entityAddress, domainName, 1L));
        dtoList.add(createFlexOrderSettlementDto(period, entityAddress, domainName, 2L));
        dtoList.add(createFlexOrderSettlementDto(period, entityAddress, domainName, 3L));
        dtoList.add(createFlexOrderSettlementDto(period, entityAddress, domainName, 4L));
        dtoList.add(createFlexOrderSettlementDto(period, entityAddress, domainName, 5L));

        service.createFlexOrderSettlements(dtoList);

        Mockito.verify(flexOrderSettlementRepository, Mockito.times(5)).persist(Mockito.anyObject());
        Mockito.verify(ptuSettlementRepository, Mockito.times(20)).persist(Mockito.anyObject());
    }

    @Test
    public void testIsEachFlexOrderReadyForSettlement() throws Exception {
        Boolean result = service.isEachFlexOrderReadyForSettlement(2015, 11);
        Mockito.verify(flexOrderSettlementRepository, Mockito.times(1)).isEachFlexOrderReadyForSettlement(2015, 11);
    }

    @Test
    public void testFindFlexOrderSettlementsForPeriod() throws Exception {
        LocalDate startDate = new LocalDate();
        LocalDate endDate = new LocalDate();

        service.findFlexOrderSettlementsForPeriod(startDate, endDate, Optional.of("connection-group"), Optional.of("usef.energy"));
        Mockito.verify(flexOrderSettlementRepository, Mockito.times(1))
                .findFlexOrderSettlementsForPeriod(startDate, endDate, Optional.of("connection-group"), Optional.of("usef.energy"));
    }

    private ConnectionGroup createConnectionGroup (String domainName, String entityAddress) {
        LocalDate startDate = new LocalDate();
        LocalDate endDate = new LocalDate();
        BrpConnectionGroup connectionGroup = new BrpConnectionGroup(entityAddress);
        connectionGroup.setBrpDomain(domainName);
        for (int i = 0; i < 5; i++) {
            createConnectionGroupState(connectionGroup,entityAddress + i, startDate, endDate );
        }

        return connectionGroup;
    }
    private ConnectionGroupState createConnectionGroupState (ConnectionGroup connectionGroup, String entityAddress, LocalDate validFrom, LocalDate validUntil) {
        Connection connection = new Connection();
        connection.setEntityAddress(entityAddress);

        ConnectionGroupState connectionGroupState = new ConnectionGroupState();
        connectionGroupState.setConnectionGroup(connectionGroup);

        connectionGroupState.setConnection(connection);
        connectionGroupState.setValidFrom(validFrom);
        connectionGroupState.setValidUntil(validUntil);
        return connectionGroupState;
    }

    private FlexOrderSettlementDto createFlexOrderSettlementDto(LocalDate period, String entityAddress, String participantDomain, Long sequenceNumber) {
        FlexOrderSettlementDto dto = new FlexOrderSettlementDto(period);
        FlexOrderDto flexOrderDto = new FlexOrderDto();
        flexOrderDto.setPeriod(period);
        flexOrderDto.setAcknowledgementStatus(AcknowledgementStatusDto.ACCEPTED);
        flexOrderDto.setConnectionGroupEntityAddress(entityAddress);
        flexOrderDto.setFlexOfferSequenceNumber(sequenceNumber);
        flexOrderDto.setSequenceNumber(sequenceNumber);
        flexOrderDto.setParticipantDomain(participantDomain);



        flexOrderDto.getPtus().add(new PtuFlexOrderDto(BigInteger.valueOf(1), BigInteger.valueOf(10), BigDecimal.valueOf(100D)));
        flexOrderDto.getPtus().add(new PtuFlexOrderDto(BigInteger.valueOf(2), BigInteger.valueOf(20), BigDecimal.valueOf(200D)));
        flexOrderDto.getPtus().add(new PtuFlexOrderDto(BigInteger.valueOf(3), BigInteger.valueOf(30), BigDecimal.valueOf(300D)));
        flexOrderDto.getPtus().add(new PtuFlexOrderDto(BigInteger.valueOf(4), BigInteger.valueOf(40), BigDecimal.valueOf(400D)));

        PtuSettlementDto s = new PtuSettlementDto();

        dto.getPtuSettlementDtos().add(createPtuSettlementDto(BigInteger.valueOf(1)));
        dto.getPtuSettlementDtos().add(createPtuSettlementDto(BigInteger.valueOf(2)));
        dto.getPtuSettlementDtos().add(createPtuSettlementDto(BigInteger.valueOf(3)));
        dto.getPtuSettlementDtos().add(createPtuSettlementDto(BigInteger.valueOf(4)));

        dto.setFlexOrder(flexOrderDto);

        return dto;
    }

    private PtuSettlementDto createPtuSettlementDto (BigInteger ptuIndex) {
        PtuSettlementDto dto = new PtuSettlementDto();

        dto.setPtuIndex(ptuIndex);
        dto.setActualPower(BigInteger.valueOf(10));
        dto.setDeliveredFlexPower(BigInteger.valueOf(10));
        dto.setNetSettlement(BigDecimal.valueOf(100D));
        dto.setPenalty(BigDecimal.valueOf(100D));
        dto.setPowerDeficiency(BigInteger.valueOf(0));
        dto.setPrice(BigDecimal.valueOf(100D));
        dto.setPrognosisPower(BigInteger.valueOf(0));

        return dto;
    }


    private PtuContainer createPtuContainer(LocalDate ptuDate, Integer ptuIndex) {
        PtuContainer ptuContainer = new PtuContainer();
        ptuContainer.setPtuDate(ptuDate);
        ptuContainer.setPtuIndex(ptuIndex);

        return ptuContainer;
    }

}

