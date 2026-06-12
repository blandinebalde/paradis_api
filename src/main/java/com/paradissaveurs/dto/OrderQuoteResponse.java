package com.paradissaveurs.dto;

public record OrderQuoteResponse(
        int originalSubtotal,
        int productPromoSavings,
        int subtotalBeforePromoCode,
        int promoCodeDiscount,
        int subtotal,
        int deliveryFee,
        int total,
        boolean hasProductPromo,
        boolean freeDelivery,
        String promoCode
) {}
