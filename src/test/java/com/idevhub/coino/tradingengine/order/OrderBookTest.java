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
import lombok.extern.log4j.Log4j2;
import lombok.val;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.idevhub.coino.tradingmodel.entity.enumeration.OrderSide.ASK;
import static com.idevhub.coino.tradingmodel.entity.enumeration.OrderSide.BID;
import static com.idevhub.coino.tradingmodel.entity.enumeration.Ticker.BTC;
import static com.idevhub.coino.tradingmodel.entity.enumeration.Ticker.LTC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SecurityBeanOverrideConfiguration.class, TradingengineApp.class, EngineTest.EngineTestConfiguration.class})
@Log4j2
public class OrderBookTest {

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

    private OrderBook book;

    private Engine engine;

    private Order[] makeDefaultLimitOrders() {
        return new Order[]{
            limitOrder(1L, ASK, 100, 105),
            limitOrder(2L, ASK, 100, 103),
            limitOrder(3L, ASK, 80, 106),
            limitOrder(4L, ASK, 120, 106),
            limitOrder(5L, ASK, 100, 106),
            limitOrder(6L, ASK, 100, 106).setCreatedDate(Instant.now().plus(365, ChronoUnit.DAYS)),
            limitOrder(7L, ASK, 100, 104),
            limitOrder(8L, ASK, 100, 107),
            limitOrder(1L, BID, 100, 106),
            limitOrder(2L, BID, 100, 104),
            limitOrder(3L, BID, 100, 107),
            limitOrder(4L, BID, 100, 106).setCreatedDate(Instant.now().plus(365, ChronoUnit.DAYS)),
            limitOrder(5L, BID, 120, 104).setCreatedDate(Instant.now().plus(365, ChronoUnit.DAYS)),
            limitOrder(6L, BID, 100, 107),
            limitOrder(7L, BID, 120, 104)
        };
    }

    private void placeDefaultLimitOrders() {
        for (Order order : makeDefaultLimitOrders()) {
            book.place(order);
        }
    }

    private static Order limitOrder(long id, OrderSide side, int volume, int price) {
        return Order.limit(id, side, decimal(volume), decimal(price));
    }

    private static Order marketOrder(long id, OrderSide side, int volume, int amount) {
        return Order.market(id, side, decimal(volume), decimal(amount));
    }

    private static BigDecimal decimal(int num) {
        return new BigDecimal(num);
    }

    @SuppressWarnings("Duplicates")
    @Before
    public void setup() {
        val tradingProperties = new TradingProperties().setBase(BTC).setQuote(LTC);
        book = new OrderBook(orderSortedSetProvider, tradingProperties);
        engine = new Engine(orderBlockingQueueProvider, book, new Notification(spymessageChannels), tradingProperties, tradingEngineSettingManager);

        doReturn(new NullChannel()).when(spymessageChannels).tradingEngineResponseMakeTrade();
        doReturn(new NullChannel()).when(spymessageChannels).tradingengineResponseChangeLastPrice();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCounterPartsForBidsThatLessThanCurrentAskOrders() {
        placeDefaultLimitOrders();

        val counterParts1 = book.getCounterParts(limitOrder(1L, BID, 100, 99));
        assertEquals(counterParts1.size(), 0);

        val counterParts2 = book.getCounterParts(limitOrder(2L, BID, 100, 105));
        assertEquals(counterParts2.size(), 3);
    }


    @Test
    public void testCounterPartsForMarketBidOrderAreSortedByPriceAndVolumeAndDate() {
        placeDefaultLimitOrders();

        val counterParts = new CopyOnWriteArrayList<Order>(
            book.getCounterParts(marketOrder(1L, BID, 100, 99)));

        assertEquals(counterParts.size(), 8);
        assertEquals(counterParts.get(0).getPrice(), decimal(103));
        assertEquals(counterParts.get(1).getPrice(), decimal(104));
        assertEquals(counterParts.get(2).getPrice(), decimal(105));

        assertEquals(counterParts.get(3).getPrice(), decimal(106));
        assertEquals(counterParts.get(3).getVolume(), decimal(120));

        assertEquals(counterParts.get(4).getPrice(), decimal(106));
        assertEquals(counterParts.get(4).getVolume(), decimal(100));

        assertEquals(counterParts.get(5).getPrice(), decimal(106));
        assertEquals(counterParts.get(5).getVolume(), decimal(80));

        assertEquals(counterParts.get(6).getPrice(), decimal(106));
        assertEquals(counterParts.get(6).getVolume(), decimal(100));

        assertEquals(counterParts.get(7).getPrice(), decimal(107));
        assertEquals(counterParts.get(7).getVolume(), decimal(100));
    }


    @Test
    public void testCounterPartsForMarketAskOrderAreSortedByPriceAndVolumeAndDate() {
        placeDefaultLimitOrders();

        val counterParts = new CopyOnWriteArrayList<Order>(book.getCounterParts(marketOrder(1L, ASK, 100, 0)));

        assertEquals(counterParts.size(), 7);
        assertEquals(counterParts.get(0).getPrice(), decimal(107));
        assertEquals(counterParts.get(counterParts.size() - 1).getPrice(), decimal(104));
        assertEquals(counterParts.get(4).getVolume(), decimal(120));
        assertEquals(counterParts.get(5).getVolume(), decimal(100));
        assertEquals(counterParts.get(6).getVolume(), decimal(120));
        assertTrue(counterParts.get(5).getCreatedDate().compareTo(counterParts.get(4).getCreatedDate()) >= 0);
    }


    @Test
    public void testCounterPartsForLimitAskOrderAreSortedByPriceAndVolumeAndDate() {
        placeDefaultLimitOrders();

        val counterParts = new CopyOnWriteArrayList<Order>(book.getCounterParts(limitOrder(1L, ASK, 100, 105)));

        assertEquals(counterParts.size(), 4);
        assertEquals(counterParts.get(0).getPrice(), decimal(107));

        assertTrue(counterParts.get(2).getCreatedDate().compareTo(counterParts.get(3).getCreatedDate()) <= 0);
    }

    @Test
    public void testCounterPartsForLimitBidOrderAreSortedByPriceAndVolumeAndDate() {
        placeDefaultLimitOrders();

        val counterParts = new CopyOnWriteArrayList<Order>(book.getCounterParts(limitOrder(1L, BID, 100, 105)));

        assertEquals(counterParts.size(), 3);
        assertEquals(counterParts.get(0).getPrice(), decimal(103));
        assertTrue(counterParts.get(counterParts.size() - 1).getPrice().compareTo(decimal(106)) == -1);
    }
}
