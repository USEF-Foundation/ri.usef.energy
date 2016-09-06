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

package energy.usef.core.factory;

import energy.usef.core.controller.IncomingMessageController;
import energy.usef.core.controller.error.OutgoingErrorMessageController;
import energy.usef.core.data.xml.bean.message.Message;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * JUnit test for the ControllerClassFactoryBuilder class.
 */
@RunWith(PowerMockRunner.class)
public class ControllerClassFactoryBuilderTest {

    /**
     * Test ControllerClassFactoryBuilder.getOutgoingErrorMessageControllerFactory method.
     */
    @Test
    public void getOutgoingErrorMessageControllerFactoryTest() {
        ControllerClassFactory<OutgoingErrorMessageController<Message>> factory = ControllerClassFactoryBuilder
                .getBuilder().getOutgoingErrorMessageControllerFactory();
        Assert.assertNotNull(factory);
    }

    /**
     * Test ControllerClassFactoryBuilder.getIncomingMessageControllerFactory method.
     */
    @Test
    public void getIncomingMessageControllerFactoryTest() {
        ControllerClassFactory<IncomingMessageController<Message>> factory = ControllerClassFactoryBuilder
                .getBuilder().getIncomingMessageControllerFactory();
        Assert.assertNotNull(factory);
    }

}
