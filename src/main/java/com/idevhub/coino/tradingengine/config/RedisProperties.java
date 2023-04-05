package com.idevhub.coino.tradingengine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;

/**
 * Properties specific to Nodebtc.
 * <p>
 * Properties are configured in the application.yml file.
 * See {@link io.github.jhipster.config.JHipsterProperties} for a good example.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "redis-server", ignoreUnknownFields = false)
public class RedisProperties {
    private String host;
    private String port;
}


