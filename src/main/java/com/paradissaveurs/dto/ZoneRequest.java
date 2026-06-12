package com.paradissaveurs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ZoneRequest(
        @NotBlank(message = "Indiquez le nom de la zone")
        String name,
        @NotNull(message = "Indiquez les frais de livraison")
        Integer fee
) {}
