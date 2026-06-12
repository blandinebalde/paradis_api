package com.paradissaveurs.entity;

import com.paradissaveurs.security.AppPermission;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "admin_users")
public class AdminUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    private String displayName;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean allowWeb = true;

    @Column(nullable = false)
    private boolean allowMobile = false;

    @Column(length = 500)
    private String permissions = "";

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Set<AppPermission> permissionSet() {
        return AppPermission.parse(permissions);
    }

    public void setPermissionSet(Set<AppPermission> perms) {
        this.permissions = AppPermission.serialize(perms != null ? perms : EnumSet.noneOf(AppPermission.class));
    }

    public boolean hasPermission(AppPermission perm) {
        return permissionSet().contains(perm);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isAllowWeb() { return allowWeb; }
    public void setAllowWeb(boolean allowWeb) { this.allowWeb = allowWeb; }
    public boolean isAllowMobile() { return allowMobile; }
    public void setAllowMobile(boolean allowMobile) { this.allowMobile = allowMobile; }
    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
