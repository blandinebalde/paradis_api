package com.paradissaveurs.dto;

import java.time.Instant;

public record AuditLogDto(
        Long id,
        Long userId,
        String username,
        String action,
        String entityType,
        String entityId,
        String details,
        String platform,
        String ipAddress,
        Instant createdAt
) {}
