package com.paradissaveurs.dto;

import java.util.List;

public record OrderPageDto(
        List<OrderDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        OrderStatusCountsDto counts
) {}
