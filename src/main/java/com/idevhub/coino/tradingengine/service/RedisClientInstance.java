package com.idevhub.coino.tradingengine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idevhub.coino.tradingengine.config.RedisProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.stereotype.Component;

@Component
public class RedisClientInstance {

    private final RedissonClient redisson;
    private final RedisProperties redisProperties;
    private final ObjectMapper mapper;

    public RedisClientInstance(RedisProperties redisProperties, ObjectMapper mapper) {
        this.redisProperties = redisProperties;
        Config rconfig = new Config();
        rconfig.useSingleServer().setAddress(redisProperties.getHost() + ":" + redisProperties.getPort());
        rconfig.setNettyThreads(4);
        rconfig.setThreads(4);
        rconfig.setCodec(new JsonJacksonCodec(mapper));
        this.redisson = Redisson.create(rconfig);
        this.mapper = mapper;
    }

    public RedissonClient getRedisson() {
        return redisson;
    }
}
