package energy.usef.dso.workflow.validate.acknowledgement.flexrequest;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import energy.usef.core.model.AcknowledgementStatus;
import energy.usef.dso.config.ConfigDso;
import java.util.Properties;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class DsoFlexRequestAcknowledgementCoordinatorTest {

    @Mock
    private ConfigDso config;
    private DsoFlexRequestAcknowledgementCoordinator coordinator;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9999);

    @Before
    public void init() {
        wireMockRule.stubFor(WireMock.post(urlEqualTo("/api/flexrequests/1/response")).willReturn(ok()));
        Properties properties = new Properties();
        properties.put("FLEX_REQUEST_ENDPOINT", "http://localhost:9999/");
        Mockito.when(config.getProperties()).thenReturn(properties);
        coordinator = new DsoFlexRequestAcknowledgementCoordinator(config);
    }

    @Test
    public void handleEvent() {
        FlexRequestAcknowledgementEvent event = new FlexRequestAcknowledgementEvent(1L, AcknowledgementStatus.ACCEPTED,
                "aggr1.com");
        coordinator.handleEvent(event);
    }

}