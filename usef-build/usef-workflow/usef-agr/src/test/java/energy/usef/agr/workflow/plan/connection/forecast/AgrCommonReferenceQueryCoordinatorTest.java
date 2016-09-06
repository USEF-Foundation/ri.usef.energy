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

package energy.usef.agr.workflow.plan.connection.forecast;

import energy.usef.agr.config.ConfigAgr;
import energy.usef.agr.config.ConfigAgrParam;
import energy.usef.agr.model.CommonReferenceOperator;
import energy.usef.agr.service.business.AgrPlanboardBusinessService;
import energy.usef.core.config.Config;
import energy.usef.core.config.ConfigParam;
import energy.usef.core.service.business.CorePlanboardBusinessService;
import energy.usef.core.service.helper.JMSHelperService;
import energy.usef.core.util.DateTimeUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.custommonkey.xmlunit.XMLUnit;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.xml.sax.SAXException;

/**
 * Test class in charge of the unit tests related to the {@link AgrCommonReferenceQueryCoordinator}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ UUID.class, DateTimeUtil.class })
public class AgrCommonReferenceQueryCoordinatorTest {

    @Mock
    private Config config;

    @Mock
    private JMSHelperService jmsHelperService;

    @Mock
    private ConfigAgr configAgr;

    @Mock
    private CorePlanboardBusinessService corePlanboardBusinessService;

    @Mock
    private AgrPlanboardBusinessService agrPlanboardBusinessService;

    private AgrCommonReferenceQueryCoordinator coordinator;

    @Before
    public void init() throws Exception {
        XMLUnit.setIgnoreWhitespace(true);
        coordinator = new AgrCommonReferenceQueryCoordinator();
        Whitebox.setInternalState(coordinator, config);
        Whitebox.setInternalState(coordinator, agrPlanboardBusinessService);
        Whitebox.setInternalState(coordinator, corePlanboardBusinessService);
        Whitebox.setInternalState(coordinator, configAgr);
        Whitebox.setInternalState(coordinator, jmsHelperService);

        final UUID uuidMock = PowerMockito.mock(UUID.class);
        PowerMockito.mockStatic(UUID.class);
        PowerMockito.when(UUID.randomUUID()).thenReturn(uuidMock);
        PowerMockito.when(uuidMock.toString()).thenReturn("12345678-1234-1234-1234-1234567890ab");

        // stubbing of CalendarUtil to have a controller timestamp
        PowerMockito.mockStatic(DateTimeUtil.class);
        PowerMockito.when(DateTimeUtil.class, "getCurrentDateTime").thenReturn(new LocalDateTime(2014, 11, 10, 11, 0));
        PowerMockito.when(DateTimeUtil.class, "getCurrentDate").thenReturn(new LocalDate(2014, 11, 10));
        // stubbing of the Config file
        PowerMockito.when(config.getProperty(ConfigParam.HOST_DOMAIN)).thenReturn("usef-example.com");
        // stubbing
        PowerMockito.when(agrPlanboardBusinessService.findAll()).thenReturn(buildCommonReferenceDomains());

        PowerMockito.when(configAgr.getIntegerProperty(ConfigAgrParam.AGR_INITIALIZE_PLANBOARD_DAYS_INTERVAL)).thenReturn(2);

    }

    @Test
    public void testMessageIsSentInQueue() throws SAXException, IOException {
        coordinator.handleEvent(new CommonReferenceQueryEvent());
        ArgumentCaptor<String> xmlCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsHelperService, Mockito.times(2)).sendMessageToOutQueue(xmlCaptor.capture());

        String actualXml = xmlCaptor.getValue();
        Assert.assertNotNull("Did not expect null xml string.", actualXml);
        Assert.assertTrue(actualXml.contains("CommonReferenceQuery"));

        Mockito.verify(corePlanboardBusinessService, Mockito.times(2))
                .findOrCreatePtuContainersForPeriod(Matchers.any(LocalDate.class));
    }

    /**
     * Builds a controlled {@link CommonReferenceOperator} entity.
     *
     * @return a {@link CommonReferenceOperator}.
     */
    private List<CommonReferenceOperator> buildCommonReferenceDomains() {
        List<CommonReferenceOperator> commonReferenceDomains = new ArrayList<>();
        CommonReferenceOperator domain = new CommonReferenceOperator();
        domain.setId(1l);
        domain.setDomain("usef-example.com");
        commonReferenceDomains.add(domain);
        return commonReferenceDomains;
    }
}
