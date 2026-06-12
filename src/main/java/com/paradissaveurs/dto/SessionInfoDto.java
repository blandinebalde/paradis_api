package com.paradissaveurs.dto;

import java.time.Instant;
import java.util.List;

public record SessionInfoDto(
        String username,
        Instant expiresAt,
        long expiresInSeconds,
        boolean active,
        List<String> permissions
) {}
