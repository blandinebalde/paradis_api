package com.paradissaveurs.dto;

import java.time.Instant;

public record ProductPromotionDto(
        String id,
        String name,
        String scope,
        String productId,
        String category,
        String discountType,
        Integer discountValue,
        Instant startDate,
        Instant endDate,
        boolean active,
        Integer previewOriginalPrice,
        Integer previewPromoPrice
) {}
