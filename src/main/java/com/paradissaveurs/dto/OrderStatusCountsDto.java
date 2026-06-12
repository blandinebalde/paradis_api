package com.paradissaveurs.dto;

public record OrderStatusCountsDto(
        long pending,
        long confirmed,
        long delivered,
        long cancelled,
        long all
) {}
