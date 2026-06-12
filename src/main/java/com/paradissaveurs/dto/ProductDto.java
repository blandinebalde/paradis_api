package com.paradissaveurs.dto;

public record ProductDto(
        String id,
        String name,
        String category,
        Integer price,
        Integer stock,
        String emoji,
        String description,
        String imageUrl,
        boolean active,
        boolean featured,
        Integer originalPrice,
        boolean onPromo,
        String promoScope
) {}
