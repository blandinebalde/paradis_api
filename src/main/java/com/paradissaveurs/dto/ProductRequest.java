package com.paradissaveurs.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank(message = "Indiquez le nom du produit")
        @Size(max = 120, message = "Le nom ne peut pas dépasser 120 caractères")
        String name,
        @NotBlank(message = "Choisissez une catégorie")
        String category,
        @NotNull(message = "Indiquez le prix du produit")
        @Min(value = 1, message = "Le prix doit être supérieur à 0 FCFA")
        Integer price,
        @NotNull(message = "Indiquez le stock disponible")
        @Min(value = 0, message = "Le stock ne peut pas être négatif")
        Integer stock,
        String emoji,
        @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
        String description,
        String imageUrl,
        Boolean active,
        Boolean featured
) {}
