package com.idevhub.coino.tradingengine.service;

import com.idevhub.coino.tradingengine.Engine;
import com.idevhub.coino.tradingengine.service.settings.TradingEngineSettingsManager;
import com.idevhub.coino.tradingmodel.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;

@Service
public class TradingLauncher {
    private final Logger log = LoggerFactory.getLogger(Engine.class);
    private final Engine engine;
    private final TradingEngineSettingsManager tradingEngineSettingsManager;

    public TradingLauncher(Engine engine,
                           TradingEngineSettingsManager tradingEngineSettingsManager) {
        this.engine = engine;
        this.tradingEngineSettingsManager = tradingEngineSettingsManager;
    }

    @Scheduled(fixedRate = 1000)
    void timerLauncher() {

        log.debug("Before try take Order");

        try {
            Order newOrder = engine.getMarketQueue().take();
            log.debug("method=[timerLauncher] action=[Success TAKE Order from Queue] " +
                    "Order=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]" +
                    "MarketQueue=[size={}]"
                , newOrder.id, newOrder.status, newOrder.type, newOrder.side, newOrder.volume,
                newOrder.amountToSpend, newOrder.price, engine.getMarketQueue().size());

            if (tradingEngineSettingsManager.isFrozenMarketStatus()) {
                engine.getMarketQueue().put(newOrder);
                return;
            }

            engine.processingOrder(newOrder);

        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }


    }
}
