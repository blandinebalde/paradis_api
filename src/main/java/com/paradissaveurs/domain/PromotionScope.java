package com.paradissaveurs.domain;

public final class PromotionScope {
    public static final String PRODUCT = "product";
    public static final String CATEGORY = "category";

    private PromotionScope() {}

    public static boolean isValid(String scope) {
        return PRODUCT.equals(scope) || CATEGORY.equals(scope);
    }
}
