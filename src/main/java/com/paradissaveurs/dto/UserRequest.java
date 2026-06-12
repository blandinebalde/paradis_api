package com.paradissaveurs.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UserRequest(
        @NotBlank(message = "Indiquez un identifiant")
        String username,
        String displayName,
        String password,
        boolean active,
        boolean allowWeb,
        boolean allowMobile,
        List<String> permissions
) {}
