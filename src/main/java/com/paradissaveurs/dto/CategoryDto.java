package com.paradissaveurs.dto;

public record CategoryDto(
        String id,
        String name,
        String emoji,
        int sortOrder,
        boolean active,
        long productCount
) {}
