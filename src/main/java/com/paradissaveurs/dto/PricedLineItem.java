package com.paradissaveurs.dto;

import com.paradissaveurs.entity.ProductEntity;

public record PricedLineItem(
        ProductEntity product,
        int quantity,
        int catalogPrice,
        int effectivePrice,
        boolean hasProductPromo
) {
    public int lineTotal() {
        return effectivePrice * quantity;
    }
}
