package energy.usef.core.filter;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import energy.usef.core.config.Config;
import java.io.IOException;
import java.util.Properties;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AllowedEndpointFilterTest {

    @Mock
    private Config config;

    @Mock
    private ContainerRequestContext context;

    @Mock
    private UriInfo uriInfo;

    @Captor
    private ArgumentCaptor<Response> responseArgumentCaptor;


    @Test
    public void noWhitelistPropertyShouldReturn403() throws IOException {
        when(config.getProperties()).thenReturn(new Properties());
        when(context.getUriInfo()).thenReturn(uriInfo);
        new AllowedEndpointFilter(config).filter(context);

        verify(context).abortWith(any());
        assertThat(responseArgumentCaptor.getValue().getStatus(), CoreMatchers.is(403));
    }

    @Test
    public void whitelistPropertyAnyShouldNotReturn403() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("ALLOWED_REST_PATHS", "*");
        when(config.getProperties()).thenReturn(properties);
        when(context.getUriInfo()).thenReturn(uriInfo);
        new AllowedEndpointFilter(config).filter(context);

        verify(context, times(0)).abortWith(responseArgumentCaptor.capture());
    }

    @Test
    public void whitelistPropertyOneUrlShouldNotReturn403() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("ALLOWED_REST_PATHS", "/USEF/2015/SenderService");
        when(uriInfo.getPath()).thenReturn("/USEF/2015/SenderService");
        when(config.getProperties()).thenReturn(properties);
        when(context.getUriInfo()).thenReturn(uriInfo);
        new AllowedEndpointFilter(config).filter(context);

        verify(context, times(0)).abortWith(responseArgumentCaptor.capture());
    }

    @Test
    public void whitelistPropertyMultipleUrlShouldNotReturn403() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("ALLOWED_REST_PATHS", "/USEF/2015/SenderService, /USEF/2015/OtherService");
        when(uriInfo.getPath()).thenReturn("/USEF/2015/OtherService");
        when(config.getProperties()).thenReturn(properties);
        when(context.getUriInfo()).thenReturn(uriInfo);
        new AllowedEndpointFilter(config).filter(context);

        verify(context, times(0)).abortWith(responseArgumentCaptor.capture());
    }

}