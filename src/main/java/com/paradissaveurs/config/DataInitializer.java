package com.paradissaveurs.config;

import com.paradissaveurs.entity.AdminUserEntity;
import com.paradissaveurs.entity.DeliveryZoneEntity;
import com.paradissaveurs.entity.ProductEntity;
import com.paradissaveurs.repository.AdminUserRepository;
import com.paradissaveurs.repository.DeliveryZoneRepository;
import com.paradissaveurs.repository.ProductRepository;
import com.paradissaveurs.security.AppPermission;
import com.paradissaveurs.service.SettingsService;
import com.paradissaveurs.service.CategoryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final DeliveryZoneRepository zoneRepository;
    private final SettingsService settingsService;
    private final CategoryService categoryService;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(ProductRepository productRepository, DeliveryZoneRepository zoneRepository,
                           SettingsService settingsService, CategoryService categoryService,
                           AdminUserRepository adminUserRepository,
                           PasswordEncoder passwordEncoder) {
        this.productRepository = productRepository;
        this.zoneRepository = zoneRepository;
        this.settingsService = settingsService;
        this.categoryService = categoryService;
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        settingsService.getOrCreate();
        categoryService.migrateFromLegacyIfEmpty();
        categoryService.normalizePromotionCategoriesOnStartup();

        if (adminUserRepository.count() == 0) {
            var admin = new AdminUserEntity();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("paradis2024"));
            admin.setDisplayName("Administrateur");
            admin.setActive(true);
            admin.setAllowWeb(true);
            admin.setAllowMobile(true);
            admin.setPermissionSet(EnumSet.allOf(AppPermission.class));
            adminUserRepository.save(admin);
        } else {
            adminUserRepository.findAll().forEach(this::upgradeLegacyUser);
        }

        if (productRepository.count() == 0) {
            seedProducts();
        }

        if (zoneRepository.count() == 0) {
            seedZones();
        }
    }

    private void upgradeLegacyUser(AdminUserEntity user) {
        boolean changed = false;
        if (user.permissionSet().isEmpty()) {
            user.setPermissionSet(EnumSet.allOf(AppPermission.class));
            changed = true;
        }
        if (user.getDisplayName() == null || user.getDisplayName().isBlank()) {
            user.setDisplayName(user.getUsername());
            changed = true;
        }
        if ("admin".equals(user.getUsername())) {
            if (!user.isActive()) {
                user.setActive(true);
                changed = true;
            }
            if (!user.isAllowWeb() || !user.isAllowMobile()) {
                user.setAllowWeb(true);
                user.setAllowMobile(true);
                changed = true;
            }
        }
        if (changed) adminUserRepository.save(user);
    }

    private void seedProducts() {
        String[][] data = {
                {"Crêpe Nutella", "Crêpes", "1500", "30", "🥞", "Avec pâte à tartiner et noisettes"},
                {"Crêpe Fruits Rouges", "Crêpes", "1800", "25", "🥞", "Fraises, framboises, chantilly"},
                {"Burger Classic", "Fast-food", "2500", "20", "🍔", "Steak, cheddar, salade, tomate"},
                {"Burger Poulet", "Fast-food", "2200", "15", "🍔", "Blanc de poulet, sauce piquante"},
                {"Frites Maison", "Fast-food", "800", "50", "🍟", "Croustillantes, sel, épices"},
                {"Jus de Bissap", "Jus naturels", "500", "40", "🍹", "100% naturel, sans sucre ajouté"},
                {"Jus Gingembre", "Jus naturels", "500", "35", "🍹", "Frais et énergisant"},
                {"Smoothie Fraise-Banane", "Smoothies", "1200", "20", "🍓", "Fraises fraîches + banane"},
                {"Smoothie Tropical", "Smoothies", "1300", "18", "🥭", "Mangue, ananas, passion"},
        };
        for (String[] row : data) {
            var p = new ProductEntity();
            p.setName(row[0]);
            p.setCategory(row[1]);
            p.setPrice(Integer.parseInt(row[2]));
            p.setStock(Integer.parseInt(row[3]));
            p.setEmoji(row[4]);
            p.setDescription(row[5]);
            productRepository.save(p);
        }
    }

    private void seedZones() {
        String[][] data = {
                {"Plateau", "500"},
                {"Almadies", "1000"},
                {"Sacré-Coeur", "800"},
                {"Grand-Yoff", "700"},
                {"Médina", "600"},
        };
        for (String[] row : data) {
            var z = new DeliveryZoneEntity();
            z.setName(row[0]);
            z.setFee(Integer.parseInt(row[1]));
            zoneRepository.save(z);
        }
    }
}
