package com.paradissaveurs.dto;

public record PromoValidateResponse(
        boolean valid,
        String message,
        String code,
        String discountType,
        Integer discountAmount,
        Integer subtotalBeforeDiscount,
        Integer subtotal,
        Integer deliveryFee,
        Integer total,
        boolean freeDelivery
) {}
