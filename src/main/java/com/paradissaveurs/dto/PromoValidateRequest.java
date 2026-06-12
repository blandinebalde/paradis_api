package com.paradissaveurs.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PromoValidateRequest(
        @NotBlank(message = "Indiquez le code promo")
        String code,
        @NotBlank(message = "Indiquez votre numéro de téléphone")
        String phone,
        @NotEmpty(message = "Votre panier est vide")
        List<@Valid CartItemRequest> items,
        String deliveryMode,
        String zoneId
) {
    public record CartItemRequest(
            @NotBlank String productId,
            int quantity
    ) {}
}
