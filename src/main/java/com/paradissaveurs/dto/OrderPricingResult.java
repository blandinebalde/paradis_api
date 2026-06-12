package com.paradissaveurs.dto;

import java.util.List;

public record OrderPricingResult(
        List<PricedLineItem> lines,
        int subtotalBeforePromoCode,
        int promoCodeDiscount,
        int subtotal,
        int deliveryFee,
        int total,
        String promoCode,
        boolean freeDelivery,
        boolean hasProductPromo
) {}
