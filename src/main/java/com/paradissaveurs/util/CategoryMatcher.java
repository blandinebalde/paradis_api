package com.paradissaveurs.util;

public final class CategoryMatcher {

    private CategoryMatcher() {}

    /** Clé normalisée pour comparer "Fast-food", "fastfood", "Fast food", etc. */
    public static String normalizeKey(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase().replaceAll("[\\s_\\-]+", "");
    }

    public static boolean matches(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) return false;
        return normalizeKey(a).equals(normalizeKey(b));
    }
}
