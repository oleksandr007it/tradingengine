package com.idevhub.coino.tradingengine.service;

import com.idevhub.coino.tradingmodel.entity.Order;

import java.util.SortedSet;

public interface OrderSortedSetProvider {
    SortedSet<Order> getSortedSet(String key);
}
