package com.paradissaveurs.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "Votre panier est vide — ajoutez au moins un produit")
        List<@Valid OrderItemDto> items,
        @NotNull(message = "Les informations client sont requises")
        @Valid CustomerDto customer,
        @NotBlank(message = "Choisissez un mode de récupération (livraison ou collecte)")
        String deliveryMode,
        String zoneId,
        @NotBlank(message = "Choisissez un mode de paiement")
        String paymentMethod,
        @NotNull(message = "Les frais de livraison sont requis")
        Integer deliveryFee,
        @NotNull(message = "Le sous-total est requis")
        Integer subtotal,
        @NotNull(message = "Le total est requis")
        Integer total,
        String promoCode,
        String notes
) {
    public record CustomerDto(
            String name,
            @NotBlank(message = "Indiquez votre numéro de téléphone")
            String phone,
            String address
    ) {}
}
