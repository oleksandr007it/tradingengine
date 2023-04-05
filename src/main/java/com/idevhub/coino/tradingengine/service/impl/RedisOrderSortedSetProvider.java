package com.idevhub.coino.tradingengine.service.impl;

import com.idevhub.coino.tradingengine.service.OrderSortedSetProvider;
import com.idevhub.coino.tradingengine.service.RedisClientInstance;
import com.idevhub.coino.tradingmodel.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.SortedSet;

@Service
@RequiredArgsConstructor
public class RedisOrderSortedSetProvider implements OrderSortedSetProvider {
    private final RedisClientInstance redisClientInstance;

    @Override
    public SortedSet<Order> getSortedSet(String sortedSetKey) {
        return redisClientInstance.getRedisson().getSortedSet(sortedSetKey);
    }
}
