package com.idevhub.coino.tradingmodel.entity;

import com.idevhub.coino.tradingmodel.entity.enumeration.OrderSide;
import com.idevhub.coino.tradingmodel.entity.enumeration.OrderStatus;
import com.idevhub.coino.tradingmodel.entity.enumeration.OrderType;
import com.idevhub.coino.tradingmodel.entity.enumeration.Ticker;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Order implements Comparable<Order>, Serializable {

    public Long id;

    public OrderType type;
    public OrderSide side;
    public BigDecimal volume;
    public BigDecimal price;
    public BigDecimal amountToSpend;
    public BigDecimal stopPrice;
    public Instant createdDate;
    public Short countOfTrade;
    public OrderStatus status;
    public Ticker base;
    public Ticker quote;

    public static Order limit(Long id, OrderSide side, BigDecimal volume, BigDecimal price) {
        return builder()
            .id(id)
            .price(price)
            .type(OrderType.LIMIT)
            .side(side)
            .volume(volume)
            .createdDate(Instant.now())
            .build();
    }

    public static Order stopLimit(Long id, OrderSide side, BigDecimal stopPrice, BigDecimal volume, BigDecimal price) {
        return builder()
            .id(id)
            .price(price)
            .type(OrderType.STOP_LIMIT)
            .side(side).stopPrice(stopPrice)
            .volume(volume)
            .createdDate(Instant.now())
            .build();
    }

    public static Order market(Long id, OrderSide side, BigDecimal volume, BigDecimal amountToSpend) {
        return builder()
            .id(id)
            .amountToSpend(amountToSpend)
            .type(OrderType.MARKET)
            .side(side)
            .volume(volume)
            .createdDate(Instant.now())
            .build();
    }


    @Override
    public int compareTo(Order o) {
        int result;
        val thatPrice = o.getPrice() != null ? o.getPrice() : BigDecimal.ZERO;
        val thisPrice = this.getPrice() != null ? this.getPrice() : BigDecimal.ZERO;
        if (o.getSide() == OrderSide.BID) {
            result = thatPrice.compareTo(thisPrice);
            if (result != 0) return result;
        } else {
            result = thisPrice.compareTo(thatPrice);
            if (result != 0) return result;
        }

        val thatDate = o.getCreatedDate() != null ? o.getCreatedDate() : Instant.MIN;
        val thisDate = this.getCreatedDate() != null ? this.getCreatedDate() : Instant.MIN;
        result = thisDate.compareTo(thatDate);
        if (result != 0) return result;

        val thatVolume = o.getVolume() != null ? o.getVolume() : BigDecimal.ZERO;
        val thisVolume = this.getVolume() != null ? this.getVolume() : BigDecimal.ZERO;
        result = thatVolume.compareTo(thisVolume);
        if (result != 0) return result;

        Long thatId = o.getId() != null ? o.getId() : 0L;
        Long thisId = this.getId() != null ? this.getId() : 0L;
        result = thatId.compareTo(thisId);
        if (result != 0) return result;

        return -1;
    }
}
