package com.idevhub.coino.tradingengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Properties specific to Nodebtc.
 * <p>
 * Properties are configured in the application.yml file.
 * See {@link io.github.jhipster.config.JHipsterProperties} for a good example.
 */

@Data
@Configuration
@ConfigurationProperties(prefix = "kafka-server", ignoreUnknownFields = false)
public class KafkaProperties {
    private String host;
}


