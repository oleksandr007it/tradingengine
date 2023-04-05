package com.idevhub.coino.tradingengine;

import com.idevhub.coino.tradingengine.config.TradingProperties;
import com.idevhub.coino.tradingengine.entity.OrderBook;
import com.idevhub.coino.tradingengine.messaging.Notification;
import com.idevhub.coino.tradingengine.service.OrderBlockingQueueProvider;
import com.idevhub.coino.tradingengine.service.settings.TradingEngineSettingsManager;
import com.idevhub.coino.tradingmodel.entity.LastPriceWrapper;
import com.idevhub.coino.tradingmodel.entity.Order;
import com.idevhub.coino.tradingmodel.entity.Trade;
import com.idevhub.coino.tradingmodel.entity.enumeration.OrderSide;
import com.idevhub.coino.tradingmodel.entity.enumeration.OrderStatus;
import com.idevhub.coino.tradingmodel.entity.enumeration.OrderType;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.concurrent.BlockingQueue;

import static com.idevhub.coino.tradingengine.utils.BigDecimalUtils.is;

@Service
@Getter
public class Engine {
    private static final String REDIS_KEY_BLOCKING_QUEUE_PREFIX = "Queue";
    private final Logger log = LoggerFactory.getLogger(Engine.class);
    private final Notification notification;
    private final LastPriceWrapper lastPriceWrapper;
    private final OrderBook orderBook;
    private final TradingProperties tradingProperties;
    private final int BIG_DECIMAL_SCALE = 19;
    private final int BIG_DECIMAL_PRECISION = 38;
    private BlockingQueue<Order> marketQueue;
    public BigDecimal minimumAmount;
    private final TradingEngineSettingsManager tradingEngineSettingsManager;

    public Engine(OrderBlockingQueueProvider orderBlockingQueueProvider, OrderBook orderBook, Notification notification, TradingProperties tradingProperties,
                  TradingEngineSettingsManager tradingEngineSettingsManager) {
        this.notification = notification;
        this.tradingProperties = tradingProperties;
        this.lastPriceWrapper = new LastPriceWrapper(new BigDecimal(BigInteger.ONE), tradingProperties.getBase(), tradingProperties.getQuote());
        this.tradingEngineSettingsManager = tradingEngineSettingsManager;
        this.marketQueue = orderBlockingQueueProvider.getBlockingQueue(REDIS_KEY_BLOCKING_QUEUE_PREFIX + tradingProperties.getBase());
        this.orderBook = orderBook;
    }


    public void processingOrder(Order order) throws InterruptedException {
        if (order.status == OrderStatus.CANCELLED) {
            cancelOrder(order);
        } else {
            minimumAmount = tradingEngineSettingsManager.getMinAmount();
            fillOrder(order);
        }
    }

    private void checkStopLists(SortedSet<Order> stopOrdersSet) throws InterruptedException {

        for (Order currentStopOrder : stopOrdersSet) {
            if (stopMeetLast(currentStopOrder)) {
                currentStopOrder.setType(currentStopOrder.getType() == OrderType.STOP_LIMIT ? OrderType.LIMIT : OrderType.MARKET);
                currentStopOrder.setStatus(OrderStatus.OPENED);
                sendNotificationEmptyCounterPart(currentStopOrder);
                log.debug("method=[checkStopLists] action=[TRY PUT Order to Queue] " +
                        "Order=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]" +
                        "MarketQueue=[size={}]"
                    , currentStopOrder.id, currentStopOrder.status, currentStopOrder.type, currentStopOrder.side, currentStopOrder.volume, currentStopOrder.amountToSpend, currentStopOrder.price, marketQueue.size());
                marketQueue.put(currentStopOrder);
                orderBook.removeStopOrder(currentStopOrder);
            }
        }

    }


    private void cancelOrder(Order order) {
        orderBook.removeOrder(order);
        log.debug("method=[cancelOrder] action=[CANCELLED Order] " +
                "Order=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]"
            , order.id, order.status, order.type, order.side, order.volume, order.amountToSpend, order.price);
        sendNotificationEmptyCounterPart(order);
    }

    private boolean preProcessingLimit(boolean isCounterParts, Order order) {

        if (!isCounterParts) {
            order.setStatus(OrderStatus.OPENED);
            sendNotificationEmptyCounterPart(order);
            orderBook.place(order);
            log.debug("method=[preProcessingLimit] action=[PLASE LIMIT Order to Order Book] " +
                    "Order=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]"
                , order.id, order.status, order.type, order.side, order.volume, order.amountToSpend, order.price);
            return false;
        } else {
            log.debug("method=[preProcessingLimit] action=[TRY FILL LIMIT Order] " +
                    "Order=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]"
                , order.id, order.status, order.type, order.side, order.volume, order.amountToSpend, order.price);
            return true;
        }

    }

    private boolean preProcessingStopLimit(Order order) {
        if (stopMeetLast(order)) {
            order.setType(OrderType.LIMIT);
            log.debug("method=[preProcessingStopLimit] action=[TRY FILL STOP LIMIT Order] " +
                    "Order=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]"
                , order.id, order.status, order.type, order.side, order.volume, order.amountToSpend, order.price);
            return true;
        } else {
            log.debug("method=[preProcessingStopLimit] action=[PLACE STOP LIMIT Order to stopLimitSortedSet] " +
                    "Order=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]"
                , order.id, order.status, order.type, order.side, order.volume, order.amountToSpend, order.price);
            orderBook.placeToStopList(order);
            return false;
        }
    }

    private boolean preProcessingMarket(boolean isCounterParts, Order order) {
        if (isCounterParts) {
            return true;
        } else {
            log.debug("method=[preProcessingMarket] action=[REJECTED  Order] " +
                    "Order=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]"
                , order.id, order.status, order.type, order.side, order.volume, order.amountToSpend, order.price);
            order.setStatus(OrderStatus.REJECTED);
            sendNotificationEmptyCounterPart(order);
            return false;
        }
    }

    private boolean preProcessingStopMarket(Order order) {
        if (stopMeetLast(order)) {
            order.setType(OrderType.MARKET);
            log.debug("method=[preProcessingStopMarket] action=[TRY FILL STOP MARKET Order] " +
                    "Order=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]"
                , order.id, order.status, order.type, order.side, order.volume, order.amountToSpend, order.price);
            return true;
        } else {
            log.debug("method=[preProcessingStopMarket] action=[PLACE STOP MARKET Order to stopMarketSortedSet] " +
                    "Order=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]"
                , order.id, order.status, order.type, order.side, order.volume, order.amountToSpend, order.price);
            orderBook.placeToStopList(order);
            return false;
        }
    }


    private void fillOrder(Order order) throws InterruptedException {

        boolean isProcessing = false;
        switch (order.getType()) {
            case STOP_LIMIT:
                isProcessing = preProcessingStopLimit(order);
                break;
            case STOP_MARKET:
                isProcessing = preProcessingStopMarket(order);
                break;
        }


        Iterator<Order> counterPartsIter = orderBook.getCounterParts(order).iterator();
        boolean isCounterParts = counterPartsIter.hasNext();

        switch (order.getType()) {
            case LIMIT:
                isProcessing = preProcessingLimit(isCounterParts, order);
                break;
            case MARKET:
                isProcessing = preProcessingMarket(isCounterParts, order);
                break;
        }

        if (isProcessing) {
            if ((order.getType() == OrderType.MARKET) && (order.getSide() == OrderSide.BID)) {
                log.debug("method=[fillOrder] action=[TRY FILL MARKET  BID Order] " +
                        "Order=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]"
                    , order.id, order.status, order.type, order.side, order.volume, order.amountToSpend, order.price);
                fillMarketBuy(order, counterPartsIter);
            } else {
                log.debug("method=[fillOrder] action=[TRY FILL Order] " +
                        "Order=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]"
                    , order.id, order.status, order.type, order.side, order.volume, order.amountToSpend, order.price);
                fillMarket(order, counterPartsIter);
            }
        }

    }


    private void fillMarket(Order order, Iterator<Order> counterPartsIter) throws InterruptedException {
        order.setStatus(OrderStatus.OPENED);
        sendNotificationEmptyCounterPart(order);
        log.debug("method=[FillMarket] action=[GET new order] Order=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]"
            , order.id, order.status, order.type, order.side, order.volume, order.amountToSpend, order.price);
        Short countOfTrade = 0;
        while (counterPartsIter.hasNext()) {

            Order counterPart = counterPartsIter.next();
            BigDecimal volume = is(counterPart.getVolume()).lt(order.getVolume()) ? counterPart.getVolume() : order.getVolume();
            Trade trade = new Trade(order, counterPart, volume, counterPart.getPrice(), Instant.now());


            order.setVolume(order.getVolume().subtract(volume));
            counterPart.setVolume(counterPart.getVolume().subtract(volume));
            order.setCountOfTrade(++countOfTrade);

            log.debug("method=[FillMarket] action=[MAKE new trade]  Trade=[Volume={} Price={}] " +
                    "ORDER=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={} countOfTrade={}]" +
                    "COUNTERPART=[v = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]", volume, trade.getPrice()
                , order.id, order.status, order.type, order.side, order.volume, order.amountToSpend, order.price, order.countOfTrade,
                counterPart.id, counterPart.status, counterPart.type, counterPart.side, counterPart.volume, counterPart.amountToSpend, counterPart.price);

            lastPriceWrapper.setLastPrice(counterPart.getPrice()); /* нужно оповестить рынок  о  изминении последней  цены*/
            notification.notifyAboutChangeLastPrice(lastPriceWrapper);
            checkStopLists(orderBook.getStopLimitSortedSet()); /* проверить стоп листы*/
            checkStopLists(orderBook.getStopMarketSortedSet());/* проверить стоп листы*/


            /*нужно оповестить  о новой сделке notify(:trade, trade) в кафку*/
            chekCompleteCounterPart(counterPart);
            if ((is(order.getVolume().multiply(lastPriceWrapper.getLastPrice())).lte(minimumAmount))) {
                // комплитнуть ордер выйти  из цикла и оповестить что ордер завершен
                order.setStatus(OrderStatus.COMPLETED);
                notification.notifyAboutTrade(trade);
                log.debug("method=[FillMarket] action=[NOTIFY about trade and complited]  Trade=[Volume={} Price={}] " +
                        "ORDER=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={} countOfTrade={}]" +
                        "COUNTERPART=[v = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]", volume, trade.getPrice()
                    , order.id, order.status, order.type, order.side, order.volume, order.amountToSpend, order.price, order.countOfTrade,
                    counterPart.id, counterPart.status, counterPart.type, counterPart.side, counterPart.volume, counterPart.amountToSpend, counterPart.price);

                break;
            } else {
                notification.notifyAboutTrade(trade);
                log.debug("method=[FillMarket] action=[NOTIFY about trade]  Trade=[Volume={} Price={}] " +
                        "ORDER=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={} countOfTrade={}]" +
                        "COUNTERPART=[v = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]", volume, trade.getPrice()
                    , order.id, order.status, order.type, order.side, order.volume, order.amountToSpend, order.price, order.countOfTrade,
                    counterPart.id, counterPart.status, counterPart.type, counterPart.side, counterPart.volume, counterPart.amountToSpend, counterPart.price);
            }
        }

        if ((order.status != OrderStatus.COMPLETED) && (order.type == OrderType.MARKET)) {
            log.debug("method=[FillMarket] action=[NOTIFY about COMPLETED Order reason empty Counter Parts]" +
                    "Order= [id = {} status ={}  type={} volume={} amountToSpend={} price ={}]"
                , order.id, order.status, order.type, order.volume, order.amountToSpend, order.price);
            order.setStatus(OrderStatus.COMPLETED);
            sendNotificationEmptyCounterPart(order);
        }
        if ((order.status != OrderStatus.COMPLETED) && (order.type == OrderType.LIMIT)) {
            orderBook.place(order);
        }
    }


    private void fillMarketBuy(Order order, Iterator<Order> counterPartsIter) throws InterruptedException {
        order.setStatus(OrderStatus.OPENED);
        sendNotificationEmptyCounterPart(order);
        log.debug("method=[fillMarketBuy] action=[GET new order] Order=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]"
            , order.id, order.status, order.type, order.side, order.volume, order.amountToSpend, order.price);
        Short countOfTrade = 0;
        while (counterPartsIter.hasNext()) {

            Order counterPart = counterPartsIter.next();
            BigDecimal volume = is(counterPart.getVolume()).lt(order.getVolume()) ? counterPart.getVolume() : order.getVolume();
            BigDecimal amount = volume.multiply(counterPart.getPrice());
            if (is(amount).gt(order.getAmountToSpend())) {
                volume = order.getAmountToSpend().divide(counterPart.getPrice(), BIG_DECIMAL_SCALE, RoundingMode.HALF_UP);
            }
            Trade trade = new Trade(order, counterPart, volume, counterPart.getPrice(), Instant.now());


            order.setVolume(order.getVolume().subtract(volume));
            BigDecimal newamount = volume.multiply(counterPart.getPrice());
            BigDecimal orderAmountToSpend = order.getAmountToSpend().subtract(newamount);
            order.setAmountToSpend(orderAmountToSpend);

            counterPart.setVolume(counterPart.getVolume().subtract(volume));
            order.setCountOfTrade(++countOfTrade);

            log.debug("method=[fillMarketBuy] action=[MAKE new trade]  Trade=[Volume={} Price={}] " +
                    "ORDER=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={} countOfTrade={}]" +
                    "COUNTERPART=[v = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]", volume, trade.getPrice()
                , order.id, order.status, order.type, order.side, order.volume, order.amountToSpend, order.price, order.countOfTrade,
                counterPart.id, counterPart.status, counterPart.type, counterPart.side, counterPart.volume, counterPart.amountToSpend, counterPart.price);


            lastPriceWrapper.setLastPrice(counterPart.getPrice());/* нужно оповестить рынок  о  изминении последней  цены*/
            notification.notifyAboutChangeLastPrice(lastPriceWrapper);

            checkStopLists(orderBook.getStopLimitSortedSet());/* проверить стоп листы*/
            checkStopLists(orderBook.getStopMarketSortedSet());/* проверить стоп листы*/
            /*нужно оповестить  о новой сделке notify(:trade, trade) в кафку*/
            chekCompleteCounterPart(counterPart);

            if ((is(order.getVolume().multiply(lastPriceWrapper.getLastPrice())).lte(minimumAmount))
                || (is(order.getAmountToSpend()).lte(minimumAmount))) {
                // комплитнуть ордер выйти  из цикла и оповестить что ордер завершен
                order.setStatus(OrderStatus.COMPLETED);
                notification.notifyAboutTrade(trade);

                log.debug("method=[fillMarketBuy] action=[NOTIFY about trade and complited]  Trade=[Volume={} Price={}] " +
                        "ORDER=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={} countOfTrade={}]" +
                        "COUNTERPART=[v = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]", volume, trade.getPrice()
                    , order.id, order.status, order.type, order.side, order.volume, order.amountToSpend, order.price, order.countOfTrade,
                    counterPart.id, counterPart.status, counterPart.type, counterPart.side, counterPart.volume, counterPart.amountToSpend, counterPart.price);


                break;
            } else {
                notification.notifyAboutTrade(trade);
                log.debug("method=[fillMarketBuy] action=[NOTIFY about trade]  Trade=[Volume={} Price={}] " +
                        "ORDER=[id = {} status ={}  type={} side={} volume={} amountToSpend={} price ={} countOfTrade={}]" +
                        "COUNTERPART=[v = {} status ={}  type={} side={} volume={} amountToSpend={} price ={}]", volume, trade.getPrice()
                    , order.id, order.status, order.type, order.side, order.volume, order.amountToSpend, order.price, order.countOfTrade,
                    counterPart.id, counterPart.status, counterPart.type, counterPart.side, counterPart.volume, counterPart.amountToSpend, counterPart.price);
            }

        }
        if (order.status != OrderStatus.COMPLETED) {
            order.setStatus(OrderStatus.COMPLETED);
            sendNotificationEmptyCounterPart(order);
            log.debug("method=[fillMarketBuy] action=[NOTIFY about COMPLETED Order reason empty Counter Parts]" +
                    "Order= [id = {} status ={}  type={} volume={} amountToSpend={} price ={}]"
                , order.id, order.status, order.type, order.volume, order.amountToSpend, order.price);
        }
    }


    private boolean stopMeetLast(Order order) {

        if (lastPriceWrapper == null) return false;
        if (order.getSide() == OrderSide.BID) {
            if (is(order.getStopPrice()).gte(lastPriceWrapper.getLastPrice()))
                return true; //order.stopPrice >= lastPriceWrapper;

        } else if (order.getSide() == OrderSide.ASK) {
            if (is(lastPriceWrapper.getLastPrice()).gte(order.getStopPrice()))
                return true;// lastPriceWrapper >= order.stopPrice
        }

        return false;
    }

    private void chekCompleteCounterPart(Order counterPart) {
        if (is(counterPart.getVolume().multiply(counterPart.getPrice())).lte(minimumAmount)) {
            counterPart.setStatus(OrderStatus.COMPLETED);
            orderBook.removeOrder(counterPart);
        } else {
            orderBook.modifyOrderBook(counterPart);
        }
    }

    private void sendNotificationEmptyCounterPart(Order order) {
        Trade trade = new Trade(order, null, new BigDecimal(BigInteger.ZERO), new BigDecimal(BigInteger.ZERO), Instant.now());
        notification.notifyAboutTrade(trade);
    }
}
