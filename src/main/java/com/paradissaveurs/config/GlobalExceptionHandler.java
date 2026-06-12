package com.paradissaveurs.config;

import com.paradissaveurs.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<String, String> FIELD_LABELS = new HashMap<>();
    static {
        FIELD_LABELS.put("username", "Identifiant");
        FIELD_LABELS.put("password", "Mot de passe");
        FIELD_LABELS.put("newPassword", "Nouveau mot de passe");
        FIELD_LABELS.put("phone", "Téléphone");
        FIELD_LABELS.put("items", "Panier");
        FIELD_LABELS.put("customer", "Informations client");
        FIELD_LABELS.put("deliveryMode", "Mode de livraison");
        FIELD_LABELS.put("paymentMethod", "Paiement");
        FIELD_LABELS.put("name", "Nom");
        FIELD_LABELS.put("category", "Catégorie");
        FIELD_LABELS.put("promoCode", "Code promo");
        FIELD_LABELS.put("code", "Code promo");
        FIELD_LABELS.put("description", "Description");
        FIELD_LABELS.put("fee", "Frais");
        FIELD_LABELS.put("price", "Prix");
        FIELD_LABELS.put("stock", "Stock");
        FIELD_LABELS.put("status", "Statut");
        FIELD_LABELS.put("productId", "Produit");
        FIELD_LABELS.put("quantity", "Quantité");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        return ResponseEntity.status(ex.getStatusCode()).body(new ApiError(message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> {
                    String label = FIELD_LABELS.getOrDefault(e.getField(), e.getField());
                    String detail = e.getDefaultMessage();
                    if (detail != null && !detail.isBlank()) return detail;
                    return label + " invalide";
                })
                .orElse("Vérifiez les informations saisies");
        return ResponseEntity.badRequest().body(new ApiError(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ApiError("Requête invalide — vérifiez les données envoyées"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("Une erreur est survenue. Réessayez dans un instant."));
    }
}
