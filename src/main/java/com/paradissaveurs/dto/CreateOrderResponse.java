package com.paradissaveurs.dto;

public record CreateOrderResponse(
        OrderDto order,
        boolean adminNotified,
        String notificationChannel,
        String message
) {}
