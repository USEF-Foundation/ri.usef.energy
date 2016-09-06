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

import static java.nio.charset.StandardCharsets.UTF_8;
import energy.usef.core.constant.USEFConstants;
import energy.usef.core.exception.TechnicalException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * XML utility.
 *
 */
public class XMLUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(XMLUtil.class);

    // XSD file that is used for validating XML (ALUS-184)
    private static final String MESSAGING_XSD_FILE = "xsd/messaging.xsd";

    private static final JAXBContext CONTEXT;

    private XMLUtil() {
        // private constructor.
    }

    static {
        try {
            CONTEXT = JAXBContext.newInstance(USEFConstants.XML_BEANS_PACKAGE);
        } catch (JAXBException e) {
            throw new TechnicalException(e.getMessage(), e);
        }
    }

    /**
     * Converts xml string to object and verifies if the the object is an instance of the specified class without validating.
     *
     * @param <T> message class
     *
     * @param xml xml string
     * @param clazz message object class
     *
     * @return corresponding object
     */
    @SuppressWarnings("unchecked")
    public static <T> T xmlToMessage(String xml, Class<T> clazz) {
        Object object = xmlToMessage(xml, false);
        if (object != null && clazz.isInstance(object)) {
            return (T) object;
        }

        throw new TechnicalException("Invalid XML content");
    }

    /**
     * Converts xml string to object and verifies if the the object is an instance of the specified class.
     *
     * @param <T> message class
     *
     * @param xml xml string
     * @param clazz message object class
     * @param validate true when the xml needs to be validated
     *
     * @return corresponding object
     */
    @SuppressWarnings("unchecked")
    public static <T> T xmlToMessage(String xml, Class<T> clazz, boolean validate) {
        Object object = xmlToMessage(xml, validate);
        if (object != null && clazz.isInstance(object)) {
            return (T) object;
        }

        throw new TechnicalException("Invalid XML content");
    }

    /**
     * Converts xml to an object without validating.
     *
     * @param xml xml string
     * @return object corresponding to this xml
     */
    public static Object xmlToMessage(String xml) {
        return xmlToMessage(xml, false);
    }

    /**
     * Converts xml to an object after optional validation against the xsd.
     *
     * @param xml xml string
     * @param validate true when the xml needs to be validated
     * @return object corresponding to this xml
     */
    public static Object xmlToMessage(String xml, boolean validate) {
        try (InputStream is = IOUtils.toInputStream(xml, UTF_8)) {
            Unmarshaller unmarshaller = CONTEXT.createUnmarshaller();

            if (validate) {
                // setup xsd validation
                SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = sf.newSchema(XMLUtil.class.getClassLoader().getResource(MESSAGING_XSD_FILE));

                unmarshaller.setSchema(schema);
            }

            return unmarshaller.unmarshal(is);
        } catch (JAXBException | IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new TechnicalException("Invalid XML content: " + e.getMessage(), e);
        } catch (SAXException e) {
            LOGGER.error(e.getMessage(), e);
            throw new TechnicalException("Unable to read XSD schema", e);
        }
    }

    /**
     * Converts message object string to xml.
     *
     * @param message message object
     * @return xml
     * 
     * @throws TechnicalException
     */
    public static String messageObjectToXml(Object message) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            CONTEXT.createMarshaller().marshal(message, os);

            return new String(os.toByteArray(), UTF_8);
        } catch (JAXBException | IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new TechnicalException("Unable to marshal object to xml", e);
        }
    }
}
