package com.paradissaveurs.domain;

import java.util.Map;
import java.util.Set;

public final class OrderStatus {

    public static final String PENDING = "pending";
    public static final String CONFIRMED = "confirmed";
    public static final String DELIVERED = "delivered";
    public static final String CANCELLED = "cancelled";

    private static final Map<String, Set<String>> ALLOWED = Map.of(
            PENDING, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(DELIVERED, CANCELLED),
            DELIVERED, Set.of(),
            CANCELLED, Set.of()
    );

    private static final Map<String, String> LABELS = Map.of(
            PENDING, "En attente",
            CONFIRMED, "Confirmée",
            DELIVERED, "Livrée",
            CANCELLED, "Annulée"
    );

    private OrderStatus() {}

    public static boolean isValid(String status) {
        return ALLOWED.containsKey(status);
    }

    public static boolean isFinal(String status) {
        return DELIVERED.equals(status) || CANCELLED.equals(status);
    }

    public static boolean canTransition(String from, String to) {
        if (from == null || to == null || from.equals(to)) return false;
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static String label(String status) {
        return LABELS.getOrDefault(status, status);
    }
}
