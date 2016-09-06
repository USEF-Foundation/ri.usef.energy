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

package energy.usef.agr.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import energy.usef.agr.exception.AgrBusinessError;
import energy.usef.agr.service.business.AgrValidationBusinessService;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.data.xml.bean.message.DispositionAcceptedRejected;
import energy.usef.core.data.xml.bean.message.DispositionAvailableRequested;
import energy.usef.core.data.xml.bean.message.FlexRequest;
import energy.usef.core.data.xml.bean.message.FlexRequestResponse;
import energy.usef.core.data.xml.bean.message.MessageMetadata;
import energy.usef.core.data.xml.bean.message.PTU;
import energy.usef.core.data.xml.bean.message.USEFRole;
import energy.usef.core.exception.BusinessException;
import energy.usef.core.exception.BusinessValidationException;
import energy.usef.core.model.DocumentStatus;
import energy.usef.core.model.DocumentType;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.service.validation.CoreBusinessError;
import energy.usef.core.service.validation.CorePlanboardValidatorService;
import energy.usef.core.util.XMLUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Test class in charge of the unit tests of the {@link CommonReferenceUpdateResponseController}.
 */
@RunWith(PowerMockRunner.class)
public class FlexRequestControllerTest {
    private static final LocalDate DATE = new LocalDate("2014-11-28");
    private static final int PROGNOSIS_SEQUENCE = 1;
    private static final String PROGNOSIS_ORIGIN = "dso.usef-example.com";
    private static final String CONGESTION_POINT = "ean.123456789012345678";

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;
    @Mock
    private CorePlanboardValidatorService corePlanboardValidatorService;

    @Mock
    private AgrValidationBusinessService agrValidationBusinessService;

    @Mock
    private JMSHelperService jmsHelperService;

    private FlexRequestController flexRequestResponseController;

    @Mock
    private Config config;

    @Before
    public void init() {
        flexRequestResponseController = new FlexRequestController();
        Whitebox.setInternalState(flexRequestResponseController, corePlanboardValidatorService);
        Whitebox.setInternalState(flexRequestResponseController, agrValidationBusinessService);
        Whitebox.setInternalState(flexRequestResponseController, "jmsService", jmsHelperService);
        Whitebox.setInternalState(flexRequestResponseController, config);
        Whitebox.setInternalState(flexRequestResponseController, corePlanboardBusinessService);

        Mockito.when(config.getProperty(ConfigParam.PTU_DURATION)).thenReturn("15");
        Mockito.when(config.getProperty(ConfigParam.HOST_DOMAIN)).thenReturn("agr.usef-example.com");
    }

    @Test
    public void testFlexRequestIsOK() throws BusinessException {
        List<PTU> ptus = createPTUList(1, 96, 300, 0.045 / 1000);
        FlexRequest flexRequest = createFlexRequest(ptus, USEFRole.BRP);

        ArgumentCaptor<String> flexRequestResponseXml = ArgumentCaptor.forClass(String.class);

        flexRequestResponseController.action(flexRequest, null);

        Mockito.verify(corePlanboardBusinessService, Mockito.times(1)).updatePrognosisStatus(flexRequest.getPrognosisSequence(),
                flexRequest.getMessageMetadata().getSenderDomain(), DocumentType.A_PLAN, DocumentStatus.PENDING_FLEX_TRADING);

        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(flexRequestResponseXml.capture());
        FlexRequestResponse flexRequestResponse = XMLUtil
                .xmlToMessage(flexRequestResponseXml.getValue(), FlexRequestResponse.class);

        assertEquals(DispositionAcceptedRejected.ACCEPTED, flexRequestResponse.getResult());
        assertNull(flexRequestResponse.getMessage());
    }

    @Test
    public void testFlexRequestInvalidCongestionPoint() throws BusinessException {
        List<PTU> ptus = createPTUList(1, 96, 300, 0.045 / 1000);
        FlexRequest flexRequest = createFlexRequest(ptus, USEFRole.DSO);

        PowerMockito.doThrow(new BusinessValidationException(AgrBusinessError.NON_EXISTING_CONGESTION_POINT, CONGESTION_POINT)).
                when(agrValidationBusinessService).validateConnectionGroup(CONGESTION_POINT);

        ArgumentCaptor<String> flexRequestResponseXml = ArgumentCaptor.forClass(String.class);

        flexRequestResponseController.action(flexRequest, null);

        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(flexRequestResponseXml.capture());
        FlexRequestResponse flexRequestResponse = XMLUtil
                .xmlToMessage(flexRequestResponseXml.getValue(), FlexRequestResponse.class);

        assertEquals(DispositionAcceptedRejected.REJECTED, flexRequestResponse.getResult());
        assertEquals("Congestionpoint " + CONGESTION_POINT + " does not exist.", flexRequestResponse.getMessage());
    }

    @Test
    public void testFlexRequestInvalidPrognosis() throws BusinessException {
        List<PTU> ptus = createPTUList(1, 96, 300, 0.045 / 1000);
        FlexRequest flexRequest = createFlexRequest(ptus, USEFRole.DSO);

        PowerMockito
                .doThrow(
                        new BusinessValidationException(AgrBusinessError.NON_EXISTING_PROGNOSIS, PROGNOSIS_ORIGIN,
                                PROGNOSIS_SEQUENCE)).
                when(agrValidationBusinessService).validatePrognosis(PROGNOSIS_ORIGIN, PROGNOSIS_SEQUENCE);

        ArgumentCaptor<String> flexRequestResponseXml = ArgumentCaptor.forClass(String.class);

        flexRequestResponseController.action(flexRequest, null);

        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(flexRequestResponseXml.capture());
        FlexRequestResponse flexRequestResponse = XMLUtil
                .xmlToMessage(flexRequestResponseXml.getValue(), FlexRequestResponse.class);

        assertEquals(DispositionAcceptedRejected.REJECTED, flexRequestResponse.getResult());
        assertEquals("The prognosis can not be found with origin " + PROGNOSIS_ORIGIN + " and sequence " + PROGNOSIS_SEQUENCE +
                ".", flexRequestResponse.getMessage());
    }

    @Test
    public void testFlexRequestInvalidPTUs() throws BusinessException {
        List<PTU> ptus = createPTUList(1, 96, 300, 0.045 / 1000);
        FlexRequest flexRequest = createFlexRequest(ptus, USEFRole.DSO);

        PowerMockito.doThrow(new BusinessValidationException(CoreBusinessError.WRONG_NUMBER_OF_PTUS)).
                when(corePlanboardValidatorService)
                .validatePTUsForPeriod(Matchers.anyListOf(PTU.class), Matchers.any(LocalDate.class), Matchers.eq(true));

        ArgumentCaptor<String> flexRequestResponseXml = ArgumentCaptor.forClass(String.class);

        flexRequestResponseController.action(flexRequest, null);

        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(flexRequestResponseXml.capture());
        FlexRequestResponse flexRequestResponse = XMLUtil
                .xmlToMessage(flexRequestResponseXml.getValue(), FlexRequestResponse.class);

        assertEquals(DispositionAcceptedRejected.REJECTED, flexRequestResponse.getResult());
        assertEquals("The number of PTU's is {} instead of {}. The message will be rejected.",
                flexRequestResponse.getMessage());
    }

    private FlexRequest createFlexRequest(List<PTU> ptus, USEFRole recipientRole) {
        FlexRequest flexRequest = new FlexRequest();
        if (USEFRole.BRP != recipientRole) {
            flexRequest.setCongestionPoint(CONGESTION_POINT);
        }
        flexRequest.setPrognosisOrigin(PROGNOSIS_ORIGIN);
        flexRequest.setPrognosisSequence(PROGNOSIS_SEQUENCE);
        flexRequest.setPeriod(DATE);

        MessageMetadata messageMetadata = new MessageMetadata();
        messageMetadata.setMessageID("testId");
        messageMetadata.setSenderDomain("test.sender.domain");
        messageMetadata.setSenderRole(recipientRole);

        flexRequest.setMessageMetadata(messageMetadata);
        flexRequest.getPTU().addAll(ptus);
        return flexRequest;
    }

    private List<PTU> createPTUList(int start, int duration, int power, double price) {
        List<PTU> ptus = new ArrayList<>();
        ptus.add(createPTU(DispositionAvailableRequested.REQUESTED, start, duration, power, price));
        return ptus;
    }

    private PTU createPTU(DispositionAvailableRequested disposition, int start, int duration, int power, double price) {
        PTU ptu = new PTU();
        ptu.setDisposition(disposition);
        ptu.setDuration(BigInteger.valueOf(duration));
        ptu.setStart(BigInteger.valueOf(start));
        ptu.setPower(BigInteger.valueOf(power));
        ptu.setPrice(BigDecimal.valueOf(price));
        return ptu;
    }

}
