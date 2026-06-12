package com.paradissaveurs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Indiquez le nom de la catégorie")
        @Size(max = 80, message = "Le nom ne peut pas dépasser 80 caractères")
        String name,
        @Size(max = 16, message = "Emoji invalide")
        String emoji,
        Integer sortOrder,
        Boolean active
) {}
