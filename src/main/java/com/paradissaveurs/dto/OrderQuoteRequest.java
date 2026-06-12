package com.paradissaveurs.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrderQuoteRequest(
        @NotEmpty(message = "Votre panier est vide")
        List<@Valid CartItemRequest> items,
        String phone,
        @NotBlank(message = "Choisissez un mode de récupération")
        String deliveryMode,
        String zoneId,
        String promoCode
) {
    public record CartItemRequest(
            @NotBlank(message = "Produit invalide dans le panier")
            String productId,
            @Min(value = 1, message = "La quantité doit être d’au moins 1")
            int quantity
    ) {}
}
