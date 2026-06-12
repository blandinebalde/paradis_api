package com.paradissaveurs.dto;

import java.time.Instant;
import java.util.List;

public record OrderDto(
        String id,
        CustomerDto customer,
        String deliveryMode,
        String zoneId,
        String paymentMethod,
        Integer deliveryFee,
        Integer subtotalBeforeDiscount,
        Integer discountAmount,
        Integer subtotal,
        Integer total,
        String promoCode,
        String status,
        String notes,
        Instant createdAt,
        Instant deliveredAt,
        List<OrderItemDto> items
) {
    public record CustomerDto(String name, String phone, String address) {}
}
