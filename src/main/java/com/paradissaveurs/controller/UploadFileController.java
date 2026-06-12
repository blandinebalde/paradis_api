package com.paradissaveurs.controller;

import com.paradissaveurs.config.AppProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Sert les images uploadées avec type MIME forcé et en-têtes anti-XSS.
 * Seuls les noms générés par le serveur (product-{id}-{hash}.ext) sont acceptés.
 */
@RestController
public class UploadFileController {

    private static final Pattern SAFE_FILENAME = Pattern.compile(
            "^product-\\d+-[a-f0-9]{8}\\.(jpg|jpeg|png|gif|webp)$",
            Pattern.CASE_INSENSITIVE
    );

    private final Path uploadDir;

    public UploadFileController(AppProperties appProperties) {
        this.uploadDir = Paths.get(appProperties.getUploadDir()).toAbsolutePath().normalize();
    }

    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> serve(@PathVariable String filename) {
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.notFound().build();
        }
        if (!SAFE_FILENAME.matcher(filename).matches()) {
            return ResponseEntity.notFound().build();
        }

        Path file = uploadDir.resolve(filename).normalize();
        if (!file.startsWith(uploadDir) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = mediaTypeFor(filename);
        Resource body = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(body);
    }

    private MediaType mediaTypeFor(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }
}
