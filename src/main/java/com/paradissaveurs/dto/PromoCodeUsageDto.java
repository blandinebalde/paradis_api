package com.paradissaveurs.dto;

import java.time.Instant;

public record PromoCodeUsageDto(
        String id,
        String orderId,
        String customerPhone,
        Instant usedAt
) {}
