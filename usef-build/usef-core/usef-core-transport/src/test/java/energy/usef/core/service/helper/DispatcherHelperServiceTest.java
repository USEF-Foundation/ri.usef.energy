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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.reflect.Whitebox.setInternalState;
import energy.usef.core.controller.TestMessageController;
import energy.usef.core.data.xml.bean.message.TestMessage;

import javax.enterprise.inject.spi.BeanManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * JUnit test for the DispatcherService class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ DispatcherHelperService.class })
public class DispatcherHelperServiceTest {

    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            +
            "<TestMessage>"
            +
            "<MessageMetadata SenderDomain=\"localhost\" Body=\"test body\" SenderRole=\"AGR\" RecipientDomain=\"localhost\" RecipientRole=\"DSO\" MessageID=\"00000000-1111-2222-333333333333\" Precedence=\"Transactional\"/>"
            +
            "</TestMessage>";

    @Mock
    private TestMessageController testMessageController;

    @Mock
    private BeanManager beanManager;

    private DispatcherHelperService service;

    @Before
    public void init() throws Exception {
        service = new DispatcherHelperService();
        setInternalState(service, "beanManager", beanManager);
    }

    /**
     * Tests DispatcherService.dispatch method.
     *
     * @throws Exception
     */
    @Test
    public void dispatchTest() throws Exception {
        DispatcherHelperService dispatcherHelperService = spy(service);
        doReturn(testMessageController).when(dispatcherHelperService, "getController", TestMessageController.class);
        dispatcherHelperService.dispatch(XML);
        verify(testMessageController, times(1)).execute(anyString(), any(TestMessage.class));
    }
}
