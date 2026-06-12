package com.paradissaveurs.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

public record PromoCodeRequest(
        @NotBlank(message = "Indiquez le code promo")
        String code,
        @NotBlank(message = "Indiquez le type de réduction")
        String discountType,
        @Min(value = 1, message = "La valeur de réduction doit être positive")
        Integer discountValue,
        @Min(value = 0, message = "Le montant minimum ne peut pas être négatif")
        Integer minOrderAmount,
        List<String> eligibleProductIds,
        List<String> eligibleCategories,
        Instant startDate,
        Instant endDate,
        @Min(value = 1, message = "Le nombre d'utilisations doit être au moins 1")
        Integer maxUsesTotal,
        @Min(value = 1, message = "Le nombre d'utilisations par client doit être au moins 1")
        Integer maxUsesPerPhone,
        Boolean active
) {}
