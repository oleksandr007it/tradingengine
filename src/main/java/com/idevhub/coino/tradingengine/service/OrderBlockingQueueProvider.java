package com.idevhub.coino.tradingengine.service;

import com.idevhub.coino.tradingmodel.entity.Order;

import java.util.concurrent.BlockingQueue;

public interface OrderBlockingQueueProvider {
    BlockingQueue<Order> getBlockingQueue(String queueKey);
}
