package com.paradissaveurs.service;

import com.paradissaveurs.dto.UserDto;
import com.paradissaveurs.dto.UserRequest;
import com.paradissaveurs.entity.AdminUserEntity;
import com.paradissaveurs.repository.AdminUserRepository;
import com.paradissaveurs.security.AppPermission;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final TokenSessionService tokenSessionService;

    public UserService(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder,
                       AuditService auditService, TokenSessionService tokenSessionService) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.tokenSessionService = tokenSessionService;
    }

    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        return adminUserRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UserDto findById(Long id) {
        return toDto(getUser(id));
    }

    @Transactional
    public UserDto create(UserRequest request) {
        if (adminUserRepository.findByUsername(request.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cet identifiant existe déjà");
        }
        if (!StringUtils.hasText(request.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indiquez un mot de passe");
        }
        var user = new AdminUserEntity();
        applyRequest(user, request, true);
        var saved = adminUserRepository.save(user);
        auditService.log("USER_CREATE", "USER", saved.getId().toString(),
                "Création utilisateur " + saved.getUsername());
        return toDto(saved);
    }

    @Transactional
    public UserDto update(Long id, UserRequest request) {
        var user = getUser(id);
        boolean wasActive = user.isActive();
        if (!user.getUsername().equals(request.username())
                && adminUserRepository.findByUsername(request.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cet identifiant existe déjà");
        }
        applyRequest(user, request, false);
        var saved = adminUserRepository.save(user);
        if (wasActive && !saved.isActive()) {
            tokenSessionService.revokeAllForUser(saved.getId());
        }
        auditService.log("USER_UPDATE", "USER", saved.getId().toString(),
                "Modification utilisateur " + saved.getUsername());
        return toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        var user = getUser(id);
        var current = currentUsername();
        if (user.getUsername().equals(current)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous ne pouvez pas supprimer votre propre compte");
        }
        tokenSessionService.revokeAllForUser(user.getId());
        adminUserRepository.delete(user);
        auditService.log("USER_DELETE", "USER", id.toString(), "Suppression utilisateur " + user.getUsername());
    }

    private void applyRequest(AdminUserEntity user, UserRequest request, boolean creating) {
        user.setUsername(request.username().trim());
        user.setDisplayName(StringUtils.hasText(request.displayName()) ? request.displayName().trim() : request.username());
        user.setActive(request.active());
        user.setAllowWeb(request.allowWeb());
        user.setAllowMobile(request.allowMobile());
        user.setPermissionSet(parsePermissions(request.permissions()));
        if (StringUtils.hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            tokenSessionService.revokeAllForUser(user.getId());
        } else if (creating) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indiquez un mot de passe");
        }
        if (!user.isAllowWeb() && !user.isAllowMobile()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Autorisez au moins l'accès Web ou Mobile");
        }
    }

    private Set<AppPermission> parsePermissions(List<String> raw) {
        if (raw == null || raw.isEmpty()) return EnumSet.noneOf(AppPermission.class);
        var set = EnumSet.noneOf(AppPermission.class);
        for (String code : raw) {
            try {
                set.add(AppPermission.valueOf(code));
            } catch (IllegalArgumentException ignored) {
                // skip unknown
            }
        }
        return set;
    }

    private AdminUserEntity getUser(Long id) {
        return adminUserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    UserDto toDto(AdminUserEntity user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.isActive(),
                user.isAllowWeb(),
                user.isAllowMobile(),
                user.permissionSet().stream().map(Enum::name).sorted().collect(Collectors.toList()),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
