package com.paradissaveurs.security;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum AppPermission {
    ORDERS_READ("Voir les commandes"),
    ORDERS_WRITE("Modifier les statuts"),
    PRODUCTS_READ("Voir les produits"),
    PRODUCTS_WRITE("Gérer les produits"),
    ZONES_WRITE("Gérer les zones"),
    SETTINGS_WRITE("Gérer les paramètres"),
    USERS_MANAGE("Gérer les utilisateurs"),
    AUDIT_READ("Consulter l'audit");

    public static final Set<AppPermission> ALL = EnumSet.allOf(AppPermission.class);

    private final String label;

    AppPermission(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String authority() {
        return "PERM_" + name();
    }

    public static Set<AppPermission> parse(String raw) {
        if (raw == null || raw.isBlank()) return EnumSet.noneOf(AppPermission.class);
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return AppPermission.valueOf(s);
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                })
                .filter(p -> p != null)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AppPermission.class)));
    }

    public static String serialize(Set<AppPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) return "";
        return permissions.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }
}
