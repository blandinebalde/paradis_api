package com.paradissaveurs.domain;

public final class PromoCodeType {
    public static final String PERCENTAGE = "percentage";
    public static final String FIXED = "fixed";
    public static final String FREE_DELIVERY = "free_delivery";

    private PromoCodeType() {}

    public static boolean isValid(String type) {
        return PERCENTAGE.equals(type) || FIXED.equals(type) || FREE_DELIVERY.equals(type);
    }
}
