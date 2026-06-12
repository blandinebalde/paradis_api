package com.paradissaveurs.dto;

public record OrderStatsDto(
        long realizedRev,
        long forecastRev,
        int cancelRate,
        OrderStatusCountsDto counts
) {}
