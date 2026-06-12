package com.paradissaveurs.dto;

public record PublicPromotionDto(
        String id,
        String name,
        String scope,
        String productId,
        String category,
        String discountType,
        Integer discountValue,
        int affectedCount
) {}
