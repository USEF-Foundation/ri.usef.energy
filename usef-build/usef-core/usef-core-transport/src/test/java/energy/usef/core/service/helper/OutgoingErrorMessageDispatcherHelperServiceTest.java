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

package energy.usef.core.service.helper;

import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import energy.usef.core.controller.error.BaseOutgoingErrorMessageController;
import energy.usef.core.data.xml.bean.message.CommonReferenceQuery;
import energy.usef.core.util.CDIUtil;
import energy.usef.core.util.ReflectionUtil;
import energy.usef.core.util.XMLUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

/**
 * JUnit test for the OutgoingErrorMessageDispatcherHelperService class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ CDIUtil.class })
public class OutgoingErrorMessageDispatcherHelperServiceTest {

    private OutgoingErrorMessageDispatcherHelperService service;

    @Mock
    private Logger LOGGER;

    @Mock
    private BaseOutgoingErrorMessageController<?> controller;

    @Before
    public void init() throws Exception {
        service = new OutgoingErrorMessageDispatcherHelperService();
        ReflectionUtil.setFinalStatic(OutgoingErrorMessageDispatcherHelperService.class.getDeclaredField("LOGGER"), LOGGER);
    }

    /**
     * Tests DispatcherService.dispatch method.
     *
     * @throws Exception
     */
    @Test
    public void dispatchWithErrorTest() throws Exception {
        CommonReferenceQuery message = new CommonReferenceQuery();
        String xml = XMLUtil.messageObjectToXml(message);

        service.dispatch(xml);

        verify(LOGGER, times(1)).debug(contains("Can not find the corresponding message error controller for the class"),
                Matchers.any(Class.class));
    }
}
