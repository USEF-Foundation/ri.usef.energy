/*
 * Copyright (c) 2014-2016 BePowered BVBA http://www.bepowered.be/
 *
 * Software is subject to the following conditions:
 *
 * The above copyright notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.energieprojecthoogdalem.messageservice.transportservice;

import nl.energieprojecthoogdalem.messageservice.transportservice.mqttmessages.MqttResponseMessage;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import java.io.FileReader;
import java.security.SecureRandom;
import java.util.Properties;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DateTime.class, SSLContext.class, MqttConnection.class})

public class MqttConnectionTest
{
    private MqttConnection mqttConnection;

    private static String CLIENTID = "TEST-CLIENT"
                        , HOST = "127.0.1.1"
                        , TCP = "tcp"
                        , SSL = "ssl"
                        , TCPPORT = "1884"
                        , SSLPORT = "8883"
                        , TOPIC = "/test/topic"
                        , MESSAGE = "MESSAGE"
                        , WILLTOPIC = "/mock/test"
                        , WILLMESSAGE = "WILLMESSAGE"
                        ;

    private Properties properties;

    @Mock
    private DateTime dateTime;

    @Mock
    private MqttClient mqttClient;

    @Mock
    private MqttConnectOptions mqttConnectOptions;

    @Mock
    private MemoryPersistence memoryPersistence;

    @Mock
    private MessageCallback messageCallback;

    @Mock
    private MqttMessage mqttMessage;

    @Mock
    private MqttException mqttException;

    @Mock
    private MqttSecurityException mqttSecurityException;

    @Mock
    private SSLContext sslContext;

    @Mock
    private SSLSocketFactory SocketFactory;

    @Mock
    private MqttResponseMessage mqttResponseMessage;

    @Before
    public void setup() throws Exception
    {
        PowerMockito.mockStatic(SSLContext.class);

        PowerMockito.when(SSLContext.getInstance(Matchers.anyString())).thenReturn(sslContext);
        PowerMockito.doNothing().when(sslContext).init(Matchers.any(KeyManager[].class), Matchers.any(TrustManager[].class), Matchers.any(SecureRandom.class) );
        PowerMockito.when(sslContext.getSocketFactory()).thenReturn(SocketFactory);

        properties = PowerMockito.spy(new Properties());
        PowerMockito.whenNew(Properties.class).withNoArguments().thenReturn(properties);
        PowerMockito.doNothing().when(properties).load(Matchers.any(FileReader.class));
        setGeneralProperties();

        PowerMockito.whenNew(MemoryPersistence.class).withAnyArguments().thenReturn(memoryPersistence);
        PowerMockito.whenNew(MqttMessage.class).withAnyArguments().thenReturn(mqttMessage);

        PowerMockito.whenNew(DateTime.class).withAnyArguments().thenReturn(dateTime);
        PowerMockito.doReturn(1L).when(dateTime).getMillis();

        PowerMockito.whenNew(MqttConnectOptions.class).withNoArguments().thenReturn(mqttConnectOptions);
        PowerMockito.doNothing().when(mqttConnectOptions).setSocketFactory(Matchers.any(SSLSocketFactory.class));

        PowerMockito.whenNew(MqttClient.class).withArguments(Matchers.anyString(), Matchers.anyString(), Matchers.any(MemoryPersistence.class)).thenReturn(mqttClient);
        PowerMockito.doNothing().when(mqttClient).setCallback(Matchers.any(MessageCallback.class));
        PowerMockito.when(mqttClient.isConnected()).thenReturn(true);

        PowerMockito.doNothing().when(mqttClient).unsubscribe(Matchers.anyString());
        PowerMockito.doNothing().when(mqttClient).subscribe(Matchers.anyString(), Matchers.anyInt());
        PowerMockito.doNothing().when(mqttClient).connect(Matchers.any(MqttConnectOptions.class));

        mqttConnection = new MqttConnection();
        Whitebox.setInternalState(mqttConnection, "messageCallback", messageCallback);
        Whitebox.setInternalState(mqttConnection, "mqttConnection", mqttConnection);

    }


    @Test
    public void testTCPConnect() throws Exception
    {
        setTCPProperties();
        mqttConnection.init();

        //verify correct fields has been set
        Mockito.verify(properties, Mockito.times(1)).load(Matchers.any(FileReader.class));
        PowerMockito.verifyNew(MqttClient.class, Mockito.times(1)).withArguments(TCP+ "://" + HOST + ':' + TCPPORT, CLIENTID + 1L, memoryPersistence);
        Mockito.verify(mqttClient, Mockito.times(1)).setCallback(messageCallback);
        Mockito.verify(mqttClient, Mockito.times(1)).connect(mqttConnectOptions);

        Mockito.verify(sslContext, Mockito.never()).init(Matchers.any(KeyManager[].class), Matchers.any(TrustManager[].class), Matchers.any(SecureRandom.class) );
        Mockito.verify(mqttConnectOptions, Mockito.never()).setUserName("username");
        Mockito.verify(mqttConnectOptions, Mockito.never()).setPassword("password".toCharArray());
        Mockito.verify(mqttConnectOptions, Mockito.times(1)).setWill(WILLTOPIC, WILLMESSAGE.getBytes(), 0, false);

        Mockito.verify(mqttConnectOptions, Mockito.times(1)).setCleanSession(true);


        assertEquals(true, mqttConnection.isConnected());


        mqttConnection.subscribe(TOPIC, 2);
        Mockito.verify(mqttClient, Mockito.times(1)).subscribe(TOPIC, 2);

        mqttConnection.publish(TOPIC, MESSAGE, 2, true);
        mqttConnection.publish(TOPIC, MESSAGE, 1);
        Mockito.verify(mqttMessage, Mockito.times(1)).setRetained(false);
        Mockito.verify(mqttMessage, Mockito.times(1)).setRetained(true);
        Mockito.verify(mqttMessage, Mockito.times(1)).setQos(1);
        Mockito.verify(mqttMessage, Mockito.times(1)).setQos(2);
        Mockito.verify(mqttClient, Mockito.times(2)).publish(TOPIC, mqttMessage);


        mqttConnection.unsubscribe(TOPIC);
        Mockito.verify(mqttClient, Mockito.times(1)).unsubscribe(TOPIC);

        mqttConnection.disconnect();
        Mockito.verify(mqttClient, Mockito.times(1)).disconnect();
        Mockito.verify(mqttClient, Mockito.times(1)).close();
    }

    @Test
    public void testSSLConnect() throws Exception
    {
        setSSLProperties();
        mqttConnection.init();

        //verify correct fields has been set
        Mockito.verify(properties, Mockito.times(1)).load(Matchers.any(FileReader.class));
        PowerMockito.verifyNew(MqttClient.class, Mockito.times(1)).withArguments(SSL+ "://" + HOST + ':' + SSLPORT, CLIENTID + 1L, memoryPersistence);
        Mockito.verify(mqttClient, Mockito.times(1)).setCallback(messageCallback);
        Mockito.verify(mqttClient, Mockito.times(1)).connect(mqttConnectOptions);

        Mockito.verify(sslContext, Mockito.times(1)).init(Matchers.any(KeyManager[].class), Matchers.any(TrustManager[].class), Matchers.any(SecureRandom.class) );
        Mockito.verify(mqttConnectOptions, Mockito.times(1)).setUserName("username");
        Mockito.verify(mqttConnectOptions, Mockito.times(1)).setPassword("password".toCharArray());

        Mockito.verify(mqttConnectOptions, Mockito.never()).setWill(Matchers.anyString(), Matchers.any(byte[].class), Matchers.anyInt(), Matchers.anyBoolean());

        Mockito.verify(mqttConnectOptions, Mockito.times(1)).setCleanSession(true);


        assertEquals(true, mqttConnection.isConnected());


        mqttConnection.subscribe(TOPIC);
        Mockito.verify(mqttClient, Mockito.times(1)).subscribe(TOPIC, 0);

        mqttConnection.publish(TOPIC, MESSAGE);
        mqttConnection.publish(TOPIC, MESSAGE, false);
        Mockito.verify(mqttMessage, Mockito.times(2)).setRetained(false);
        Mockito.verify(mqttMessage, Mockito.times(2)).setQos(0);
        Mockito.verify(mqttClient, Mockito.times(2)).publish(TOPIC, mqttMessage);


        mqttConnection.unsubscribe(TOPIC);
        Mockito.verify(mqttClient, Mockito.times(1)).unsubscribe(TOPIC);

        mqttConnection.disconnect();
        Mockito.verify(mqttClient, Mockito.times(1)).disconnect();
        Mockito.verify(mqttClient, Mockito.times(1)).close();
    }

    @Test
    public void testNoConnection() throws Exception
    {
        PowerMockito.when(mqttClient.isConnected()).thenReturn(false);
        PowerMockito.doThrow(mqttException).when(mqttClient).disconnect();
        PowerMockito.doThrow(mqttException).when(mqttClient).publish(Matchers.anyString(), Matchers.any(MqttMessage.class));
        PowerMockito.doThrow(mqttException).when(mqttClient).subscribe(Matchers.anyString(), Matchers.anyInt());
        PowerMockito.doThrow(mqttException).when(mqttClient).unsubscribe(Matchers.anyString());

        PowerMockito.doThrow(mqttSecurityException).when(mqttClient).connect(Matchers.any(MqttConnectOptions.class));
        mqttConnection.init();

        Mockito.verify(properties, Mockito.times(1)).load(Matchers.any(FileReader.class));
        PowerMockito.verifyNew(MqttClient.class, Mockito.times(1)).withArguments("tcp://"+HOST+":1883", CLIENTID + 1L, memoryPersistence);
        Mockito.verify(mqttClient, Mockito.times(1)).setCallback(messageCallback);
        Mockito.verify(mqttClient, Mockito.times(1)).connect(mqttConnectOptions);

        assertEquals(false, mqttConnection.isConnected());


        PowerMockito.doThrow(mqttException).when(mqttClient).connect(Matchers.any(MqttConnectOptions.class));
        assertEquals(false, mqttConnection.connect().get());


        mqttConnection.subscribe(TOPIC);
        Mockito.verify(mqttClient, Mockito.times(1)).subscribe(TOPIC, 0);

        mqttConnection.publish(TOPIC, MESSAGE);
        Mockito.verify(mqttMessage, Mockito.times(1)).setRetained(false);
        Mockito.verify(mqttMessage, Mockito.times(1)).setQos(0);
        Mockito.verify(mqttClient, Mockito.times(1)).publish(TOPIC, mqttMessage);

        mqttConnection.unsubscribe(TOPIC);
        Mockito.verify(mqttClient, Mockito.times(1)).unsubscribe(TOPIC);

        mqttConnection.disconnect();
        Mockito.verify(mqttClient, Mockito.times(1)).disconnect();
        Mockito.verify(mqttClient, Mockito.never()).close();
    }

    @Test
    public void testPublishAndReceive() throws Exception
    {
        setTCPProperties();
        mqttConnection.init();

        mqttConnection.publishAndReceive(TOPIC, MESSAGE, mqttResponseMessage);

        Mockito.verify(messageCallback, Mockito.times(1)).addReceiveMessage(mqttResponseMessage);
        Mockito.verify(mqttMessage, Mockito.times(1) ).setQos(0);
        Mockito.verify(mqttMessage, Mockito.times(1) ).setRetained(false);
        Mockito.verify(mqttClient, Mockito.times(1) ).publish(TOPIC, mqttMessage);
    }


    private void setGeneralProperties()
    {
        properties.setProperty("clientID",CLIENTID);
        properties.setProperty("host", HOST);


        properties.setProperty("cleanSession", "true");
    }

    private void setTCPProperties()
    {
        properties.setProperty("protocol", TCP);
        properties.setProperty("port", TCPPORT);

        properties.setProperty("willTopic",WILLTOPIC);
        properties.setProperty("willMessage",WILLMESSAGE);
        properties.setProperty("willQOS","0");
        properties.setProperty("willRetain","false");
    }

    private void setSSLProperties()
    {
        properties.setProperty("port", SSLPORT);
        properties.setProperty("protocol", SSL);
        properties.setProperty("sslVersion","TLS");


        properties.setProperty("authenticate", "true");
        properties.setProperty("username", "username");
        properties.setProperty("password", "password");
    }

}