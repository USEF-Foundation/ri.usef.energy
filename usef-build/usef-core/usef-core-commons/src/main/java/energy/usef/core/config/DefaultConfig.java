package energy.usef.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;

/**
 * Default property config class.
 *
 */
@ApplicationScoped
public class DefaultConfig extends AbstractConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConfig.class);

    /**
     * Default constructor.
     */
    public DefaultConfig() {
        try {
            readProperties();
        } catch (IOException e) {
            LOGGER.error("Error while loading the properties: {}.", e.getMessage(), e);
        }
    }

    /**
     * Initialize a bean after the instance has been constructed.
     */
    @PostConstruct
    public void initBean() {
        startConfigWatcher();
    }

    /**
     * Clean up the bean before destroying this instance.
     */
    @PreDestroy
    public void cleanupBean() {
        stopConfigWatcher();
    }

    /**
     * Gets a property value as a {@link String}.
     *
     * @param configParam the configuration parameter
     * @return property value
     */
    public String getProperty(ConfigParam configParam) {
        if (properties == null) {
            return null;
        }
        return properties.getProperty(configParam.name());
    }

    /**
     * Gets a property value as a {@link Double}.
     *
     * @param configParam the configuration parameter
     * @return property value
     */
    public Double getDoubleProperty(ConfigParam configParam) {
        return Double.parseDouble(properties.getProperty(configParam.name()));
    }

    /**
     * Gets a property value as a {@link Long}.
     *
     * @param configParam the configuration parameter
     * @return property value
     */
    public Long getLongProperty(ConfigParam configParam) {
        return Long.parseLong(properties.getProperty(configParam.name()));
    }

    /**
     * Gets a property value as an {@link Integer}.
     *
     * @param configParam the configuration parameter
     * @return property value
     */
    public Integer getIntegerProperty(ConfigParam configParam) {
        return Integer.parseInt(properties.getProperty(configParam.name()));
    }

}
