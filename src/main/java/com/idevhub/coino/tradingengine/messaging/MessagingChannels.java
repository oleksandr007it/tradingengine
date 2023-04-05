package com.idevhub.coino.tradingengine.messaging;


import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.stereotype.Service;

@Service
public interface MessagingChannels {
    // Channel names from application.yml
    String TRADING_ACCOUNT_REQUEST_PLACE_ORDER = "tradingaccounRequestPlaceOrder";
    String TRADING_ENGINE_RESPONSE_MAKE_TRADE = "tradingengineResponseMakeTrade";
    String TRADING_ENGINE_RESPONSE_CHANGE_LAST_PRICE = "tradingengineResponseChangeLastPrice";
    String TRADING_ACCOUNT_REQUEST_CHANGE_MARKET_STATUS = "tradingaccounRequestChangeMarketStatus";


    //
    // INPUT CHANNELS
    //
    @Input(TRADING_ACCOUNT_REQUEST_PLACE_ORDER)
    SubscribableChannel tradingAccounRequestPlaceOrder();

    @Input(TRADING_ACCOUNT_REQUEST_CHANGE_MARKET_STATUS)
    SubscribableChannel tradingaccounRequestChangeMarketStatus();


    //
    // OUTPUT CHANNELS
    //
    @Output(TRADING_ENGINE_RESPONSE_MAKE_TRADE)
    MessageChannel tradingEngineResponseMakeTrade();

    @Output(TRADING_ENGINE_RESPONSE_CHANGE_LAST_PRICE)
    MessageChannel tradingengineResponseChangeLastPrice();
}
