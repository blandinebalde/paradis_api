package com.paradissaveurs.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Normalisation et validation des numéros sénégalais (+221, 9 chiffres nationaux). */
public final class PhoneUtils {

    public static final String INVALID_MESSAGE =
            "Numéro de téléphone sénégalais invalide (ex. 77 123 45 67 ou +221 77 123 45 67)";

    private PhoneUtils() {}

    /** Retourne les 9 chiffres nationaux (sans indicatif 221 ni zéro initial). */
    public static String normalize(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("221") && digits.length() > 9) {
            digits = digits.substring(3);
        }
        while (digits.startsWith("0") && digits.length() > 9) {
            digits = digits.substring(1);
        }
        return digits;
    }

    /** Mobile (7X…) ou fixe (3X…), exactement 9 chiffres après normalisation. */
    public static boolean isValidSenegalese(String phone) {
        String digits = normalize(phone);
        if (digits.length() != 9) return false;
        char first = digits.charAt(0);
        return first == '7' || first == '3';
    }

    public static String requireValidSenegalese(String phone) {
        if (!isValidSenegalese(phone)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_MESSAGE);
        }
        return normalize(phone);
    }
}
