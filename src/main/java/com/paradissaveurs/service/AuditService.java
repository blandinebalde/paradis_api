package com.paradissaveurs.service;

import com.paradissaveurs.entity.AuditLogEntity;
import com.paradissaveurs.entity.AdminUserEntity;
import com.paradissaveurs.repository.AuditLogRepository;
import com.paradissaveurs.repository.AdminUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AdminUserRepository adminUserRepository;

    public AuditService(AuditLogRepository auditLogRepository, AdminUserRepository adminUserRepository) {
        this.auditLogRepository = auditLogRepository;
        this.adminUserRepository = adminUserRepository;
    }

    public void log(String action, String entityType, String entityId, String details) {
        log(action, entityType, entityId, details, currentPlatform());
    }

    public void log(String action, String entityType, String entityId, String details, String platform) {
        var user = currentUser();
        var entry = new AuditLogEntity();
        entry.setUserId(user != null ? user.getId() : null);
        entry.setUsername(user != null ? user.getUsername() : "system");
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details);
        entry.setPlatform(platform != null ? platform : "api");
        entry.setIpAddress(clientIp());
        entry.setCreatedAt(Instant.now());
        auditLogRepository.save(entry);
    }

    public void logLogin(AdminUserEntity user, String platform, boolean success, String details) {
        var entry = new AuditLogEntity();
        entry.setUserId(user != null ? user.getId() : null);
        entry.setUsername(user != null ? user.getUsername() : "unknown");
        entry.setAction(success ? "LOGIN" : "LOGIN_FAILED");
        entry.setEntityType("USER");
        entry.setEntityId(user != null ? user.getId().toString() : null);
        entry.setDetails(details);
        entry.setPlatform(platform != null ? platform : "web");
        entry.setIpAddress(clientIp());
        entry.setCreatedAt(Instant.now());
        auditLogRepository.save(entry);
    }

    public Page<AuditLogEntity> search(
            String username,
            String action,
            String entityType,
            String entityId,
            String platform,
            String search,
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        Specification<AuditLogEntity> spec = (root, query, cb) -> {
            var preds = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (StringUtils.hasText(username)) {
                preds.add(cb.equal(root.get("username"), username));
            }
            if (StringUtils.hasText(action)) {
                preds.add(cb.equal(root.get("action"), action));
            }
            if (StringUtils.hasText(entityType)) {
                preds.add(cb.equal(root.get("entityType"), entityType));
            }
            if (StringUtils.hasText(entityId)) {
                preds.add(cb.equal(root.get("entityId"), entityId));
            }
            if (StringUtils.hasText(platform)) {
                preds.add(cb.equal(root.get("platform"), platform));
            }
            if (from != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            if (StringUtils.hasText(search)) {
                String like = "%" + search.toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("details")), like),
                        cb.like(cb.lower(root.get("entityId")), like),
                        cb.like(cb.lower(root.get("username")), like)
                ));
            }
            return cb.and(preds.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return auditLogRepository.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public List<String> distinctActions() {
        return auditLogRepository.findAll(Sort.by(Sort.Direction.ASC, "action")).stream()
                .map(AuditLogEntity::getAction)
                .distinct()
                .sorted()
                .toList();
    }

    private AdminUserEntity currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return adminUserRepository.findByUsername(auth.getName()).orElse(null);
    }

    private String currentPlatform() {
        HttpServletRequest req = currentRequest();
        if (req == null) return "api";
        String p = req.getHeader("X-Client-Platform");
        return StringUtils.hasText(p) ? p : "web";
    }

    private String clientIp() {
        HttpServletRequest req = currentRequest();
        if (req == null) return null;
        String xff = req.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }
}
