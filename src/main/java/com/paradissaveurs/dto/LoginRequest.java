package com.paradissaveurs.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Indiquez votre identifiant")
        String username,
        @NotBlank(message = "Indiquez votre mot de passe")
        String password,
        String platform
) {}
