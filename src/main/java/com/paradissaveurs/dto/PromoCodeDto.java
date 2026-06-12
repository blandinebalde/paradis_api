package com.paradissaveurs.dto;

import java.time.Instant;
import java.util.List;

public record PromoCodeDto(
        String id,
        String code,
        String discountType,
        Integer discountValue,
        Integer minOrderAmount,
        List<String> eligibleProductIds,
        List<String> eligibleCategories,
        Instant startDate,
        Instant endDate,
        Integer maxUsesTotal,
        Integer maxUsesPerPhone,
        int usageCount,
        boolean active
) {}
