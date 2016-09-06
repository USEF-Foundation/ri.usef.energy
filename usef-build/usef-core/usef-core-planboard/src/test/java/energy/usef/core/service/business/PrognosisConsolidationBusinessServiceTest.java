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

package energy.usef.core.service.business;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.Message;
import energy.usef.core.model.PlanboardMessage;
import energy.usef.core.model.PrognosisType;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuPrognosis;
import energy.usef.core.repository.ConnectionGroupRepository;
import energy.usef.core.repository.PlanboardMessageRepository;
import energy.usef.core.repository.PtuContainerRepository;
import energy.usef.core.repository.PtuPrognosisRepository;
import energy.usef.core.util.DateTimeUtil;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class PrognosisConsolidationBusinessServiceTest {

    @Mock
    PlanboardMessageRepository planboardMessageRepository;

    @Mock
    private PtuContainerRepository ptuContainerRepository;

    @Mock
    PtuPrognosisRepository ptuPrognosisRepository;

    @Mock
    private ConnectionGroupRepository connectionGroupRepository;

    @Mock
    Config config;

    PrognosisConsolidationBusinessService service;

    LocalDate localDate = DateTimeUtil.getCurrentDate();

    @Before
    public void init() {
        service = new PrognosisConsolidationBusinessService();
        Whitebox.setInternalState(service, "ptuContainerRepository", ptuContainerRepository);
        Whitebox.setInternalState(service, "planboardMessageRepository", planboardMessageRepository);
        Whitebox.setInternalState(service, "ptuPrognosisRepository", ptuPrognosisRepository);
        Whitebox.setInternalState(service, "connectionGroupRepository", connectionGroupRepository);
        Whitebox.setInternalState(service, "config", config);
        PowerMockito.when(ptuContainerRepository.findPtuContainersMap(Matchers.any(LocalDate.class)))
                .then(invocation -> IntStream.rangeClosed(1, 96)
                        .mapToObj(index -> new PtuContainer((LocalDate) invocation.getArguments()[0], index))
                        .collect(Collectors.toMap(PtuContainer::getPtuIndex, Function.identity())));
        PowerMockito.when(connectionGroupRepository.find(Matchers.anyString())).then(call -> {
            CongestionPointConnectionGroup connectionGroup = new CongestionPointConnectionGroup();
            connectionGroup.setUsefIdentifier((String) call.getArguments()[0]);
            return connectionGroup;
        });
    }

    @Test
    public void testConsolidatePrognosisForDate() throws Exception {
        // given
        String entityAddress = "ean.111-2222";
        String participantDomain = "agr.usef-example.com";
        Random random = new Random();
        PowerMockito.when(planboardMessageRepository.findPrognosisRelevantForDateByUsefIdentifier(localDate, entityAddress,
                participantDomain))
                .then(invocation -> IntStream.rangeClosed(1, 2).mapToObj(index -> {
                    PlanboardMessage pbMessage = new PlanboardMessage();
                    pbMessage.setSequence(random.nextLong());
                    Message message = new Message();
                    message.setCreationTime(DateTimeUtil.getCurrentDateTime().minusHours(index));
                    pbMessage.setMessage(message);
                    return pbMessage;
                }).collect(Collectors.toList()));
        PowerMockito.when(ptuPrognosisRepository.findBySequence(Matchers.anyLong()))
                .then(invocation -> IntStream.rangeClosed(1, 96).mapToObj(index -> {
                    PtuPrognosis ptuPrognosis = new PtuPrognosis();
                    PtuContainer ptuContainer = new PtuContainer(localDate.plusDays(1), index);
                    ptuPrognosis.setPtuContainer(ptuContainer);
                    ptuPrognosis.setConnectionGroup(PowerMockito.mock(ConnectionGroup.class));
                    ptuPrognosis.setType(PrognosisType.A_PLAN);
                    ptuPrognosis.setParticipantDomain(PowerMockito.mock(String.class));
                    ptuPrognosis.setSequence(PowerMockito.mock(Long.class));
                    ptuPrognosis.setPower(BigInteger.valueOf(random.nextInt(300)));
                    return ptuPrognosis;
                }).collect(Collectors.toList()));
        PowerMockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);
        PowerMockito.when(config.getIntegerProperty(ConfigParam.INTRADAY_GATE_CLOSURE_PTUS)).thenReturn(1);

        // when
        List<PtuPrognosis> ptuPrognosisList = service.consolidatePrognosisForDate(localDate, entityAddress, participantDomain);

        // then
        Mockito.verify(ptuPrognosisRepository, Mockito.times(1)).findBySequence(Matchers.anyLong());
        Assert.assertTrue(ptuPrognosisList.stream().allMatch(ptuPrognosis -> ptuPrognosis.getPower() != null));
        Assert.assertTrue(ptuPrognosisList.stream().allMatch(ptuPrognosis -> ptuPrognosis.getConnectionGroup() != null));
        Assert.assertTrue(ptuPrognosisList.stream().allMatch(ptuPrognosis -> ptuPrognosis.getType() != null));
        Assert.assertTrue(ptuPrognosisList.stream().allMatch(ptuPrognosis -> ptuPrognosis.getParticipantDomain() != null));
        Assert.assertTrue(ptuPrognosisList.stream().allMatch(ptuPrognosis -> ptuPrognosis.getSequence() != null));
    }

    @Test
    public void testConsolidatePrognosisForDateCompleteAfterTwoPlanboardMessages() {
        // given
        String entityAddress = "ean.111-2222";
        String participantDomain = "agr.usef-example.com";
        PowerMockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);
        PowerMockito.when(config.getIntegerProperty(ConfigParam.INTRADAY_GATE_CLOSURE_PTUS)).thenReturn(1);

        Random random = new Random();
        PowerMockito.when(planboardMessageRepository.findPrognosisRelevantForDateByUsefIdentifier(localDate, entityAddress,
                participantDomain))
                .then(invocation -> IntStream.rangeClosed(1, 5).mapToObj(index -> {
                    PlanboardMessage pbMessage = new PlanboardMessage();
                    pbMessage.setSequence(random.nextLong());
                    Message message = new Message();
                    // FIX-ME set fixed time
                    message.setCreationTime(DateTimeUtil.getCurrentDateTime().minusHours(index));
                    pbMessage.setMessage(message);
                    return pbMessage;
                }).collect(Collectors.toList()));

        PowerMockito.when(ptuPrognosisRepository.findBySequence(Matchers.anyLong())).thenReturn(
                // First call PTU 1 .. 48
                IntStream.rangeClosed(1, 48).mapToObj(index -> {
                    PtuPrognosis ptuPrognosis = new PtuPrognosis();
                    PtuContainer ptuContainer = new PtuContainer(localDate.plusDays(1), index);
                    ptuPrognosis.setPtuContainer(ptuContainer);
                    ptuPrognosis.setPower(BigInteger.valueOf(random.nextInt(300)));
                    return ptuPrognosis;
                }).collect(Collectors.toList())).thenReturn(
                // Second call PTU 48 .. 96
                IntStream.rangeClosed(49, 96).mapToObj(index -> {
                    PtuPrognosis ptuPrognosis = new PtuPrognosis();
                    PtuContainer ptuContainer = new PtuContainer(localDate.plusDays(1), index);
                    ptuPrognosis.setPtuContainer(ptuContainer);
                    ptuPrognosis.setPower(BigInteger.valueOf(random.nextInt(300)));
                    return ptuPrognosis;
                }).collect(Collectors.toList()));

        // when
        List<PtuPrognosis> ptuPrognosisList = service.consolidatePrognosisForDate(localDate, entityAddress, participantDomain);

        // then
        Mockito.verify(ptuPrognosisRepository, Mockito.times(2)).findBySequence(Matchers.anyLong());
        Assert.assertTrue(ptuPrognosisList.stream().allMatch(ptuPrognosis -> ptuPrognosis.getPower() != null));
    }

    @Test
    public void testConsolidatePrognosisWithPtuPrognosisPastGateClosure() {
        // given
        String entityAddress = "ean.111-2222";
        String participantDomain = "agr.usef-example.com";
        Random random = new Random();
        PowerMockito.when(planboardMessageRepository.findPrognosisRelevantForDateByUsefIdentifier(localDate, entityAddress,
                participantDomain))
                .then(invocation -> IntStream.rangeClosed(1, 1).mapToObj(index -> {
                    PlanboardMessage pbMessage = new PlanboardMessage();
                    pbMessage.setSequence(random.nextLong());
                    pbMessage.setCreationDateTime(new LocalDateTime(2015, 3, 30, 22, 0));
                    Message message = new Message();
                    message.setCreationTime(new LocalDateTime(2015, 3, 30, 22, 0));
                    pbMessage.setMessage(message);
                    return pbMessage;
                }).collect(Collectors.toList()));
        PowerMockito.when(ptuPrognosisRepository.findBySequence(Matchers.anyLong()))
                .then(invocation -> IntStream.rangeClosed(88, 96).mapToObj(index -> {
                    PtuPrognosis ptuPrognosis = new PtuPrognosis();
                    PtuContainer ptuContainer = new PtuContainer(localDate, index);
                    ptuPrognosis.setPtuContainer(ptuContainer);
                    ptuPrognosis.setPower(BigInteger.valueOf(random.nextInt(300)));
                    return ptuPrognosis;
                }).collect(Collectors.toList()));
        PowerMockito.when(config.getIntegerProperty(ConfigParam.PTU_DURATION)).thenReturn(15);
        PowerMockito.when(config.getIntegerProperty(ConfigParam.INTRADAY_GATE_CLOSURE_PTUS)).thenReturn(8);


        // when
        List<PtuPrognosis> ptuPrognosisList = service.consolidatePrognosisForDate(localDate, entityAddress, participantDomain);

        // then
        Assert.assertEquals(9, ptuPrognosisList.stream().filter(ptuPrognosis -> ptuPrognosis.getPower() != null).count());
    }
}
