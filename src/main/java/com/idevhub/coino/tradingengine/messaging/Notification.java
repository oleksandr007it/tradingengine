package com.idevhub.coino.tradingengine.messaging;

import com.idevhub.coino.tradingmodel.entity.LastPriceWrapper;
import com.idevhub.coino.tradingmodel.entity.Order;
import com.idevhub.coino.tradingmodel.entity.Trade;
import com.idevhub.coino.tradingmodel.entity.enumeration.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class Notification {

    private final Logger log = LoggerFactory.getLogger(Notification.class);

    private final MessagingChannels messageChannels;

    public Notification(MessagingChannels messageChannels) {
        this.messageChannels = messageChannels;
    }


    public void notifyAboutTrade(Trade trade) {
        putTradeToMessagingСhannel(trade);
        log.debug("notifyAboutTrade make NEW Trade  volume={}  price ={}"
            , trade.getVolume(), trade.getPrice());
    }

    public void notifyAboutChangeLastPrice(LastPriceWrapper lastPriceWrapper) {
        putLastPriceToMessagingСhannel(lastPriceWrapper);
        log.debug("notifyAboutChangeLastPrice LastPrice   base={}  quote ={} lastPrice={}"
            , lastPriceWrapper.base, lastPriceWrapper.quote, lastPriceWrapper.lastPrice);
    }

    private void putTradeToMessagingСhannel(Trade trade) {
        messageChannels.tradingEngineResponseMakeTrade().send(MessageBuilder.withPayload(trade).build());
    }

    private void putLastPriceToMessagingСhannel(LastPriceWrapper lastPriceWrapper) {
        messageChannels.tradingengineResponseChangeLastPrice().send(MessageBuilder.withPayload(lastPriceWrapper).build());
    }


}
