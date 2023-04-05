package com.idevhub.coino.tradingengine.config;

import com.idevhub.coino.tradingmodel.entity.enumeration.Ticker;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Properties specific to Nodebtc.
 * <p>
 * Properties are configured in the application.yml file.
 * See {@link io.github.jhipster.config.JHipsterProperties} for a good example.
 */

@Configuration
@Data
@Accessors(chain = true)
@ConfigurationProperties(prefix = "trading", ignoreUnknownFields = false)
public class TradingProperties {
    private Ticker base;
    private Ticker quote;
}
