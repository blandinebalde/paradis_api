package com.paradissaveurs.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderItemDto(
        @NotBlank(message = "Un article du panier est invalide")
        String productId,
        String name,
        String emoji,
        String imageUrl,
        Integer originalPrice,
        Integer price,
        @NotNull(message = "Indiquez la quantité pour chaque article")
        @Min(value = 1, message = "La quantité doit être d’au moins 1")
        Integer quantity
) {}
