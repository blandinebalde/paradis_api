package com.paradissaveurs.dto;

import java.util.List;

public record SettingsDto(
        Integer maxProductsPerOrder,
        String shopName,
        String slogan,
        String shopAddress,
        String shopPhone,
        String waveNumber,
        String omNumber,
        String adminNotifyPhone,
        Boolean notifyViaWhatsApp,
        Boolean notifyViaSms,
        Boolean wave,
        Boolean om,
        Boolean cash,
        List<String> categories,
        String adminUser
) {}
