package com.idevhub.coino.tradingengine.service.client;

import com.codahale.metrics.annotation.Timed;
import com.idevhub.coino.tradingengine.client.AuthorizedFeignClient;
import com.idevhub.coino.tradingmodel.entity.enumeration.MarketStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Component
@AuthorizedFeignClient(name = "tradingaccount")
public interface TradingAccountFeignClient {

    @GetMapping("/api/status-market-setting")
    @Timed
    ResponseEntity<Map<String, MarketStatus>> getMarketStatusSetting();

}
