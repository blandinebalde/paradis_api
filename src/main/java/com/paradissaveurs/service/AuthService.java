package com.paradissaveurs.service;

import com.paradissaveurs.dto.LoginRequest;
import com.paradissaveurs.dto.LoginResponse;
import com.paradissaveurs.dto.SessionInfoDto;
import com.paradissaveurs.entity.AdminUserEntity;
import com.paradissaveurs.repository.AdminUserRepository;
import com.paradissaveurs.security.AuthHelper;
import com.paradissaveurs.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenSessionService tokenSessionService;
    private final AuditService auditService;

    public AuthService(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, TokenSessionService tokenSessionService,
                       AuditService auditService) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenSessionService = tokenSessionService;
        this.auditService = auditService;
    }

    public LoginResponse login(LoginRequest request) {
        String platform = request.platform() != null ? request.platform() : "web";
        var userOpt = adminUserRepository.findByUsername(request.username());

        if (userOpt.isEmpty()) {
            auditService.logLogin(null, platform, false, "Identifiant inconnu : " + request.username());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants incorrects");
        }

        var user = userOpt.get();

        if (!user.isActive()) {
            auditService.logLogin(user, platform, false, "Compte désactivé");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Compte désactivé — contactez un administrateur");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            auditService.logLogin(user, platform, false, "Mot de passe incorrect");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants incorrects");
        }

        if (AuthHelper.isMobilePlatform(platform) && !user.isAllowMobile()) {
            auditService.logLogin(user, platform, false, "Accès mobile non autorisé");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Votre compte n'est pas autorisé à se connecter sur l'application mobile");
        }

        if (AuthHelper.isWebPlatform(platform) && !user.isAllowWeb()) {
            auditService.logLogin(user, platform, false, "Accès web non autorisé");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Votre compte n'est pas autorisé à se connecter sur le web");
        }

        var issued = tokenSessionService.issue(user, platform);
        auditService.logLogin(user, platform, true, "Connexion réussie");

        var perms = user.permissionSet().stream().map(Enum::name).sorted().collect(Collectors.toList());
        long expiresInSeconds = Duration.between(Instant.now(), issued.expiresAt()).getSeconds();
        return new LoginResponse(
                issued.token(),
                user.getUsername(),
                user.getId(),
                user.getDisplayName(),
                user.isAllowWeb(),
                user.isAllowMobile(),
                perms,
                issued.expiresAt(),
                Math.max(expiresInSeconds, 0)
        );
    }

    public void logout(Authentication auth) {
        String jti = auth.getCredentials() != null ? auth.getCredentials().toString() : null;
        if (jti != null && tokenSessionService.revoke(jti)) {
            auditService.log("LOGOUT", "SESSION", jti, "Déconnexion");
        }
    }

    public SessionInfoDto sessionInfo(Authentication auth) {
        String jti = auth.getCredentials() != null ? auth.getCredentials().toString() : null;
        boolean active = jti != null && tokenSessionService.isActive(jti);
        Instant expiresAt = active ? tokenSessionService.expiresAt(jti) : null;
        long expiresInSeconds = 0;
        if (expiresAt != null) {
            expiresInSeconds = Math.max(Duration.between(Instant.now(), expiresAt).getSeconds(), 0);
        }
        var user = requireUser(auth.getName());
        var perms = user.permissionSet().stream().map(Enum::name).sorted().collect(Collectors.toList());
        return new SessionInfoDto(auth.getName(), expiresAt, expiresInSeconds, active, perms);
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        var user = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Mot de passe actuel incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le nouveau mot de passe doit être différent de l'actuel");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        adminUserRepository.save(user);
        tokenSessionService.revokeAllForUser(user.getId());
        auditService.log("PASSWORD_CHANGE", "USER", user.getId().toString(),
                "Mot de passe modifié — toutes les sessions révoquées");
    }

    public AdminUserEntity requireUser(String username) {
        return adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session invalide"));
    }
}
