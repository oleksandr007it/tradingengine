package com.idevhub.coino.tradingengine.messaging;

import org.springframework.cloud.stream.annotation.EnableBinding;

@EnableBinding(value = {MessagingChannels.class})
public class MessagingConfiguration {
}
