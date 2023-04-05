package com.idevhub.coino.tradingengine.entity;

import com.idevhub.coino.tradingengine.config.TradingProperties;
import com.idevhub.coino.tradingengine.service.OrderSortedSetProvider;
import com.idevhub.coino.tradingengine.utils.BigDecimalUtils;
import com.idevhub.coino.tradingmodel.entity.Order;
import com.idevhub.coino.tradingmodel.entity.enumeration.OrderType;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.idevhub.coino.tradingmodel.entity.enumeration.OrderSide.ASK;
import static com.idevhub.coino.tradingmodel.entity.enumeration.OrderSide.BID;
import static com.idevhub.coino.tradingmodel.entity.enumeration.OrderType.LIMIT;
import static com.idevhub.coino.tradingmodel.entity.enumeration.OrderType.STOP_LIMIT;

@Component
@Getter
public class OrderBook {
    public static final String REDIS_KEY_BIDS_SORTED_SET_PREFIX = "bidsSortedSet";
    public static final String REDIS_KEY_ASKS_SORTED_SET_PREFIX = "asksSortedSet";
    public static final String REDIS_KEY_STOP_MARKET_SORTED_SET_PREFIX = "stopMarketSortedSet";
    public static final String REDIS_KEY_STOP_LIMIT_SORTED_SET_PREFIX = "stopLimitSortedSet";
    private static final String REDIS_KEY_COUNTER_PARTS_SORTED_SET_PREFIX = "counterPartsSortedSet";

    private final OrderSortedSetProvider orderSortedSetProvider;
    private final TradingProperties tradingProperties;
    private SortedSet<Order> bidsSortedSet;
    private SortedSet<Order> asksSortedSet;
    private SortedSet<Order> stopMarketSortedSet;
    private SortedSet<Order> stopLimitSortedSet;
    private String baseQuote;

    public OrderBook(OrderSortedSetProvider orderSortedSetProvider, TradingProperties tradingProperties) {
        baseQuote = tradingProperties.getBase().name() + tradingProperties.getQuote().name();
        bidsSortedSet = orderSortedSetProvider.getSortedSet(REDIS_KEY_BIDS_SORTED_SET_PREFIX + baseQuote);
        asksSortedSet = orderSortedSetProvider.getSortedSet(REDIS_KEY_ASKS_SORTED_SET_PREFIX + baseQuote);
        stopMarketSortedSet = orderSortedSetProvider.getSortedSet(REDIS_KEY_STOP_MARKET_SORTED_SET_PREFIX + baseQuote);
        stopLimitSortedSet = orderSortedSetProvider.getSortedSet(REDIS_KEY_STOP_LIMIT_SORTED_SET_PREFIX + baseQuote);

        this.orderSortedSetProvider = orderSortedSetProvider;
        this.tradingProperties = tradingProperties;
    }


    public void place(Order order) {
        switch (order.getSide()) {
            case ASK://цена, по которой продавец готов продать.
                // по  возрастанию  цена объем время размещение в ордер бук самый большой объем сверху
                asksSortedSet.add(order);
                break;

            case BID://цена, которую готов заплатить покупатель финансового инструмента. кто  хочет купить
                // по  убыванию  цена, объем по  убыванию   и время размещение в ордер бук
                bidsSortedSet.add(order);
                break;
        }
    }

    public void placeToStopList(Order order) {
        switch (order.getType()) {
            case STOP_LIMIT:
                stopLimitSortedSet.add(order);
                break;

            case STOP_MARKET://цена, которую готов заплатить покупатель финансового инструмента. кто  хочет купить
                // по  убыванию  цена, объем по  убыванию   и время размещение в ордер бук
                stopMarketSortedSet.add(order);
                break;
        }
    }

    public void removeStopOrder(Order order) {

        if ((order.getType() == STOP_LIMIT) || (order.getType() == LIMIT)) {
            stopLimitSortedSet.removeIf(o -> o.id.equals(order.id));
        } else {
            stopMarketSortedSet.removeIf(o -> o.id.equals(order.id));
        }
    }


    public void removeOrder(Order order) {

        if (order.getSide() == BID) {
            bidsSortedSet.removeIf(o -> o.id.equals(order.id));
        } else {
            asksSortedSet.removeIf(o -> o.id.equals(order.id));
        }
    }

    public void modifyOrderBook(Order counterParts) {
        removeOrder(counterParts);
        if (counterParts.getSide() == ASK) {
            asksSortedSet.add(counterParts);
        } else {
            bidsSortedSet.add(counterParts);

        }
    }


    public SortedSet<Order> getCounterParts(Order order) {
        SortedSet<Order> counterPartsSortedSet = orderSortedSetProvider.getSortedSet(REDIS_KEY_COUNTER_PARTS_SORTED_SET_PREFIX + baseQuote);

        counterPartsSortedSet.clear();
        if (order.getType() == OrderType.MARKET) {
            if (order.getSide() == ASK) {
                counterPartsSortedSet.addAll(bidsSortedSet);
                return counterPartsSortedSet;
            } else {
                counterPartsSortedSet.addAll(asksSortedSet);
                return counterPartsSortedSet;
            }
        }


        if (order.getType() == LIMIT) {
            if (order.getSide() == ASK) {
                /* ограничить  по  цене все  что  больше*/
                counterPartsSortedSet.addAll(bidsSortedSet.stream().filter(currentBidsOrder ->
                    BigDecimalUtils.is(currentBidsOrder.getPrice()).gte(order.getPrice()))
                    .collect(Collectors.toCollection(TreeSet::new)));
                return counterPartsSortedSet;
            } else {
                /* ограничить  по  цене все  что меньше*/
                counterPartsSortedSet.addAll(asksSortedSet.stream().filter(currentAsksOrder ->
                    BigDecimalUtils.is(currentAsksOrder.getPrice()).lte(order.getPrice()))
                    .collect(Collectors.toCollection(TreeSet::new)));
                return counterPartsSortedSet;
            }
        }
        return counterPartsSortedSet;

    }

    public void clear() {
        bidsSortedSet.clear();
        asksSortedSet.clear();
        stopMarketSortedSet.clear();
        stopLimitSortedSet.clear();
    }
}
