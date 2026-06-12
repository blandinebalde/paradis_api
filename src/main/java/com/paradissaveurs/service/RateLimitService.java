package com.paradissaveurs.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final class Window {
        int count;
        Instant start = Instant.now();
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public void checkLimit(String key, int maxRequests, Duration windowSize) {
        Window window = windows.computeIfAbsent(key, k -> new Window());
        synchronized (window) {
            Instant now = Instant.now();
            if (Duration.between(window.start, now).compareTo(windowSize) >= 0) {
                window.count = 0;
                window.start = now;
            }
            window.count++;
            if (window.count > maxRequests) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Trop de tentatives — réessayez dans quelques instants");
            }
        }
    }
}
