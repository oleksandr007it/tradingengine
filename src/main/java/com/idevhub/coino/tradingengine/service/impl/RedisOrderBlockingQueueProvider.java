package com.idevhub.coino.tradingengine.service.impl;

import com.idevhub.coino.tradingengine.service.OrderBlockingQueueProvider;
import com.idevhub.coino.tradingengine.service.RedisClientInstance;
import com.idevhub.coino.tradingmodel.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;

@RequiredArgsConstructor
@Service
public class RedisOrderBlockingQueueProvider implements OrderBlockingQueueProvider {
    private final RedisClientInstance redisClientInstance;

    @Override
    public BlockingQueue<Order> getBlockingQueue(String queueKey) {
        return redisClientInstance.getRedisson().getBlockingQueue(queueKey);
    }
}
