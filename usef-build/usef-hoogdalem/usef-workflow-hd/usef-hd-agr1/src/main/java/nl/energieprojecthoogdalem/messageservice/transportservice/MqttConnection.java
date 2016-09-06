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

import info.usef.core.config.AbstractConfig;
import nl.energieprojecthoogdalem.messageservice.transportservice.mqttmessages.MqttResponseMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.joda.time.DateTime;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Asynchronous;
import javax.ejb.AsyncResult;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.security.SecureRandom;


import java.util.Properties;
import java.util.concurrent.Future;

/**
 * Mqtt transport service class for sending mqtt messages
 * */
@Startup
@Singleton
@ApplicationScoped
public class MqttConnection
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttConnection.class);

    private Properties prop;

    private MqttClient mqttClient;

    @Inject
    private MessageCallback messageCallback;

    //reference to self, to execute connect async
    @Inject
    private MqttConnection mqttConnection;

    /**
     * initializes the {@link MqttClient} with the {@link MessageCallback}
     * reads properties from the mqtt.properties file
     * and connects to the broker
     * note: should only be called from EJB @PostConstruct
     * */
    @PostConstruct
    public void init()
    {
        prop = new Properties();
        try
        {
            prop.load(new FileReader(AbstractConfig.getConfigurationFolder() + "mqtt.properties"));

            final String id = getClientID();
            LOGGER.trace("client id {}", id);

            mqttClient = new MqttClient(getHost(), id, new MemoryPersistence());
            mqttClient.setCallback(messageCallback);

            //no need to wait on connect, if unable to connect the exception will call the messageCallback.connectionLost and will call connect again (here we wait until its done)
            mqttConnection.connect();

        }
        catch (IOException exception)
        {
            LOGGER.warn("Unable to read mqtt properties reason: {}", exception.getMessage());
        }
        catch(MqttException exception)
        {
            LOGGER.error("Unable to create client, reason: ", exception);
        }
    }

    //configuration
    /**
     * returns an unique client id depending on the current time
     * in format "client id" + unix timestamp
     * @return a string containing an unique client id read from the properties file
     * */
    private String getClientID()
    {
        //LOGGER.trace("clientID: {}", prop.getProperty("clientID", "java-paho-pahomqtt-client") );
        return prop.getProperty("clientID", "java-pahomqtt-client") + new DateTime().getMillis();
    }

    /**
     * retrieves the host string in paho MqttClient format
     * format example 1 tcp://127.0.1.1:1500 (uses plain tcp, localhost, port 1500)
     * format example 2 ssl://example.com:8883 (uses encrypted ssl, domain example.com, default mqtt ssl port 8883)
     * @return a host string read from the protocol, host, port properties file
     * */
    private String getHost()
    {
        LOGGER.trace("host: {}", prop.getProperty("protocol", "tcp") + "://" + prop.getProperty("host", "127.0.0.1") + ":" + prop.getProperty("port", "1883") );
        return prop.getProperty("protocol", "tcp") + "://" + prop.getProperty("host", "127.0.0.1") + ":" + prop.getProperty("port", "1883");
    }

    /**
     * reads the mqtt properties file and returns the paho mqtt connection options class
     * parameters for the properties file:
     * host: a hostname or ip address of the broker (defaults to 127.0.0.1)
     * port: the port number of the broker (*ALWAYS defaults to 1883)
     * protocol: the network protocol to use valid values ssl, tcp (defaults to tcp)
     * sslVersion: the ssl version available in java to use (defaults to TLS) @see SSLContext for possible values
     *
     * clientID: the client id string to use, will always be appended by an unix timestamp suffix (defaults to java-pahomqtt-client)
     *
     * authenticate: use username and password authentication valid values true, false (defaults to false)
     * username the user to authenticate with (defaults to "", an empty string)
     * password the password to authenticate with (defaults to "", an empty string)
     *
     * cleanSession if a clean session will be kept valid values true, false (defaults to true)
     *
     * willTopic the topic string to publish if the client connection is lost with the broker (WILL is not used if left emtpy)
     * willMessage the message to publish if the client connection is lost with the broker (defaults to empty string, ignored if willTopic is not set)
     * willQOS the quality of service level to use when publishing if the client connection is lost with the broker valid values 0, 1, 2 (defaults to 0, ignored if willTopic is not set)
     * willRetain if the message needs to be retained when publishing if the client connection is lost with the broker valid values true, false (defaults to false, ignored if willTopic is not set)
     *
     * @return {@link MqttConnectOptions} options with the values set from the properties file
     * */
    private MqttConnectOptions getConfiguration()
    {
        MqttConnectOptions options = new MqttConnectOptions();

        LOGGER.trace("using authentication: {}", prop.getProperty("authenticate","false") );
        if( prop.getProperty("authenticate","false").equals("true") )
        {
            LOGGER.trace("using username: {}, using password: {}", prop.getProperty("username", ""), ( ! prop.getProperty("password", "").equals("") ));
            options.setUserName( prop.getProperty("username", "") );
            options.setPassword( prop.getProperty("password", "").toCharArray() );
        }

        LOGGER.trace("using protocol: {}", prop.getProperty("protocol", "tcp"));
        if( prop.getProperty("protocol","tcp").equals("ssl") )
        {
            //LOGGER.trace("using SSL version: {}", prop.getProperty("sslVersion", "TLS"));
            try
            {
                final SSLContext sslContext = SSLContext.getInstance( prop.getProperty("sslVersion","TLS")  );
                sslContext.init(null, TRUSTMANAGER, new SecureRandom());
                options.setSocketFactory( sslContext.getSocketFactory());
            }
            catch (NoSuchAlgorithmException | KeyManagementException exception)
            {
                LOGGER.error("Unable to apply SSL properties, reason: ", exception);
            }

        }


        if( ! prop.getProperty("willTopic","").equals("") )
        {
            LOGGER.trace("will topic: {} message: {} qos: {} retain: {}"
                        , prop.getProperty("willTopic","")
                        , prop.getProperty("willMessage","")
                        , Integer.parseInt( prop.getProperty("willQOS","0") )
                        , Boolean.parseBoolean(prop.getProperty("willRetain","false"))
            );
            options.setWill(prop.getProperty("willTopic","")
                    , prop.getProperty("willMessage","").getBytes()
                    , Integer.parseInt( prop.getProperty("willQOS","0") )
                    , Boolean.parseBoolean(prop.getProperty("willRetain","false"))
            );
        }

        LOGGER.trace("clean session: {}", prop.getProperty("cleanSession", "true"));
        options.setCleanSession( Boolean.parseBoolean(prop.getProperty("cleanSession","true")) );

        return options;
    }

    //connection
    /**
     * disconnects and disables the MqttClient
     * note: should only be called by EJB @PreDestroy
     * */
    @PreDestroy
    public void disconnect()
    {
        if( mqttClient != null)
        {
            try
            {
                mqttClient.disconnect();
                mqttClient.close();
                LOGGER.info("mqtt client has been disconnected.");
                mqttClient = null;
            }
            catch (MqttException exception)
            {
               LOGGER.error("Unable to disconnect mqtt client, reason: ", exception);
            }
        }
        else
            LOGGER.warn("mqtt client ");
    }

    /**
     * attempts to (re)connect to the broker
     * @return an asynchronous boolean result true if a connection can be made or false if no connection can be made
     * */
    @Asynchronous
    public Future<Boolean> connect()
    {
        if(mqttClient != null)
        {
            String host = getHost();
            LOGGER.trace("Connecting to broker {} ...", host);
            try
            {
                mqttClient.connect(getConfiguration());
                LOGGER.info("mqtt client connected.");
                return new AsyncResult<>(true);
            }
            catch (MqttSecurityException exception)
            {
                LOGGER.error("(security) Unable to establish connection with {}, reason ",host, exception);
                messageCallback.connectionLost(exception.getCause());
            }
            catch (MqttException exception)
            {
                LOGGER.error("Unable to establish connection with {}, reason ",host, exception);
                messageCallback.connectionLost(exception.getCause());
            }

        }
        else
        {
            LOGGER.error("Client was not initialized!");
        }
        return new AsyncResult<>(false);
    }

    /**
     * returns if the client is connected to the broker
     * @return boolean true if the client is connected or false if the client isn't connected
     * */
    public boolean isConnected(){return mqttClient.isConnected();}

    /**
     * returns a ssl TrustManager
     * */
    private static final TrustManager[] TRUSTMANAGER = new TrustManager[]
    {
        new X509TrustManager()
        {
            public X509Certificate[] getAcceptedIssuers()
                        {
                            return null;
                        }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        }
    };

    //messaging
    /**
     * publishes a message to a mqtt topic
     * uses QOS 0 and retain false
     * see the MqttConnection.publish( String topic, String content, boolean retain) for more details
     * */
    public void publish( String topic, String content)  {   publish(topic, content, 0, false);  }

    /**
     * publishes a message to a mqtt topic
     * uses retain false
     * see the MqttConnection.publish( String topic, String content, boolean retain) for more details
     * */
    public void publish( String topic, String content, int QOS) {   publish(topic, content, QOS, false);  }

    /**
     * publishes a message to a mqtt topic
     * uses QOS 0
     * see the MqttConnection.publish( String topic, String content, boolean retain) for more details
     * */
    public void publish( String topic, String content, boolean retain) {   publish(topic, content, 0, retain);  }

    /**
     * publishes a message to a mqtt topic using the given QOS and retain
     * @param topic the mqtt topic to publish
     * @param content the message payload to send with the topic
     * @param QOS the quality of service level (valid numbers 0, 1, 2)
     * @param retain if the message needs to be retained
     * */
    public void publish( String topic, String content, int QOS, boolean retain)
    {
        LOGGER.trace("Publishing topic: {} message: {} QOS: {} retain: {} time: {}", topic, content, QOS, retain, new DateTime());
        MqttMessage message = new MqttMessage(content.getBytes());
        message.setQos(QOS);
        message.setRetained(retain);
        try
        {
            mqttClient.publish(topic, message);
        }
        catch(MqttException exception)
        {
            LOGGER.error("Unable to send message (topic: {}), reason: ", topic, exception);
        }
    }

    /**
     * unsubscribes from the given mqtt topic string
     * @param topic the topic string to stop listening for new messages
     * */
    public void unsubscribe(String topic)
    {
        LOGGER.trace("Unsubscribing from: {}", topic);
        try
        {
            mqttClient.unsubscribe(topic);
        }
        catch(MqttException exception)
        {
            LOGGER.error("Unable to unsubscribe from topic: {}, reason: ", topic, exception);
        }

    }

    /**
     * subscribes to the mqtt topic string
     * uses QOS 0
     * see the MqttConnection.subscribe(String topic, int QOS) for more info
     * */
    public void subscribe(String topic){ subscribe(topic, 0); }

    /**
     * subscribes to the mqtt topic string
     * @param topic the topic string to start listening for new messages
     * @param QOS the quality of service level for the topics (valid numbers are 0, 1, 2)
     * */
    public void subscribe(String topic, int QOS)
    {
        LOGGER.trace("Subscribing to: {}", topic);
        try
        {
            mqttClient.subscribe(topic, QOS);
        }
        catch(MqttException exception)
        {
            LOGGER.error("Unable subscribing to topic: {}, reason: ", topic, exception);
        }
    }

    /**
     * publishes a message and puts a response message in the {@link MessageCallback} receive map using retain false and QOS 0
     * @param topic the mqtt topic to publish
     * @param message the message payload to send with the topic
     * @param toReceive a {@link MqttResponseMessage} with expected subscription topic set, where the received message payload will be set once received
     * */
    public void publishAndReceive(String topic, String message, MqttResponseMessage toReceive)
    {
        publishAndReceive(topic, message, 0, false, toReceive);
    }

    /**
     * publishes a message and puts a response message in the {@link MessageCallback} receive map using the given retain and QOS values
     * @param topic the mqtt topic to publish
     * @param message the message payload to send with the topic
     * @param QOS the quality of service level for the topics (valid numbers are 0, 1, 2)
     * @param retain if the message needs to be retained
     * @param toReceive a {@link MqttResponseMessage} with expected subscription topic set, where the received message payload will be set once received
     * */
    public void publishAndReceive(String topic, String message, int QOS, boolean retain, MqttResponseMessage toReceive)
    {
        messageCallback.addReceiveMessage(toReceive);
        publish(topic,message, QOS, retain);
    }

    /**
     * wrapper for the {@link MessageCallback} removeNonReceived method,
     *
     * removes a response message from the {@link MessageCallback} receive map
     *
     * @param topic a subscription topic
     * */
    public void removeNonReceived(String topic)
    {
        messageCallback.removeReceiveMessage(topic);
    }

}
