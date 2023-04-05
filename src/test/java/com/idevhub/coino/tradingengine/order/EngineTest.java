package com.idevhub.coino.tradingengine.order;

import com.idevhub.coino.tradingengine.Engine;
import com.idevhub.coino.tradingengine.TradingengineApp;
import com.idevhub.coino.tradingengine.config.SecurityBeanOverrideConfiguration;
import com.idevhub.coino.tradingengine.config.TradingProperties;
import com.idevhub.coino.tradingengine.entity.OrderBook;
import com.idevhub.coino.tradingengine.messaging.MessagingChannels;
import com.idevhub.coino.tradingengine.messaging.Notification;
import com.idevhub.coino.tradingengine.service.OrderBlockingQueueProvider;
import com.idevhub.coino.tradingengine.service.OrderSortedSetProvider;
import com.idevhub.coino.tradingengine.service.settings.TradingEngineSettingsManager;
import com.idevhub.coino.tradingmodel.entity.Order;
import com.idevhub.coino.tradingmodel.entity.enumeration.OrderSide;
import com.idevhub.coino.tradingmodel.entity.enumeration.OrderStatus;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.NullChannel;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static com.idevhub.coino.tradingmodel.entity.enumeration.OrderSide.ASK;
import static com.idevhub.coino.tradingmodel.entity.enumeration.OrderStatus.COMPLETED;
import static com.idevhub.coino.tradingmodel.entity.enumeration.Ticker.BTC;
import static com.idevhub.coino.tradingmodel.entity.enumeration.Ticker.LTC;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SecurityBeanOverrideConfiguration.class, TradingengineApp.class, EngineTest.EngineTestConfiguration.class})
@Log4j2
public class EngineTest {
    @Configuration
    static class EngineTestConfiguration {
        @Bean
        OrderSortedSetProvider orderSortedSetProvider() {
            return key -> new TreeSet<>();
        }

        @Bean
        OrderBlockingQueueProvider orderBlockingQueueProvider() {
            return key -> new ArrayBlockingQueue<>(100);
        }
    }

    @Mock
    private MessagingChannels spymessageChannels;

    @Autowired
    private OrderSortedSetProvider orderSortedSetProvider;

    @Autowired
    private OrderBlockingQueueProvider orderBlockingQueueProvider;

    @Autowired
    private TradingEngineSettingsManager tradingEngineSettingManager;

    private OrderBook orderBook;

    private Engine engine;

    private Order[] makeDefaultLimitOrders() {
        return new Order[]{
            limitOrder(1L, ASK, 100, 105),
            limitOrder(2L, ASK, 100, 103),
            limitOrder(3L, ASK, 80, 106),
            limitOrder(4L, ASK, 120, 106),
            limitOrder(5L, ASK, 100, 106),
            limitOrder(6L, ASK, 100, 106),
            limitOrder(7L, ASK, 100, 104),
            limitOrder(8L, ASK, 100, 107),
            limitOrder(1L, OrderSide.BID, 100, 106),
            limitOrder(2L, OrderSide.BID, 100, 104),
            limitOrder(3L, OrderSide.BID, 100, 107),
            limitOrder(4L, OrderSide.BID, 100, 106),
            limitOrder(5L, OrderSide.BID, 100, 104),
            limitOrder(6L, OrderSide.BID, 100, 107)
        };
    }

    @SuppressWarnings("Duplicates")
    @Before
    public void setup() {
        val tradingProperties = new TradingProperties().setBase(BTC).setQuote(LTC);
        orderBook = new OrderBook(orderSortedSetProvider, tradingProperties);
        engine = new Engine(orderBlockingQueueProvider, orderBook, new Notification(spymessageChannels), tradingProperties, tradingEngineSettingManager);

        doReturn(new NullChannel()).when(spymessageChannels).tradingEngineResponseMakeTrade();
        doReturn(new NullChannel()).when(spymessageChannels).tradingengineResponseChangeLastPrice();
        MockitoAnnotations.initMocks(this);
    }

    private void placeDefaultLimitOrders() {
        for (Order order : makeDefaultLimitOrders()) {
            getOrderBook().place(order);
        }
    }

    @Test
    public void fillMarketBuyOrderTest() throws InterruptedException {
        placeDefaultLimitOrders();

        for (Order marketOrder : asList(
            marketOrder(1L, OrderSide.BID, 100, 99),
            marketOrder(2L, OrderSide.BID, 100, 90),
            marketOrder(3L, OrderSide.BID, 100, 190)
        )) {
            putToMarketQueue(marketOrder);
        }

        val currentOrder = getMarketQueue().take();
        assertThat(currentOrder.amountToSpend, equalTo(decimal(99)));
        assertThat(getMarketQueue(), hasSize(2));
        engine.processingOrder(currentOrder);
        assertThat(currentOrder.getCountOfTrade(), equalTo((short) 1));
        assertThat(currentOrder.getStatus(), equalTo(COMPLETED));
        assertThat(currentOrder.getAmountToSpend(), lessThan(BigDecimal.ZERO));
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(8));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(6));
        assertThat(getOrderBook().getAsksSortedSet().first().getPrice(), equalTo(decimal(103)));
        assertThat(getOrderBook().getAsksSortedSet().first().getVolume(), lessThan(decimal(100)));

        val previousOrder = getMarketQueue().take();
        assertThat(getMarketQueue(), hasSize(1));
        engine.processingOrder(previousOrder);
        assertThat(getOrderBook().getAsksSortedSet().first().getPrice(), equalTo(decimal(103)));
        assertThat(getOrderBook().getAsksSortedSet().first().getVolume(), lessThan(decimal(99)));

        val previousOrder2 = getMarketQueue().take();
        assertThat(getMarketQueue(), hasSize(0));
        engine.processingOrder(previousOrder2);
        assertThat(getOrderBook().getAsksSortedSet().first().getPrice(), equalTo(decimal(103)));
        assertThat(getOrderBook().getAsksSortedSet().first().getVolume(), lessThan(decimal(97)));

        getMarketQueue().put(marketOrder(4L, OrderSide.BID, 200, 10900));
        val anotherCurrentOrder = getMarketQueue().take();
        assertThat(getMarketQueue(), hasSize(0));
        engine.processingOrder(anotherCurrentOrder);
        assertThat(anotherCurrentOrder.getCountOfTrade(), equalTo((short) 2));
        assertThat(anotherCurrentOrder.getStatus(), equalTo(COMPLETED));
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(7));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(6));
        assertThat(getOrderBook().getAsksSortedSet().first().getPrice(), equalTo(decimal(104)));
        assertThat(getOrderBook().getAsksSortedSet().first().getVolume(), lessThan(decimal(91)));

        getMarketQueue().put(marketOrder(5L, OrderSide.BID, 200, 109000));
        val anotherCurrentOrder2 = getMarketQueue().take();
        assertThat(getMarketQueue(), hasSize(0));
        engine.processingOrder(anotherCurrentOrder2);
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(5));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(6));
        assertThat(getOrderBook().getAsksSortedSet().first().getPrice(), equalTo(decimal(106)));
        assertThat(getOrderBook().getAsksSortedSet().first().getVolume(), lessThan(decimal(111)));

        getMarketQueue().put(marketOrder(6L, OrderSide.BID, 9009000, 9009000));
        val anotherCurrentOrder3 = getMarketQueue().take();
        assertThat(getMarketQueue(), hasSize(0));
        engine.processingOrder(anotherCurrentOrder3);
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(0));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(6));
        assertThat(anotherCurrentOrder3.getVolume(), lessThan(decimal(9008510)));
        assertThat(anotherCurrentOrder3.getCountOfTrade(), equalTo((short) 5));
        assertThat(anotherCurrentOrder3.getStatus(), equalTo(COMPLETED));
        assertThat(anotherCurrentOrder3.getAmountToSpend(), lessThan(decimal(8956899)));
    }

    private OrderBook getOrderBook() {
        return engine.getOrderBook();
    }

    private static BigDecimal decimal(int num) {
        return new BigDecimal(num);
    }

    private void putToMarketQueue(Order e) {
        try {
            getMarketQueue().put(e);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }

    private static Order limitOrder(long id, OrderSide side, int volume, int price) {
        return Order.limit(id, side, decimal(volume), decimal(price));
    }

    private static Order marketOrder(long id, OrderSide side, int volume, int amount) {
        return Order.market(id, side, decimal(volume), decimal(amount));
    }

    @Test
    public void fillMarketSellOrderTest() throws InterruptedException {
        placeDefaultLimitOrders();

        for (Order orderMarket : asList(
            marketOrder(1L, ASK, 200, 0),
            marketOrder(2L, ASK, 10, 0),
            marketOrder(3L, ASK, 1, 0))
        ) {
            getMarketQueue().put(orderMarket);
        }

        val currentOrder = getMarketQueue().take();
        assertThat(getMarketQueue(), hasSize(2));
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(8));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(6));
        engine.processingOrder(currentOrder);
        assertThat(currentOrder.getCountOfTrade(), equalTo((short)2));
        assertThat(currentOrder.getStatus(), equalTo(COMPLETED));
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(8));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(4));
        assertThat(getOrderBook().getBidsSortedSet().first().getVolume(), equalTo(decimal(100)));
        assertThat(getOrderBook().getBidsSortedSet().first().getPrice(), equalTo(decimal(106)));

        val previousOrder = getMarketQueue().take();
        assertThat(getMarketQueue(), hasSize(1));
        engine.processingOrder(previousOrder);
        assertThat(previousOrder.getCountOfTrade(), equalTo((short) 1));
        assertThat(previousOrder.getStatus(), equalTo(COMPLETED));
        Order secondBidOrder = getSecondOrderBookBidOrder();
        assertThat(secondBidOrder.getVolume(), equalTo(decimal(90)));
        assertThat(secondBidOrder.getPrice(), equalTo(decimal(106)));

        val previousOrder2 = getMarketQueue().take();
        assertThat(getMarketQueue(), hasSize(0));
        engine.processingOrder(previousOrder2);
        assertThat(getOrderBook().getBidsSortedSet().first().getVolume(), equalTo(decimal(99)));
        assertThat(getOrderBook().getBidsSortedSet().first().getPrice(), equalTo(decimal(106)));

        val newMarketOrder = marketOrder(4L, ASK, 600, 0);
        getMarketQueue().put(newMarketOrder);

        val anotherCurrentOrder = getMarketQueue().take();
        assertThat(getMarketQueue(), hasSize(0));
        engine.processingOrder(anotherCurrentOrder);
        assertThat(anotherCurrentOrder.getCountOfTrade(), equalTo((short) 4));
        assertThat(anotherCurrentOrder.getStatus(), equalTo(COMPLETED));
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(8));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(0));
        assertThat(anotherCurrentOrder.getVolume(), equalTo(decimal(211)));
        assertThat(anotherCurrentOrder.getAmountToSpend(), equalTo(BigDecimal.ZERO));
    }


    @Test
    public void fillLimitSellOrderTest() throws InterruptedException {
        placeDefaultLimitOrders();

        getMarketQueue().clear();
        Order orderMarket = limitOrder(1L, ASK, 10, 105);
        getMarketQueue().add(orderMarket);
        orderMarket = limitOrder(2L, ASK, 100, 200);
        getMarketQueue().add(orderMarket);
        orderMarket = limitOrder(3L, ASK, 1, 90);
        getMarketQueue().add(orderMarket);
        Order currentOrder = getMarketQueue().take();
        engine.processingOrder(currentOrder);
        assertThat(getSecondOrderBookBidOrder().getPrice(), equalTo(decimal(107)));
        assertThat(getSecondOrderBookBidOrder().getVolume(), equalTo(decimal(90)));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(6));
        assertThat(getOrderBook().getAsksSortedSet().first().getPrice(), equalTo(decimal(103)));
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(8));
        currentOrder = getMarketQueue().take();
        engine.processingOrder(currentOrder);
        assertThat(getOrderBook().getAsksSortedSet().last().getPrice(), equalTo(decimal(200)));
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(9));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(6));
        currentOrder = getMarketQueue().take();
        engine.processingOrder(currentOrder);
        assertThat(getOrderBook().getBidsSortedSet().first().getPrice(), equalTo(decimal(107)));
        assertThat(getOrderBook().getBidsSortedSet().first().getVolume(), equalTo(decimal(99)));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(6));
        assertThat(getOrderBook().getAsksSortedSet().first().getPrice(), equalTo(decimal(103)));
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(9));
        orderMarket = limitOrder(111L, ASK, 1000, 105);
        getMarketQueue().add(orderMarket);
        currentOrder = getMarketQueue().take();
        engine.processingOrder(currentOrder);
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(10));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(2));
        assertThat(currentOrder.getPrice(), equalTo(decimal(105)));
        assertThat(currentOrder.getStatus(), equalTo(OrderStatus.OPENED));
        assertThat(currentOrder.getVolume(), equalTo(decimal(611)));
    }

    private Order getSecondOrderBookBidOrder() {
        return new ArrayList<Order>(getOrderBook().getBidsSortedSet()).get(1);
    }


    @Test
    public void fillLimitBuyOrderTest() throws InterruptedException {
        placeDefaultLimitOrders();

        getMarketQueue().clear();
        Order orderMarket = limitOrder(1L, OrderSide.BID, 10, 105);

        getMarketQueue().add(orderMarket);
        orderMarket = limitOrder(2L, OrderSide.BID, 100, 100);

        getMarketQueue().add(orderMarket);
        orderMarket = limitOrder(3L, OrderSide.BID, 1, 19000);

        getMarketQueue().add(orderMarket);
        Order cuuretnOrder = getMarketQueue().take();
        engine.processingOrder(cuuretnOrder);
        assertThat(cuuretnOrder.getVolume(), equalTo(new BigDecimal(BigInteger.ZERO)));
        assertThat(cuuretnOrder.getStatus(), equalTo(COMPLETED));
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(8));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(6));
        assertThat(getOrderBook().getBidsSortedSet().first().getPrice(), equalTo(decimal(107)));
        assertThat(getOrderBook().getAsksSortedSet().first().getPrice(), equalTo(decimal(103)));
        assertThat(getOrderBook().getAsksSortedSet().first().getVolume(), equalTo(decimal(90)));
        cuuretnOrder = getMarketQueue().take();
        engine.processingOrder(cuuretnOrder);
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(8));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(7));
        assertThat(getOrderBook().getBidsSortedSet().first().getPrice(), equalTo(decimal(107)));
        assertThat(getOrderBook().getAsksSortedSet().first().getPrice(), equalTo(decimal(103)));
        assertThat(getOrderBook().getAsksSortedSet().first().getVolume(), equalTo(decimal(90)));
        cuuretnOrder = getMarketQueue().take();
        engine.processingOrder(cuuretnOrder);
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(8));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(7));
        assertThat(getOrderBook().getBidsSortedSet().first().getPrice(), equalTo(decimal(107)));
        assertThat(getOrderBook().getAsksSortedSet().first().getPrice(), equalTo(decimal(103)));
        assertThat(getOrderBook().getAsksSortedSet().first().getVolume(), equalTo(decimal(89)));
        orderMarket = limitOrder(1L, OrderSide.BID, 1000, 105);
        getMarketQueue().add(orderMarket);
        cuuretnOrder = getMarketQueue().take();
        engine.processingOrder(cuuretnOrder);
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(5));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(8));
        assertThat(cuuretnOrder.getPrice(), equalTo(decimal(105)));
        assertThat(cuuretnOrder.getStatus(), equalTo(OrderStatus.OPENED));
        assertThat(cuuretnOrder.getVolume(), equalTo(decimal(711)));
    }

    @Test
    public void fillLimitASKToLimitBIDTest() throws InterruptedException {
        placeDefaultLimitOrders();

        getOrderBook().getAsksSortedSet().clear();
        getOrderBook().getBidsSortedSet().clear();
        getMarketQueue().clear();
        Order orderMarket = Order.limit(1L, ASK, new BigDecimal(BigInteger.ONE), decimal(3));

        getMarketQueue().add(orderMarket);
        Order cuuretnOrder = getMarketQueue().take();
        assertThat(cuuretnOrder.getVolume(), equalTo(new BigDecimal(BigInteger.ONE)));
        engine.processingOrder(cuuretnOrder);
        assertThat(cuuretnOrder.getVolume(), equalTo(new BigDecimal(BigInteger.ONE)));
        orderMarket = Order.limit(2L, OrderSide.BID, new BigDecimal(BigInteger.ONE), decimal(3));
        getMarketQueue().add(orderMarket);
        cuuretnOrder = getMarketQueue().take();
        assertThat(cuuretnOrder.getVolume(), equalTo(new BigDecimal(BigInteger.ONE)));
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(1));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(0));
        engine.processingOrder(cuuretnOrder);
        assertThat(cuuretnOrder.getVolume(), equalTo(new BigDecimal(BigInteger.ZERO)));
        assertThat(cuuretnOrder.getStatus(), equalTo(COMPLETED));
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(0));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(0));
    }

    @Test
    public void fillLimitBIDToLimitASKTest() throws InterruptedException {
        placeDefaultLimitOrders();

        Order orderMarket = Order.limit(1L, OrderSide.BID, new BigDecimal(BigInteger.ONE), decimal(3));

        getMarketQueue().add(orderMarket);
        Order cuuretnOrder = getMarketQueue().take();
        assertThat(cuuretnOrder.getVolume(), equalTo(new BigDecimal(BigInteger.ONE)));
        engine.processingOrder(cuuretnOrder);
        assertThat(cuuretnOrder.getVolume(), equalTo(new BigDecimal(BigInteger.ONE)));
        orderMarket = Order.limit(2L, ASK, new BigDecimal(BigInteger.ONE), decimal(3));

        getMarketQueue().add(orderMarket);
        cuuretnOrder = getMarketQueue().take();
        assertThat(cuuretnOrder.getVolume(), equalTo(new BigDecimal(BigInteger.ONE)));
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(8));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(7));
        engine.processingOrder(cuuretnOrder);
        assertThat(cuuretnOrder.getVolume(), equalTo(new BigDecimal(BigInteger.ZERO)));
        assertThat(cuuretnOrder.getStatus(), equalTo(COMPLETED));
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(8));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(7));
        assertThat(getSecondOrderBookBidOrder().getVolume(), equalTo(decimal(99)));
        assertThat(getSecondOrderBookBidOrder().getPrice(), equalTo(decimal(107)));
    }

    @Test
    public void fillStopLimitBIDToLimitASKTest() throws InterruptedException {
        placeDefaultLimitOrders();

        getMarketQueue().clear();
        getOrderBook().getStopLimitSortedSet().clear();
        Order orderMarket = limitOrder(123L, OrderSide.BID, 10, 105);

        getMarketQueue().add(orderMarket);
        Order cuuretnOrder = getMarketQueue().take();
        engine.processingOrder(cuuretnOrder);

        orderMarket = Order.stopLimit(14L, OrderSide.BID, decimal(100), decimal(1000), decimal(105));
        getMarketQueue().add(orderMarket);
        cuuretnOrder = getMarketQueue().take();
        assertThat(cuuretnOrder.getVolume(), equalTo(decimal(1000)));
        engine.processingOrder(cuuretnOrder);
        assertThat(getOrderBook().getStopLimitSortedSet(), hasSize(1));
        getOrderBook().getAsksSortedSet().clear();
        getOrderBook().getBidsSortedSet().clear();
        orderMarket = limitOrder(1L, ASK, 10, 104);
        getMarketQueue().add(orderMarket);
        orderMarket = limitOrder(2L, ASK, 10, 100);
        getMarketQueue().add(orderMarket);
        orderMarket = limitOrder(3L, OrderSide.BID, 10, 100);
        getMarketQueue().add(orderMarket);
        cuuretnOrder = getMarketQueue().take();
        engine.processingOrder(cuuretnOrder);
        cuuretnOrder = getMarketQueue().take();
        engine.processingOrder(cuuretnOrder);
        cuuretnOrder = getMarketQueue().take();
        engine.processingOrder(cuuretnOrder);
        assertThat(getOrderBook().getStopLimitSortedSet(), hasSize(0));
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(1));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(0));
        cuuretnOrder = getMarketQueue().take();
        engine.processingOrder(cuuretnOrder);
        assertThat(cuuretnOrder.getPrice(), equalTo(decimal(105)));
        assertThat(cuuretnOrder.getStatus(), equalTo(OrderStatus.OPENED));
        assertThat(cuuretnOrder.getVolume(), equalTo(decimal(990)));
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(0));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(1));
    }


    @Test
    public void fillStopLimitASKToLimitBIDTest() throws InterruptedException {
        placeDefaultLimitOrders();

        getMarketQueue().clear();
        getOrderBook().getStopLimitSortedSet().clear();
        Order orderMarket = limitOrder(123L, ASK, 10, 105);

        getMarketQueue().add(orderMarket);
        Order cuuretnOrder = getMarketQueue().take();
        engine.processingOrder(cuuretnOrder);

        orderMarket = Order.stopLimit(14L, ASK, decimal(108), decimal(1000), decimal(105));
        getMarketQueue().add(orderMarket);
        cuuretnOrder = getMarketQueue().take();
        assertThat(cuuretnOrder.getVolume(), equalTo(decimal(1000)));
        engine.processingOrder(cuuretnOrder);
        assertThat(getOrderBook().getStopLimitSortedSet(), hasSize(1));
        getOrderBook().getAsksSortedSet().clear();
        getOrderBook().getBidsSortedSet().clear();
        orderMarket = limitOrder(1L, OrderSide.BID, 99, 106);
        getMarketQueue().add(orderMarket);
        orderMarket = limitOrder(2L, OrderSide.BID, 10, 108);
        getMarketQueue().add(orderMarket);
        orderMarket = limitOrder(3L, ASK, 10, 108);
        getMarketQueue().add(orderMarket);
        cuuretnOrder = getMarketQueue().take();
        engine.processingOrder(cuuretnOrder);
        cuuretnOrder = getMarketQueue().take();
        engine.processingOrder(cuuretnOrder);
        cuuretnOrder = getMarketQueue().take();
        engine.processingOrder(cuuretnOrder);
        assertThat(getOrderBook().getStopLimitSortedSet(), hasSize(0));
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(0));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(1));
        cuuretnOrder = getMarketQueue().take();
        engine.processingOrder(cuuretnOrder);
        assertThat(cuuretnOrder.getPrice(), equalTo(decimal(105)));
        assertThat(cuuretnOrder.getStatus(), equalTo(OrderStatus.OPENED));
        assertThat(cuuretnOrder.getVolume(), equalTo(decimal(901)));
        assertThat(getOrderBook().getAsksSortedSet(), hasSize(1));
        assertThat(getOrderBook().getBidsSortedSet(), hasSize(0));
    }

    private BlockingQueue<Order> getMarketQueue() {
        return engine.getMarketQueue();
    }

    @After
    public void stop() {
        getMarketQueue().clear();
        getOrderBook().getAsksSortedSet().clear();
        getOrderBook().getBidsSortedSet().clear();
    }
}
