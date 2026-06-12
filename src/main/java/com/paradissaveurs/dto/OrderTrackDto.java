package com.paradissaveurs.dto;

import java.time.Instant;
import java.util.List;

/** Réponse publique de suivi — sans PII client (nom, téléphone, adresse, notes). */
public record OrderTrackDto(
        String id,
        String status,
        String deliveryMode,
        String paymentMethod,
        Integer deliveryFee,
        Integer subtotal,
        Integer total,
        Instant createdAt,
        Instant deliveredAt,
        List<OrderItemDto> items
) {}
