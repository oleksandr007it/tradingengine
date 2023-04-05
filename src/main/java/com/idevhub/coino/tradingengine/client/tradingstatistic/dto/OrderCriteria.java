package com.idevhub.coino.tradingengine.client.tradingstatistic.dto;

import com.idevhub.coino.tradingmodel.entity.enumeration.OrderSide;
import com.idevhub.coino.tradingmodel.entity.enumeration.OrderStatus;
import com.idevhub.coino.tradingmodel.entity.enumeration.OrderType;
import com.idevhub.coino.tradingmodel.entity.enumeration.Ticker;
import io.github.jhipster.service.filter.Filter;
import io.github.jhipster.service.filter.InstantFilter;
import io.github.jhipster.service.filter.LongFilter;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class OrderCriteria implements Serializable {
    public static class OrderTypeFilter extends Filter<OrderType> {}

    public static class OrderSideFilter extends Filter<OrderSide> {}

    public static class OrderStatusFilter extends Filter<OrderStatus> {}

    public static class OrderBaseFilter extends Filter<Ticker> {}

    public static class OrderQuoteFilter extends Filter<Ticker> {}

    private LongFilter id;

    private OrderTypeFilter type;
    private OrderSideFilter side;
    private InstantFilter createdDate;

    private OrderStatusFilter status;
    private OrderBaseFilter base;
    private OrderQuoteFilter quote;
}
