package energy.usef.core.endpoint;

import energy.usef.core.config.ConfigParam;
import energy.usef.core.config.DefaultConfig;
import energy.usef.core.util.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Properties;

@Path("/configuration")
public class ConfigurationEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationEndpoint.class);

    @Inject
    private DefaultConfig config;

    /**
     * Empty constructor.
     */
    public ConfigurationEndpoint() { }

    public ConfigurationEndpoint(DefaultConfig config) {
        this.config = config;
    }

    /**
     * Endpoint to get a {@Link Properties} object containing the default configuration.
     *
     * @return a {@Link Response} message containing the default configuration
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllCommonReferenceOperators() {
        LOGGER.info("Received request to get Configuration");
        try {
            return Response.ok(JsonUtil.createJsonText(getWhitelistedProperties()), MediaType.APPLICATION_JSON_TYPE)
                    .build();
        } catch (IOException e) {
            LOGGER.error("{}", e);
            return Response.serverError().entity(JsonUtil.exceptionBody(e)).build();
        } finally {
            LOGGER.info("Processed request to get all Configuration");
        }
    }

    private Properties getWhitelistedProperties() {
        Properties properties = config.getProperties();
        String[] whitelistedPropertyKeys = properties
                .getProperty(ConfigParam.CONFIGURATION_ENDPOINT_PROPERTIES_WHITELIST.name(), "").split(",");
        Properties allowedProperties = new Properties();
        for (String whitelistedProperty : whitelistedPropertyKeys) {
            String property = properties.getProperty(whitelistedProperty);
            if (!StringUtils.isEmpty(property)) {
                allowedProperties.setProperty(whitelistedProperty, property);
            }
        }
        return allowedProperties;
    }

}
