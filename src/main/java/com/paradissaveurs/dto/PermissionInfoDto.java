package com.paradissaveurs.dto;

import com.paradissaveurs.security.AppPermission;
import java.util.Arrays;
import java.util.List;

public record PermissionInfoDto(String code, String label) {
    public static List<PermissionInfoDto> all() {
        return Arrays.stream(AppPermission.values())
                .map(p -> new PermissionInfoDto(p.name(), p.getLabel()))
                .toList();
    }
}
