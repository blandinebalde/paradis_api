package com.paradissaveurs.dto;

import java.time.Instant;
import java.util.List;

public record LoginResponse(
        String token,
        String username,
        Long userId,
        String displayName,
        boolean allowWeb,
        boolean allowMobile,
        List<String> permissions,
        Instant expiresAt,
        long expiresInSeconds
) {}
