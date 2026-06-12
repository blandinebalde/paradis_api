package com.paradissaveurs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Indiquez votre mot de passe actuel")
        String currentPassword,
        @NotBlank(message = "Indiquez le nouveau mot de passe")
        @Size(min = 8, message = "Le mot de passe doit faire au moins 8 caractères")
        String newPassword
) {}
