package com.idevhub.coino.tradingmodel.entity.enumeration;

public enum Ticker {

    NONE("NONE", "Empty"),
    BTC("BTC","Bitcoin"),
    ETH("ETH","Ethereum"),
    LTC("LTC","Litecoin"),
    TUSD("TUSD","TrueUSD");

    private final String shortName;
    private final String description;

    Ticker(String shortName, String description) {
        this.shortName = shortName;
        this.description = description;
    }

    public String getShortName() {
        return shortName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return getShortName();
    }
}
