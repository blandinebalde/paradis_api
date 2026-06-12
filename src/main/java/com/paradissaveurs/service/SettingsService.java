package com.paradissaveurs.service;

import com.paradissaveurs.dto.SettingsDto;
import com.paradissaveurs.entity.ShopSettingsEntity;
import com.paradissaveurs.mapper.EntityMapper;
import com.paradissaveurs.repository.ShopSettingsRepository;
import com.paradissaveurs.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SettingsService {

    private final ShopSettingsRepository settingsRepository;
    private final ProductRepository productRepository;
    private final EntityMapper mapper;
    private final AuditService auditService;

    public SettingsService(ShopSettingsRepository settingsRepository, ProductRepository productRepository,
                           EntityMapper mapper, AuditService auditService) {
        this.settingsRepository = settingsRepository;
        this.productRepository = productRepository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    public ShopSettingsEntity getOrCreate() {
        return settingsRepository.findById(1L).orElseGet(this::createDefault);
    }

    public SettingsDto getPublicSettings() {
        return mapper.toSettingsDto(getOrCreate(), false);
    }

    public SettingsDto getAdminSettings() {
        return mapper.toSettingsDto(getOrCreate(), true);
    }

    /**
     * Vérifie que le mode de paiement demandé est activé dans les paramètres boutique.
     * Identifiants acceptés : livraison (cash), wave, om.
     */
    public void requireEnabledPaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choisissez un mode de paiement valide");
        }
        var settings = getOrCreate();
        String method = paymentMethod.trim().toLowerCase();
        boolean enabled = switch (method) {
            case "livraison" -> Boolean.TRUE.equals(settings.getCash());
            case "wave" -> Boolean.TRUE.equals(settings.getWave());
            case "om" -> Boolean.TRUE.equals(settings.getOm());
            default -> false;
        };
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce mode de paiement n'est pas disponible actuellement");
        }
    }

    @Transactional
    public SettingsDto update(SettingsDto dto) {
        var entity = getOrCreate();
        mapper.applySettings(entity, dto);
        var saved = settingsRepository.save(entity);
        auditService.log("SETTINGS_UPDATE", "SETTINGS", "1", "Paramètres boutique mis à jour");
        return mapper.toSettingsDto(saved, true);
    }

    @Transactional
    public void saveCategoriesJson(List<String> names) {
        var entity = getOrCreate();
        entity.setCategoriesJson(mapper.writeCategories(names));
        settingsRepository.save(entity);
    }

    private ShopSettingsEntity createDefault() {
        var entity = new ShopSettingsEntity();
        entity.setId(1L);
        entity.setMaxProductsPerOrder(20);
        entity.setShopName("Paradis des Saveurs");
        entity.setSlogan("Le paradis des gourmands 😋");
        entity.setShopAddress("123 Avenue des Gourmands");
        entity.setShopPhone("+221 77 000 00 00");
        entity.setWaveNumber("+221 77 111 11 11");
        entity.setOmNumber("+221 77 222 22 22");
        entity.setAdminNotifyPhone("+221 77 000 00 00");
        entity.setNotifyViaWhatsApp(true);
        entity.setNotifyViaSms(false);
        entity.setWave(true);
        entity.setOm(true);
        entity.setCash(true);
        entity.setCategoriesJson(mapper.writeCategories(
                java.util.List.of("Crêpes", "Fast-food", "Jus naturels", "Smoothies")));
        return settingsRepository.save(entity);
    }
}
