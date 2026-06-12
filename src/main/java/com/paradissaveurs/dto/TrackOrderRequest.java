package com.paradissaveurs.dto;

import jakarta.validation.constraints.NotBlank;

public record TrackOrderRequest(
        @NotBlank(message = "Indiquez le numéro de commande")
        String orderId,
        @NotBlank(message = "Indiquez votre numéro de téléphone")
        String phone
) {}
