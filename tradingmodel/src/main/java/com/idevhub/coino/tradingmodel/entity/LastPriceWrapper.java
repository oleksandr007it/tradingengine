package com.idevhub.coino.tradingmodel.entity;

import com.idevhub.coino.tradingmodel.entity.enumeration.Ticker;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;


@Getter
@EqualsAndHashCode
public class LastPriceWrapper implements Serializable {
    public BigDecimal lastPrice = BigDecimal.ZERO;
    public final Ticker base;
    public final Ticker quote;

    public static LastPriceWrapper empty() {
        return new LastPriceWrapper(BigDecimal.ZERO, Ticker.NONE, Ticker.NONE);
    }

    public LastPriceWrapper() {
        this.base = Ticker.NONE;
        this.quote = Ticker.NONE;
    }

    public LastPriceWrapper(BigDecimal lastPrice, Ticker base, Ticker quote) {
        this.lastPrice = lastPrice;
        this.base = base;
        this.quote = quote;
    }

    public void setLastPrice(BigDecimal lastPrice) {
        this.lastPrice = lastPrice;
    }
}
