package com.idevhub.coino.tradingengine.service.dto;

import com.idevhub.coino.tradingmodel.entity.enumeration.MarketStatus;

import java.io.Serializable;

public class MarketPairStatusDTO implements Serializable {

    private String base;

    private String quote;

    private MarketStatus status;

    public MarketPairStatusDTO() {
    }

    public MarketPairStatusDTO(String base, String quote, MarketStatus status) {
        this.base = base;
        this.quote = quote;
        this.status = status;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }

    public MarketStatus getStatus() {
        return status;
    }

    public void setStatus(MarketStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "MarketPairStatusDTO{" +
            "base='" + base + '\'' +
            ", quote='" + quote + '\'' +
            ", status=" + status +
            '}';
    }
}
