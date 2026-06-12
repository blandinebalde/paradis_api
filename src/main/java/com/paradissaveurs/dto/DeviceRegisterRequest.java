package com.paradissaveurs.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceRegisterRequest(
        @NotBlank String token,
        String platform
) {}
