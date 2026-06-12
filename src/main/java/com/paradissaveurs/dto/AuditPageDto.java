package com.paradissaveurs.dto;

import java.util.List;

public record AuditPageDto(
        List<AuditLogDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
