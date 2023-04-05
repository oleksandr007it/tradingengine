package com.idevhub.coino.tradingengine.service.settings;

import com.idevhub.coino.settings.entity.SettingItem;

public enum TradingEngineSettings implements SettingItem {

    MARKET_STATUS("ACTIVE"),
    MIN_AMOUNT("0.0001");

    private String defaultValue;

    TradingEngineSettings(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String defaultValue() {
        return defaultValue;
    }
}
