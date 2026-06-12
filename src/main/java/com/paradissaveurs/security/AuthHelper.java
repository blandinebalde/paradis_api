package com.paradissaveurs.security;

import com.paradissaveurs.entity.AdminUserEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;

public final class AuthHelper {

    private AuthHelper() {}

    public static List<SimpleGrantedAuthority> authoritiesFor(AdminUserEntity user) {
        var list = new ArrayList<SimpleGrantedAuthority>();
        list.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (user != null) {
            for (AppPermission p : user.permissionSet()) {
                list.add(new SimpleGrantedAuthority(p.authority()));
            }
        }
        return list;
    }

    public static boolean isWebPlatform(String platform) {
        return platform == null || platform.isBlank() || "web".equalsIgnoreCase(platform);
    }

    public static boolean isMobilePlatform(String platform) {
        return "mobile".equalsIgnoreCase(platform);
    }
}
