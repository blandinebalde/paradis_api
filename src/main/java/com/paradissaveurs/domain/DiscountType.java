package com.paradissaveurs.domain;

public final class DiscountType {
    public static final String PERCENTAGE = "percentage";
    public static final String FIXED = "fixed";

    private DiscountType() {}

    public static boolean isValid(String type) {
        return PERCENTAGE.equals(type) || FIXED.equals(type);
    }
}
