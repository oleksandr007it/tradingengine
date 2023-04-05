package com.idevhub.coino.tradingengine.messaging;


import com.idevhub.coino.tradingengine.Engine;
import com.idevhub.coino.tradingengine.service.dto.MarketPairStatusDTO;
import com.idevhub.coino.tradingengine.service.settings.TradingEngineSettingsManager;
import com.idevhub.coino.tradingmodel.entity.Order;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class Messaging {
    private final Logger log = LoggerFactory.getLogger(Messaging.class);
    private final Engine engine;
    private final TradingEngineSettingsManager tradingEngineSettingsManager;

    @StreamListener(MessagingChannels.TRADING_ACCOUNT_REQUEST_PLACE_ORDER)
    @Transactional
    public void tradingAccounRequestPlaceOrder(Order order) throws InterruptedException {
        log.info("method=[tradingAccounRequestPlaceOrder] action=[TRY PUT Order to Queue] " +
                "Order=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]" +
                "MarketQueue=[size={}]"
            , order.id, order.status, order.type, order.side, order.volume, order.amountToSpend,
            order.price, engine.getMarketQueue().size());
        engine.getMarketQueue().put(order);
    }

    @StreamListener(MessagingChannels.TRADING_ACCOUNT_REQUEST_CHANGE_MARKET_STATUS)
    @Transactional
    public void tradingAccountRequestChangeMarketStatus(MarketPairStatusDTO marketPairStatusDTO) {
        log.debug("MESSAGE_IN for changing market status: {}", marketPairStatusDTO);
        tradingEngineSettingsManager.setMarketStatus(marketPairStatusDTO.getStatus());
    }
}
