package energy.usef.core.filter;


import energy.usef.core.config.Config;
import java.io.IOException;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@PreMatching
public class AllowedEndpointFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllowedEndpointFilter.class);

    @Inject
    private Config config;

    public AllowedEndpointFilter() {
    }

    public AllowedEndpointFilter(final Config config) {
        this.config = config;
    }

    @Override
    public void filter(ContainerRequestContext reqContext) throws IOException {
        if (!isPathOnWhitelist(reqContext.getUriInfo().getPath())) {
            LOGGER.info("Path " + reqContext.getUriInfo().getPath() + " is not whitelisted returning FORBIDDEN");
            reqContext.abortWith(Response.status(Status.FORBIDDEN).build());
        }
    }

    private boolean isPathOnWhitelist(String path) {
        String allowedRestPaths = config.getProperties().getProperty("ALLOWED_REST_PATHS");
        LOGGER.trace("Config ALLOWED_REST_PATHS is: " + allowedRestPaths + " request path is: " + path);

        if (!StringUtils.isEmpty(allowedRestPaths)) {
            if ("*".equals(allowedRestPaths)) {
                return true;
            } else {
                return isPathOnWhitelist(path, allowedRestPaths.split(","));
            }
        }
        return false;
    }

    private Boolean isPathOnWhitelist(String requestPath, String[] paths) {
        for (String path : paths) {
            if (requestPath.contains(StringUtils.strip(path))) {
                return true;
            }
        }
        return false;
    }

}
