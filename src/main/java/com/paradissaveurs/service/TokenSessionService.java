package com.paradissaveurs.service;

import com.paradissaveurs.config.AppProperties;
import com.paradissaveurs.dto.TokenIssueResult;
import com.paradissaveurs.entity.AdminSessionEntity;
import com.paradissaveurs.entity.AdminUserEntity;
import com.paradissaveurs.repository.AdminSessionRepository;
import com.paradissaveurs.security.JwtService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class TokenSessionService {

    private final AdminSessionRepository sessionRepository;
    private final JwtService jwtService;
    private final AppProperties appProperties;

    public TokenSessionService(AdminSessionRepository sessionRepository,
                               JwtService jwtService,
                               AppProperties appProperties) {
        this.sessionRepository = sessionRepository;
        this.jwtService = jwtService;
        this.appProperties = appProperties;
    }

    public long expirationMs() {
        return appProperties.getJwt().getExpirationMs();
    }

    @Transactional
    public TokenIssueResult issue(AdminUserEntity user, String platform) {
        String jti = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusMillis(expirationMs());

        var session = new AdminSessionEntity();
        session.setJti(jti);
        session.setUserId(user.getId());
        session.setUsername(user.getUsername());
        session.setPlatform(platform);
        session.setExpiresAt(expiresAt);
        sessionRepository.save(session);

        String token = jwtService.generateToken(user, jti, expiresAt);
        return new TokenIssueResult(token, jti, expiresAt);
    }

    @Transactional(readOnly = true)
    public boolean isActive(String jti) {
        if (jti == null || jti.isBlank()) return false;
        return sessionRepository.findByJti(jti)
                .map(AdminSessionEntity::isActive)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Instant expiresAt(String jti) {
        if (jti == null || jti.isBlank()) return null;
        return sessionRepository.findByJti(jti)
                .filter(AdminSessionEntity::isActive)
                .map(AdminSessionEntity::getExpiresAt)
                .orElse(null);
    }

    @Transactional
    public boolean revoke(String jti) {
        if (jti == null || jti.isBlank()) return false;
        return sessionRepository.revokeByJti(jti, Instant.now()) > 0;
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        if (userId != null) {
            sessionRepository.revokeAllByUserId(userId, Instant.now());
        }
    }

    /** Supprime les sessions expirées ou révoquées depuis plus de 7 jours. */
    @Scheduled(fixedRateString = "${app.jwt.cleanup-interval-ms:3600000}")
    @Transactional
    public void purgeOldSessions() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        sessionRepository.deleteOlderThan(cutoff);
    }
}
