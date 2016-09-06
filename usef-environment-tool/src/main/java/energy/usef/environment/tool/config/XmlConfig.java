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

package energy.usef.environment.tool.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import energy.usef.environment.tool.yaml.RoleConfig;

public class XmlConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(XmlConfig.class);
    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private static final int NUMBER_OF_QUEUES = 3;

    private XmlConfig() {
        // empty constructor.
    }

    public static String processLogbackXml(String xmlContent, String uniqueDomainRoleName)
            throws ParserConfigurationException, SAXException, IOException, TransformerException {
        Document doc = loadXmlDocument(xmlContent);

        NodeList nodelistPattern = doc.getElementsByTagName("fileNamePattern");
        if (nodelistPattern.getLength() != 2) {
            LOGGER.error("The logbackXml should contain only 2 elements with name 'fileNamePattern'. Probably the USEF " +
                    "Environment Tool needs to be adjusted.");
            System.exit(1);
        }

        nodelistPattern.item(0).setTextContent("${jboss.server.log.dir}/" + uniqueDomainRoleName + "/default%d{yyyy-MM-dd_HH}.log");
        nodelistPattern.item(1).setTextContent(
                "${jboss.server.log.dir}/" + uniqueDomainRoleName + "/confidential%d{yyyy-MM-dd_HH}.log");

        NodeList nodelistFile = doc.getElementsByTagName("file");
        if (nodelistFile.getLength() != 2) {
            LOGGER.error("The logbackXml should contain only 2 elements with name 'file'. Probably the USEF Environment Tool " +
                    "needs to be adjusted.");
            System.exit(1);
        }

        nodelistFile.item(0).setTextContent("${jboss.server.log.dir}/" + uniqueDomainRoleName + "/default-messages.log");
        nodelistFile.item(1).setTextContent("${jboss.server.log.dir}/" + uniqueDomainRoleName + "/confidential-messages.log");

        NodeList nodelistAdditional = doc.getElementsByTagName("include");
        if (nodelistAdditional.getLength() != 1) {
            LOGGER.error("The logbackXml should contain only 2 elements with name 'include'. Probably the USEF Environment Tool " +
                    "needs to be adjusted.");
            System.exit(1);
        }

        nodelistAdditional.item(0).getAttributes().getNamedItem("file").
                setTextContent("${jboss.server.config.dir}/" + uniqueDomainRoleName + "/" + ToolConfig.LOGBACK);

        return saveXmlDocument(doc);
    }

    public static String processPersistenceXml(String xmlContent, RoleConfig roleConfig)
            throws ParserConfigurationException, SAXException, IOException, TransformerException {

        String dataSourceName = roleConfig.getUniqueDatasourceName();
        String dbSchemaName = roleConfig.getUniqueDbSchemaName();
        Document doc = loadXmlDocument(xmlContent);

        doc.getElementsByTagName("jta-data-source").item(0).setTextContent("java:jboss/datasources/" + dataSourceName );

        NodeList nodelist = doc.getElementsByTagName("property");
        boolean found = false;
        for (int i = 0; i < nodelist.getLength(); i++) {
            Node nameNode = nodelist.item(i).getAttributes().getNamedItem("name");
            if ("hibernate.default_schema".equalsIgnoreCase(nameNode.getTextContent())) {
                found = true;
                Node valueNode = nodelist.item(i).getAttributes().getNamedItem("value");
                valueNode.setTextContent(dbSchemaName);
            }
        }

        if (!found) {
            LOGGER.error("Could not find parameter 'hibernate.default_schema' in the persistence.xml of the war. The USEF " +
                    "Environment Tool needs to be adjusted.");
            System.exit(1);
        }

        return saveXmlDocument(doc);
    }

    public static String processQueueConfigJms(String xmlContent, String uniqueDomainRoleName)
            throws ParserConfigurationException, SAXException, IOException, TransformerException {
        Document doc = loadXmlDocument(xmlContent);

        NodeList nodelist = doc.getElementsByTagName("entry");
        if (nodelist.getLength() != NUMBER_OF_QUEUES) {
            LOGGER.error("The USEF Environment Tooling expects {} queues in the file queue-config-jms.xml and found {}. " +
                    "The USEF tooling needs probably to be adjusted, because the USEF source code has been changed.",
                    NUMBER_OF_QUEUES, nodelist.getLength());
            System.exit(1);
        }

        nodelist.item(0).getAttributes().getNamedItem("name").
                setTextContent("java:jboss/exported/jms/usefInQueue_" + uniqueDomainRoleName);
        nodelist.item(1).getAttributes().getNamedItem("name").
                setTextContent("java:jboss/exported/jms/usefOutQueue_" + uniqueDomainRoleName);
        nodelist.item(2).getAttributes().getNamedItem("name").
                setTextContent("java:jboss/exported/jms/usefNotSentQueue_" + uniqueDomainRoleName);

        NodeList nodelistqueues = doc.getElementsByTagName("jms-queue");
        if (nodelistqueues.getLength() != NUMBER_OF_QUEUES) {
            LOGGER.error("The USEF Environment Tooling expects {} queues in the file queue-config-jms.xml and found {}. " +
                    "The USEF tooling needs probably to be adjusted, because the USEF source code has been changed.",
                    NUMBER_OF_QUEUES, nodelist.getLength());
            System.exit(1);
        }

        nodelistqueues.item(0).getAttributes().getNamedItem("name").
                setTextContent("IN_QUEUE_" + uniqueDomainRoleName.toUpperCase());
        nodelistqueues.item(1).getAttributes().getNamedItem("name").
                setTextContent("OUT_QUEUE_" + uniqueDomainRoleName.toUpperCase());
        nodelistqueues.item(2).getAttributes().getNamedItem("name").
                setTextContent("NOT_SENT_QUEUE_" + uniqueDomainRoleName.toUpperCase());

        return saveXmlDocument(doc);
    }

    public static String processWebXml(String xmlContent, String uniqueDomainRoleName)
            throws ParserConfigurationException, SAXException, IOException, TransformerException {
        Document doc = loadXmlDocument(xmlContent);

        NodeList nodelist = doc.getElementsByTagName("jndi-name");
        if (nodelist.getLength() != NUMBER_OF_QUEUES) {
            LOGGER.error("The USEF Environment Tooling expects {} queues in the file jboss-web.xml and found {}. " +
                    "The USEF tooling needs probably to be adjusted, because the USEF source code has been changed.",
                    NUMBER_OF_QUEUES, nodelist.getLength());
            System.exit(1);
        }

        nodelist.item(0).setTextContent("java:jboss/exported/jms/usefInQueue_" + uniqueDomainRoleName);
        nodelist.item(1).setTextContent("java:jboss/exported/jms/usefOutQueue_" + uniqueDomainRoleName);
        nodelist.item(2).setTextContent("java:jboss/exported/jms/usefNotSentQueue_" + uniqueDomainRoleName);

        NodeList nodelistContextRoot = doc.getElementsByTagName("context-root");
        if (nodelistContextRoot.getLength() != 1) {
            LOGGER.error("No context root defined in the jboss-web.xml. Please adjust the USEF Environment Tooling.");
            System.exit(1);
        }
        nodelistContextRoot.item(0).setTextContent(uniqueDomainRoleName);

        return saveXmlDocument(doc);
    }

    public static String processEjb3Xml(String xmlContent, String uniqueDomainRoleName)
            throws ParserConfigurationException, SAXException, IOException, TransformerException {
        Document doc = loadXmlDocument(xmlContent);

        NodeList nodelist = doc.getElementsByTagName("activation-config-property-value");
        if (nodelist.getLength() != NUMBER_OF_QUEUES) {
            LOGGER.error("The USEF Environment Tooling expects {} queues in the file jboss-ejb3.xml and found {}. " +
                    "The USEF tooling needs probably to be adjusted, because the USEF source code has been changed.",
                    NUMBER_OF_QUEUES, nodelist.getLength());
            System.exit(1);
        }

        nodelist.item(0).setTextContent("java:jboss/exported/jms/usefInQueue_" + uniqueDomainRoleName);
        nodelist.item(1).setTextContent("java:jboss/exported/jms/usefOutQueue_" + uniqueDomainRoleName);
        nodelist.item(2).setTextContent("java:jboss/exported/jms/usefNotSentQueue_" + uniqueDomainRoleName);

        return saveXmlDocument(doc);
    }

    private static String saveXmlDocument(Document doc) throws TransformerFactoryConfigurationError,
            TransformerConfigurationException, TransformerException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(os);
        transformer.transform(source, result);
        return new String(os.toByteArray(), DEFAULT_CHARSET);
    }

    private static Document loadXmlDocument(String xmlContent) throws ParserConfigurationException, SAXException, IOException {
        InputStream is = new ByteArrayInputStream(xmlContent.getBytes(DEFAULT_CHARSET));
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(is);
        return doc;
    }

}
