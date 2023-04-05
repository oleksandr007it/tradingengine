package com.idevhub.coino.tradingengine.web.rest;

import com.idevhub.coino.tradingengine.Engine;
import com.idevhub.coino.tradingmodel.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PutOrdersResource {
    private final Engine engine;

    @PutMapping("/add")
    public void changeLevel(@RequestBody Order order) throws InterruptedException {
        engine.getMarketQueue().put(order);
    }
}
