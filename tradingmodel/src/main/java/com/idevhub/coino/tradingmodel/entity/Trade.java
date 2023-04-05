package com.idevhub.coino.tradingmodel.entity;

import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;


@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Trade implements Serializable {
    private Order order;
    private Order counterPart;
    private BigDecimal volume;
    private BigDecimal price;
    private Instant createdDate;
}
