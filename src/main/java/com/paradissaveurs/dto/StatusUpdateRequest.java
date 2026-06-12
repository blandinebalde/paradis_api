package com.paradissaveurs.dto;

import jakarta.validation.constraints.NotBlank;

public record StatusUpdateRequest(
        @NotBlank(message = "Choisissez un statut pour la commande")
        String status
) {}
