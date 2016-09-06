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

package energy.usef.core.util;

import static org.junit.Assert.assertEquals;

import energy.usef.core.data.xml.bean.message.Prognosis;
import energy.usef.core.data.xml.bean.message.TestMessage;
import energy.usef.core.exception.TechnicalException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.junit.Assert;
import org.junit.Test;

/**
 * XML Util Test.
 */
public class XMLUtilTest {

    private static final String TEST_MESSAGE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><TestMessage><MessageMetadata SenderDomain=\"agr.usef-example.com\" SenderRole=\"AGR\" RecipientDomain=\"cro.usef-example.com\" RecipientRole=\"CRO\" TimeStamp=\"2015-02-05T14:08:33.687\" MessageID=\"fa1fd0b2-ec92-4635-b347-c1eabc4324bf\" ConversationID=\"fa1fd0b2-ec92-4635-b347-c1eabc4324bf\" Precedence=\"Routine\" ValidUntil=\"2015-02-05T14:08:33.687\"/></TestMessage>";
    private static final String FAILED_TEST_MESSAGE = "<TestMe!ssage><MessageMetadata MessageID=\"12345678-1234-1234-1234567890ab\"/></TestMessage>";
    private static final String PROGNOSIS_MESSAGE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Prognosis Type=\"D-Prognosis\" PTU-Duration=\"PT15M\" Period=\"2020-01-01\" TimeZone=\"Europe/Amsterdam\" CongestionPoint=\"ea1.1992-01.com.usef-example:gridpoint.11111111-1111-1111-1111\" Sequence=\"000000000001\"><MessageMetadata SenderDomain=\"agr.usef-example.com\" SenderRole=\"AGR\" RecipientDomain=\"dso.usef-example.com\" RecipientRole=\"DSO\" TimeStamp=\"2015-02-09T10:49:56.397\" MessageID=\"b78d0af7-2486-4d5f-a680-44d915b9c43c\" ConversationID=\"b78d0af7-2486-4d5f-a680-44d915b9c43c\" Precedence=\"Transactional\" ValidUntil=\"2015-02-09T10:49:56.397\" /><PTU Power=\"100\" Start=\"1\" Duration=\"96\"/></Prognosis>";
    private static final String PROGNOSIS_FAILED_MESSAGE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Prognosis Type=\"D-Prognosis\" PTU-Duration=\"PT15M\" Period=\"2020-01-01\" TimeZone=\"Europe/Amsterdam\" CongestionPoint=\"ea1.1992-01.com.usef-example:gridpoint.11111111-1111-1111-1111\" Sequence=\"000000000001\"><MessageMetadata SenderDomain=\"agr.usef-example.com\" SenderRole=\"AGR\" RecipientDomain=\"dso.usef-example.com\" RecipientRole=\"DSO\" TimeStamp=\"2015/02/09T10:49:56.397Z\" MessageID=\"b78d0af7-2486-4d5f-a680-44d915b9c43c\" ConversationID=\"b78d0af7-2486-4d5f-a680-44d915b9c43c\" Precedence=\"Transactional\" ValidUntil=\"2015/02/09T10:49:56.397Z\" /><PTUFAIL Power=\"100\" Start=\"1\" Duration=\"96\"/></Prognosis>";


    @Test
    public void testUtilityClass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.assertEquals("There must be only one constructor", 1, XMLUtil.class.getDeclaredConstructors().length);
        Constructor<XMLUtil> constructor = XMLUtil.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor must be private", Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        constructor.setAccessible(false);
    }

    @Test
    public void testPrognosisMessage() {
        Prognosis message = XMLUtil.xmlToMessage(PROGNOSIS_MESSAGE, Prognosis.class, true);
        assertEquals("b78d0af7-2486-4d5f-a680-44d915b9c43c", message.getMessageMetadata().getMessageID());
    }

    @Test(expected = TechnicalException.class)
    public void testPrognosisMessageFailed() {
        @SuppressWarnings("unused")
        Prognosis message = XMLUtil.xmlToMessage(PROGNOSIS_FAILED_MESSAGE, Prognosis.class, true);
    }

    @Test
    public void testXmlToMessage() {
        TestMessage message = (TestMessage) XMLUtil.xmlToMessage(TEST_MESSAGE);
        assertEquals("fa1fd0b2-ec92-4635-b347-c1eabc4324bf", message.getMessageMetadata().getMessageID());
    }

    @Test(expected = TechnicalException.class)
    public void testXmlToMessageFailed() {
        XMLUtil.xmlToMessage(FAILED_TEST_MESSAGE);
    }

    @Test
    public void testXmlToMessageWithClass() {
        TestMessage message = XMLUtil.xmlToMessage(TEST_MESSAGE, TestMessage.class);
        assertEquals("fa1fd0b2-ec92-4635-b347-c1eabc4324bf", message.getMessageMetadata().getMessageID());
    }

    @Test(expected = TechnicalException.class)
    public void testXmlToMessageWithClassFailed() {
        XMLUtil.xmlToMessage(FAILED_TEST_MESSAGE, TestMessage.class);
    }

    @Test
    public void testMessageObjectToXml() {
        TestMessage message = XMLUtil.xmlToMessage(TEST_MESSAGE, TestMessage.class);
        String xml = XMLUtil.messageObjectToXml(message);
        assertEquals(TEST_MESSAGE, xml);
    }

    @Test(expected = TechnicalException.class)
    public void testMessageToXmlFailed() {
        XMLUtil.messageObjectToXml("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMessageToXmlNull() {
        XMLUtil.messageObjectToXml(null);
    }
}
