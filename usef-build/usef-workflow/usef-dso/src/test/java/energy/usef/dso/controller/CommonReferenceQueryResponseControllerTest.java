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

package energy.usef.dso.controller;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.IOUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import energy.usef.core.data.xml.bean.message.CommonReferenceEntityType;
import energy.usef.core.data.xml.bean.message.CommonReferenceQueryResponse;
import energy.usef.core.data.xml.bean.message.CongestionPoint;
import energy.usef.core.data.xml.bean.message.DispositionSuccessFailure;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.model.CongestionPointConnectionGroup;
import energy.usef.core.model.ConnectionGroup;
import energy.usef.core.model.ConnectionGroupState;
import energy.usef.core.model.Message;
import energy.usef.core.model.PtuContainer;
import energy.usef.core.model.PtuState;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.business.MessageService;
import energy.usef.core.service.business.SequenceGeneratorService;
import energy.usef.core.service.helper.MessageMetadataBuilder;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.config.ConfigDsoParam;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;

/**
 * Text class for CommonReferenceQueryController.
 */
@RunWith(PowerMockRunner.class)
public class CommonReferenceQueryResponseControllerTest {

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Mock
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;
    @Mock
    private ConfigDso configDso;
    @Mock
    private MessageService messageService;

    private CommonReferenceQueryResponseController controller;

    @Before
    public void init() {
        controller = new CommonReferenceQueryResponseController();
        SequenceGeneratorService sequenceGeneratorService = new SequenceGeneratorService();

        Whitebox.setInternalState(controller, corePlanboardBusinessService);
        Whitebox.setInternalState(controller, dsoPlanboardBusinessService);
        Whitebox.setInternalState(controller, messageService);
        Whitebox.setInternalState(controller, sequenceGeneratorService);
        Whitebox.setInternalState(controller, configDso);
        PowerMockito.when(configDso.getIntegerProperty(ConfigDsoParam.DSO_INITIALIZE_PLANBOARD_DAYS_INTERVAL)).thenReturn(2);
        PowerMockito.when(messageService.getInitialMessageOfConversation(Matchers.any(String.class))).then(invocation -> {
            Message message = new Message();
            message.setCreationTime(new LocalDateTime(2015, 1, 1, 1, 0, 0));
            message.setXml(fetchFileContent("energy/usef/dso/controller/common_reference_query.xml"));
            message.setConversationId((String) invocation.getArguments()[0]);
            return message;
        });
    }

    /**
     * Tests CommonReferenceQueryController.action method with a success response.
     *
     * @throws BusinessException
     */
    @Test
    public void testSuccessResponseAction() throws BusinessException {
        CommonReferenceQueryResponse message = new CommonReferenceQueryResponse();
        message.setResult(DispositionSuccessFailure.SUCCESS);
        message.setMessageMetadata(new MessageMetadataBuilder().conversationID().build());
        message.getCongestionPoint().add(new CongestionPoint());
        ConnectionGroupState connectionGroupState = new ConnectionGroupState();
        ConnectionGroup connectionGroup = new CongestionPointConnectionGroup();
        connectionGroup.setUsefIdentifier("usef.example.com");
        connectionGroupState.setConnectionGroup(connectionGroup);

        PowerMockito.when(corePlanboardBusinessService.findActiveConnectionGroupStates(Matchers.any(LocalDate.class),
                Matchers.eq(CongestionPointConnectionGroup.class)))
                .then(invocation -> Arrays.asList(connectionGroupState, connectionGroupState, connectionGroupState));

        PowerMockito.when(corePlanboardBusinessService.findOrCreatePtuContainersForPeriod(Mockito.any(LocalDate.class)))
                .thenReturn(buildPtuContainers(2));
        PowerMockito.when(corePlanboardBusinessService
                .findActiveConnectionGroupStates(Mockito.any(LocalDate.class), Mockito.eq(CongestionPointConnectionGroup.class)))
                .thenReturn(Arrays.<ConnectionGroupState>asList(connectionGroupState));

        controller.action(message, null);
        Mockito.verify(corePlanboardBusinessService, Mockito.times(1))

                .storeCommonReferenceQueryResponse(Matchers.eq(message), Matchers.eq(CommonReferenceEntityType.CONGESTION_POINT),
                        Matchers.eq(new LocalDate(2015, 1, 2)), Matchers.eq(2));
        Mockito.verify(dsoPlanboardBusinessService, Mockito.times(1))
                .updateAggregatorsOnCongestionPointConnectionGroup(Mockito.any(CongestionPoint.class), Mockito.any(LocalDate.class),
                        Mockito.eq(2));


        Mockito.verify(corePlanboardBusinessService, Mockito.times(4)).storePtuState(Mockito.any(PtuState.class));
    }

    private List<PtuContainer> buildPtuContainers(int i) {
        return IntStream.rangeClosed(1, i).mapToObj(index -> new PtuContainer(new LocalDate(), index)).collect(Collectors.toList());
    }

    /**
     * Tests CommonReferenceQueryController.action method with a failed response.
     *
     * @throws BusinessException
     */
    @Test
    public void testFailedResponseAction() throws BusinessException {
        CommonReferenceQueryResponse message = new CommonReferenceQueryResponse();
        message.setResult(DispositionSuccessFailure.FAILURE);
        message.setMessageMetadata(new MessageMetadataBuilder().conversationID().build());
        controller.action(message, null);
        Mockito.verify(corePlanboardBusinessService, Mockito.times(0))
                .storeCommonReferenceQueryResponse(Matchers.eq(message), Matchers.eq(CommonReferenceEntityType.CONGESTION_POINT),
                        Matchers.eq(new LocalDate(2015, 1, 2)), Matchers.eq(2));
        Mockito.verify(dsoPlanboardBusinessService, Mockito.times(0))
                .updateAggregatorsOnCongestionPointConnectionGroup(Mockito.any(CongestionPoint.class),
                        Matchers.any(LocalDate.class), Matchers.eq(1));

        Mockito.verify(corePlanboardBusinessService, Mockito.times(0)).storePtuState(Mockito.any(PtuState.class));
    }

    private String fetchFileContent(String filePath) throws IOException {
        StringWriter xmlWriter = new StringWriter();
        IOUtils.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath), xmlWriter);
        return xmlWriter.toString();
    }

}
