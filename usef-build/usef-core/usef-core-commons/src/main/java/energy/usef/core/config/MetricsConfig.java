package energy.usef.core.config;

import io.prometheus.client.hotspot.DefaultExports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * Metrics config class.
 *
 */
@Startup
@Singleton
public class MetricsConfig extends AbstractConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsConfig.class);

    /**
     * Initialize a bean after the instance has been constructed.
     */
    @PostConstruct
    public void initBean() {
        DefaultExports.initialize();
        LOGGER.info("Prometheus default metrics initialized");
    }

}
