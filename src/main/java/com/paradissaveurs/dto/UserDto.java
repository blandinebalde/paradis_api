package com.paradissaveurs.dto;

import java.time.Instant;
import java.util.List;

public record UserDto(
        Long id,
        String username,
        String displayName,
        boolean active,
        boolean allowWeb,
        boolean allowMobile,
        List<String> permissions,
        Instant createdAt,
        Instant updatedAt
) {}
