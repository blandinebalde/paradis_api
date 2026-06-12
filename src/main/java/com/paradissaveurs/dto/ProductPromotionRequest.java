package com.paradissaveurs.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ProductPromotionRequest(
        String name,
        @NotBlank(message = "Indiquez la portée (produit ou catégorie)")
        String scope,
        String productId,
        String category,
        @NotBlank(message = "Indiquez le type de réduction")
        String discountType,
        @NotNull(message = "Indiquez la valeur de la réduction")
        @Min(value = 1, message = "La valeur de réduction doit être positive")
        Integer discountValue,
        Instant startDate,
        Instant endDate,
        Boolean active
) {}
