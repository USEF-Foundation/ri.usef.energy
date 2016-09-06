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

package energy.usef.core.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import energy.usef.core.data.xml.bean.message.TestMessageResponse;
import energy.usef.core.util.ReflectionUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

/**
 * JUnit test for the TestMessageResponseController class.
 */
@RunWith(PowerMockRunner.class)
public class TestMessageResponseControllerTest {
    private static final String TEST_MESSAGE_RESPONSE_RECEIVED = "Test message response received";

    @Mock
    private Logger LOGGER;

    private TestMessageResponseController controller;

    @Before
    public void init() throws Exception {
        controller = new TestMessageResponseController();
        ReflectionUtil.setFinalStatic(TestMessageResponseController.class.getDeclaredField("LOGGER"), LOGGER);
    }

    /**
     * Tests TestMessageResponseController.action method.
     */
    @Test
    public void actionTest() {
        controller.action(new TestMessageResponse(), null);
        verify(LOGGER, times(1)).info(Matchers.eq(TEST_MESSAGE_RESPONSE_RECEIVED));
    }

}
