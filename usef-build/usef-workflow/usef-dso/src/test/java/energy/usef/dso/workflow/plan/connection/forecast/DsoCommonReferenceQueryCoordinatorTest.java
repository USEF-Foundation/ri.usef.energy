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

package energy.usef.dso.workflow.plan.connection.forecast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.custommonkey.xmlunit.XMLUnit;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.xml.sax.SAXException;

import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.util.DateTimeUtil;
import energy.usef.dso.config.ConfigDso;
import energy.usef.dso.config.ConfigDsoParam;
import energy.usef.dso.model.CommonReferenceOperator;
import energy.usef.dso.service.business.DsoPlanboardBusinessService;

/**
 * Test class in charge of the unit tests related to the {@link DsoCommonReferenceQueryCoordinator}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ UUID.class, DateTimeUtil.class })
public class DsoCommonReferenceQueryCoordinatorTest {

    @Mock
    private Config config;

    @Mock
    private ConfigDso configDso;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private DsoPlanboardBusinessService dsoPlanboardBusinessService;

    @Mock
    private JMSHelperService jmsHelperService;

    private DsoCommonReferenceQueryCoordinator coordinator;

    @Before
    public void init() throws Exception {
        XMLUnit.setIgnoreWhitespace(true);
        coordinator = new DsoCommonReferenceQueryCoordinator();
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, configDso);
        Whitebox.setInternalState(coordinator, dsoPlanboardBusinessService);
        Whitebox.setInternalState(coordinator, jmsHelperService);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);

        final UUID uuidMock = PowerMockito.mock(UUID.class);
        PowerMockito.mockStatic(UUID.class);
        PowerMockito.when(UUID.randomUUID()).thenReturn(uuidMock);
        PowerMockito.when(uuidMock.toString()).thenReturn("12345678-1234-1234-1234-1234567890ab");

        // stubbing of DateTimeUtil to have a controller timestamp
        PowerMockito.mockStatic(DateTimeUtil.class);
        PowerMockito.when(DateTimeUtil.class, "getCurrentDate").thenReturn(new LocalDate(2014, 11, 10));
        // stubbing of the Config file
        PowerMockito.when(config.getProperty(ConfigParam.HOST_DOMAIN)).thenReturn("usef-example.com");
        PowerMockito.when(configDso.getIntegerProperty(ConfigDsoParam.DSO_INITIALIZE_PLANBOARD_DAYS_INTERVAL)).thenReturn(1);
    }

    @Test
    public void testMessageIsSentInQueue() throws SAXException, IOException {
        List<CommonReferenceOperator> croList = new ArrayList<>();
        croList.add(new CommonReferenceOperator());
        Mockito.when(dsoPlanboardBusinessService.findAllCommonReferenceOperators()).thenReturn(croList);
        coordinator.handleEvent(new CommonReferenceQueryEvent());
        ArgumentCaptor<String> xmlCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsHelperService, Mockito.times(1)).sendMessageToOutQueue(xmlCaptor.capture());

        String actualXml = xmlCaptor.getValue();
        Assert.assertNotNull("Did not expect null xml string.", actualXml);
    }
}
