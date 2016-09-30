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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import energy.usef.core.rest.RestResult;
import energy.usef.core.rest.RestResultFactory;
import org.jboss.resteasy.util.HttpResponseCodes;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;

public class JsonUtil {

    public static final String JSON_V4_SCHEMA_IDENTIFIER = "http://json-schema.org/draft-04/schema#";
    public static final String JSON_SCHEMA_IDENTIFIER_ELEMENT = "$schema";

    public static final int ROOT_KEY = -1;

    private JsonUtil() {
        // Hide default constructor
    }


    /**
     * Convert any {@Link Object} to a json {@Link String}
     *
     * @param object the {@Link Object} to convert
     * @return a json {@Link String} representing the {@Link Object}
     */
    public static String createJsonText(Object object) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        StringWriter writer = new StringWriter();
        JsonGenerator generator = new JsonFactory().createGenerator(writer);
        mapper.writeValue(generator, object);
        generator.close();

        return writer.toString();
    }

    public static String exceptionBody(Exception e) {
        return "{\"exception\": " + e.getMessage() + "\"}";
    }

    /**
     * Create a {@Link JsonNode} from a resource
     *
     * @param resource a {@Link String} representing the resource
     * @return a {@Link JsonNode}
     */
    public static JsonNode getJsonNodeFromResource(String resource) throws IOException {
        return JsonLoader.fromResource(resource);
    }

    /**
     * Create a {@Link JsonSchema} from a resource
     *
     * @param resource a {@Link String} representing the resource
     * @return a {@Link JsonSchema}
     */
    public static JsonSchema getSchemaNodeFromResource(String resource) throws IOException, ProcessingException {
        final JsonNode schemaNode = getJsonNodeFromResource(resource);
        return getSchemaNode(schemaNode);
    }

    /**
     * Validate a {@Link JsonNode} against a {@Link JsonSchema}.
     *
     * @param jsonSchemaNode the {@Link JsonSchema} to validate against
     * @param jsonNode the{@Link JsonNode} to validate
     *
     * @return a {@Link ProcesingReport}
     */
    public static ProcessingReport isJsonValid(JsonSchema jsonSchemaNode, JsonNode jsonNode) throws ProcessingException {
        return jsonSchemaNode.validate(jsonNode, true);
    }

    private static JsonSchema getSchemaNode(JsonNode jsonNode) throws ProcessingException {
        final JsonNode schemaIdentifier = jsonNode.get(JSON_SCHEMA_IDENTIFIER_ELEMENT);
        if (null == schemaIdentifier) {
            ((ObjectNode) jsonNode).put(JSON_SCHEMA_IDENTIFIER_ELEMENT, JSON_V4_SCHEMA_IDENTIFIER);
        }

        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        return factory.getJsonSchema(jsonNode);
    }

    /**
     * Validate a {@Link JsonNode} against a json schema.
     *
     * @param schemaResource a {@Link String} indication the resource containing a json schema
     * @param node the {@Link JsonNode} to validate
     * @param report a {@Link Map} to hold the validation results
     */
    public static void validateNodeSyntax(String schemaResource, JsonNode node, Map<Integer, RestResult> report)
            throws IOException, ProcessingException {
        JsonSchema jsonSchema = JsonUtil.getSchemaNodeFromResource(schemaResource);

        ProcessingReport processingReport = JsonUtil.isJsonValid(jsonSchema, node);
        Iterator<ProcessingMessage> iterator = processingReport.iterator();
        while (iterator.hasNext()) {
            ProcessingMessage pm = iterator.next();
            JsonNode messageNode = pm.asJson();

            // Entry is 'root' for the top-level element, entry number for others.
            Integer entry = ROOT_KEY;
            if (!("".equals(messageNode.get("instance").get("pointer").asText()))) {
                entry = Integer.parseInt(messageNode.get("instance").get("pointer").asText().split("/")[1]);
            }
            String message = messageNode.get("message").asText();

            addRestResult(report, message, entry, HttpResponseCodes.SC_BAD_REQUEST);
        }
    }

    /**
     * Construct a RestResult stating that the method is not supported for a given entity
     *
     * @param method a http method
     * @param entity an entity name
     *
     * @return a {@Link RestResult}
     */

    public static RestResult notSupported (String method, String entity) {
        RestResult result = new RestResult();
        result.setCode(HttpResponseCodes.SC_NOT_IMPLEMENTED);
        result.getErrors().add(method + " is not supported for " + entity);

        return result;
    }

    /**
     * Construct a RestResult stating that the role is not supported in a certain context
     *
     * @param role a http method
     *
     * @return a {@Link RestResult}
     */

    public static RestResult unknownRole (String role) {
        RestResult result = new RestResult();
        result.setCode(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR);
        result.getErrors().add(role + " is not supported in this context");

        return result;
    }

    private static void addRestResult(Map<Integer, RestResult> report, String message, Integer entry, int httpCode) {
        if (report.containsKey(entry)) {
            RestResult dto = report.get(entry);
            dto.setCode(httpCode);
            dto.getErrors().add(message);
        } else {
            RestResult dto = RestResultFactory.getJsonRestResult();
            dto.setCode(httpCode);
            dto.getErrors().add(message);
            report.put(entry, dto);
        }
    }
}
