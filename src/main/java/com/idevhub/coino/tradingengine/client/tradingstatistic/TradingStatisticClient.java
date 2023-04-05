package com.idevhub.coino.tradingengine.client.tradingstatistic;

import com.idevhub.coino.tradingengine.client.AuthorizedFeignClient;
import com.idevhub.coino.tradingengine.client.tradingstatistic.dto.OrderCriteria;
import com.idevhub.coino.tradingmodel.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@AuthorizedFeignClient(name = "tradingstatistic")
public interface TradingStatisticClient {
    @GetMapping("/api/orders")
    ResponseEntity<List<Order>> getAllOrders(@RequestParam("criteria") OrderCriteria criteria, @RequestParam("pageable") Pageable pageable);
}
