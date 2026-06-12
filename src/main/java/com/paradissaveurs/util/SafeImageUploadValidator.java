package com.paradissaveurs.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

/** Validation MIME, extension et magic bytes pour uploads images produits. */
public final class SafeImageUploadValidator {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private SafeImageUploadValidator() {}

    public static String validateAndExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier requis");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image trop volumineuse (max 5 Mo)");
        }

        String mime = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_MIME.contains(mime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Format non autorisé — utilisez JPEG, PNG, GIF ou WebP");
        }

        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(12);
            String ext = extensionFromMagic(header);
            if (ext == null || !mimeMatchesExtension(mime, ext)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Le contenu du fichier ne correspond pas à une image valide");
            }
            return ext;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier illisible");
        }
    }

    private static boolean mimeMatchesExtension(String mime, String ext) {
        return switch (ext) {
            case ".jpg" -> mime.equals("image/jpeg");
            case ".png" -> mime.equals("image/png");
            case ".gif" -> mime.equals("image/gif");
            case ".webp" -> mime.equals("image/webp");
            default -> false;
        };
    }

    private static String extensionFromMagic(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return ".jpg";
        }
        if (h.length >= 8
                && h[0] == (byte) 0x89 && h[1] == 0x50 && h[2] == 0x4E && h[3] == 0x47
                && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A) {
            return ".png";
        }
        if (h.length >= 6) {
            String sig = new String(h, 0, 6);
            if (sig.startsWith("GIF87a") || sig.startsWith("GIF89a")) {
                return ".gif";
            }
        }
        if (h.length >= 12
                && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return ".webp";
        }
        return null;
    }
}
