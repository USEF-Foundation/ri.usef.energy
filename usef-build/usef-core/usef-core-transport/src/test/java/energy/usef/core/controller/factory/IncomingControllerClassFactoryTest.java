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

package energy.usef.core.controller.factory;

import static org.junit.Assert.assertEquals;
import energy.usef.core.controller.TestMessageController;
import energy.usef.core.data.xml.bean.message.TestMessage;
import energy.usef.core.util.XMLUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * JUnit test for the IncomingControllerClassFactory class.
 */
@RunWith(PowerMockRunner.class)
public class IncomingControllerClassFactoryTest {

    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            +
            "<TestMessage>"
            +
            "<MessageMetadata SenderDomain=\"testSenderDomain\" Body=\"test body\" SenderRole=\"aggregator\" RecipientDomain=\"localhost\" RecipientRole=\"dso\" MessageID=\"testMessageId\" Class=\"transactional\"/>"
            +
            "</TestMessage>";

    /**
     * Tests IncomingControllerClassFactory.getControllerClass method.
     */
    @Test
    public void getControllerClassTest() {
        TestMessage xmlObject = (TestMessage) XMLUtil.xmlToMessage(XML);
        assertEquals(IncomingControllerClassFactory.getControllerClass(xmlObject.getClass()), TestMessageController.class);
    }

}
