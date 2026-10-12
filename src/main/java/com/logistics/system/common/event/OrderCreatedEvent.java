package com.logistics.system.common.event;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        Long customerId,
        List<OrderItem> items
) {
    public record OrderItem(
            String sku,
            String productName,
            int quantity,
            BigDecimal unitPrice
    ) {}
}
