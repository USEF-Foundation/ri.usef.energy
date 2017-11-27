package energy.usef.core.endpoint;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import energy.usef.core.config.ConfigParam;
import energy.usef.core.config.DefaultConfig;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import javax.ws.rs.core.Response;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class ConfigurationEndpointTest {

    @Mock
    private DefaultConfig config;

    @Test
    public void noWhitelistSetShouldNotReturnProperties() throws IOException {
        Properties properties = new Properties();
        properties.put("test", "1234");
        when(config.getProperties()).thenReturn(properties);

        Response response = new ConfigurationEndpoint(config).getAllCommonReferenceOperators();
        Object json = response.getEntity();

        assertThat(response.getStatus(), is(200));
        Map jsonAsMap = new ObjectMapper().readValue((String) json, Map.class);
        assertTrue(jsonAsMap.isEmpty());
    }

    @Test
    public void whitelistSetButNoMatchingProperties() throws IOException {
        Properties properties = new Properties();
        properties.put("server", "1234");
        properties.put(ConfigParam.CONFIGURATION_ENDPOINT_PROPERTIES_WHITELIST.name(), "password");
        when(config.getProperties()).thenReturn(properties);

        Response response = new ConfigurationEndpoint(config).getAllCommonReferenceOperators();
        Object json = response.getEntity();

        assertThat(response.getStatus(), is(200));
        Map jsonAsMap = new ObjectMapper().readValue((String) json, Map.class);
        assertTrue(jsonAsMap.isEmpty());
    }

    @Test
    public void whitelistSetAndOneMatchingProperties() throws IOException {
        Properties properties = new Properties();
        properties.put("server", "1234");
        properties.put("password", "abcd");
        properties.put(ConfigParam.CONFIGURATION_ENDPOINT_PROPERTIES_WHITELIST.name(), "server");
        when(config.getProperties()).thenReturn(properties);

        Response response = new ConfigurationEndpoint(config).getAllCommonReferenceOperators();
        Object json = response.getEntity();

        assertThat(response.getStatus(), is(200));
        Map jsonAsMap = new ObjectMapper().readValue((String) json, Map.class);
        assertTrue(jsonAsMap.containsKey("server"));
        assertFalse(jsonAsMap.containsKey("password"));
    }

}