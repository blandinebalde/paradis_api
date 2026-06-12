package com.paradissaveurs.service;

import com.paradissaveurs.dto.CategoryDto;
import com.paradissaveurs.dto.CategoryRequest;
import com.paradissaveurs.entity.CategoryEntity;
import com.paradissaveurs.entity.ProductPromotionEntity;
import com.paradissaveurs.domain.PromotionScope;
import com.paradissaveurs.mapper.EntityMapper;
import com.paradissaveurs.repository.CategoryRepository;
import com.paradissaveurs.repository.ProductPromotionRepository;
import com.paradissaveurs.repository.ProductRepository;
import com.paradissaveurs.util.CategoryMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CategoryService {

    public static final String UNCLASSIFIED = "Non classé";

    private static final Map<String, String> DEFAULT_EMOJIS = Map.of(
            "Crêpes", "🥞",
            "Fast-food", "🍔",
            "Jus naturels", "🍹",
            "Smoothies", "🍓",
            UNCLASSIFIED, "🍽️"
    );

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductPromotionRepository promotionRepository;
    private final SettingsService settingsService;
    private final EntityMapper mapper;
    private final AuditService auditService;

    public CategoryService(CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           ProductPromotionRepository promotionRepository,
                           SettingsService settingsService,
                           EntityMapper mapper,
                           AuditService auditService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.promotionRepository = promotionRepository;
        this.settingsService = settingsService;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional
    public void migrateFromLegacyIfEmpty() {
        if (categoryRepository.count() > 0) return;

        var settings = settingsService.getOrCreate();
        List<String> names = mapper.parseCategories(settings.getCategoriesJson());
        int order = 0;
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            if (categoryRepository.findByNameIgnoreCase(name.trim()).isPresent()) continue;
            var entity = new CategoryEntity();
            entity.setName(name.trim());
            entity.setEmoji(defaultEmoji(name.trim()));
            entity.setSortOrder(order++);
            entity.setActive(true);
            categoryRepository.save(entity);
        }

        if (categoryRepository.count() == 0) {
            seedDefaultCategories();
        }
        syncSettingsJson();
        normalizePromotionCategories();
    }

    /** Corrige les noms de catégorie des promos existantes (ex. « fastfood » → « Fast-food »). */
    @Transactional
    public void normalizePromotionCategoriesOnStartup() {
        if (categoryRepository.count() == 0) return;
        normalizePromotionCategories();
    }

    private void normalizePromotionCategories() {
        for (ProductPromotionEntity promo : promotionRepository.findAll()) {
            if (!PromotionScope.CATEGORY.equals(promo.getScope()) || promo.getCategory() == null) continue;
            String canonical = canonicalName(promo.getCategory());
            if (!canonical.equals(promo.getCategory())) {
                promo.setCategory(canonical);
                promotionRepository.save(promo);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> findAllPublic() {
        return categoryRepository.findAllByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> findAllAdmin() {
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getActiveNames() {
        return categoryRepository.findAllByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(CategoryEntity::getName)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<CategoryEntity> resolve(String input) {
        if (input == null || input.isBlank()) return Optional.empty();
        String key = CategoryMatcher.normalizeKey(input);
        return categoryRepository.findAll().stream()
                .filter(c -> CategoryMatcher.normalizeKey(c.getName()).equals(key))
                .findFirst();
    }

    /** Nom canonique enregistré en base (ex. « Fast-food » pour « fastfood »). */
    @Transactional(readOnly = true)
    public String canonicalName(String input) {
        return resolve(input).map(CategoryEntity::getName).orElse(input != null ? input.trim() : null);
    }

    @Transactional(readOnly = true)
    public boolean matches(String categoryA, String categoryB) {
        return CategoryMatcher.matches(categoryA, categoryB);
    }

    @Transactional(readOnly = true)
    public long countActiveProducts(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) return 0;
        return productRepository.findAllByActiveTrueOrderByFeaturedDescNameAsc().stream()
                .filter(p -> CategoryMatcher.matches(p.getCategory(), categoryName))
                .count();
    }

    @Transactional(readOnly = true)
    public void validateCategoryName(String category) {
        if (category == null || category.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choisissez une catégorie");
        }
        var resolved = resolve(category);
        if (resolved.isEmpty() || !resolved.get().isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Catégorie invalide — choisissez parmi : " + String.join(", ", getActiveNames()));
        }
    }

    @Transactional
    public CategoryDto create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)
                || resolve(name).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette catégorie existe déjà");
        }
        var entity = new CategoryEntity();
        entity.setName(name);
        entity.setEmoji(request.emoji() != null && !request.emoji().isBlank()
                ? request.emoji().trim() : defaultEmoji(name));
        entity.setSortOrder(request.sortOrder() != null ? request.sortOrder() : nextSortOrder());
        entity.setActive(request.active() == null || request.active());
        var saved = categoryRepository.save(entity);
        syncSettingsJson();
        auditService.log("CATEGORY_CREATE", "CATEGORY", saved.getId().toString(), "Catégorie : " + saved.getName());
        return toDto(saved);
    }

    @Transactional
    public CategoryDto update(Long id, CategoryRequest request) {
        var entity = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catégorie introuvable"));
        String newName = request.name().trim();
        var duplicate = resolve(newName);
        if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette catégorie existe déjà");
        }
        String oldName = entity.getName();
        if (!oldName.equals(newName)) {
            categoryRepository.reassignProducts(oldName, newName);
            promotionRepository.reassignCategory(oldName, newName);
        }
        entity.setName(newName);
        if (request.emoji() != null && !request.emoji().isBlank()) {
            entity.setEmoji(request.emoji().trim());
        }
        if (request.sortOrder() != null) entity.setSortOrder(request.sortOrder());
        if (request.active() != null) entity.setActive(request.active());
        var saved = categoryRepository.save(entity);
        syncSettingsJson();
        auditService.log("CATEGORY_UPDATE", "CATEGORY", id.toString(), "Catégorie : " + saved.getName());
        return toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        var entity = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catégorie introuvable"));
        if (UNCLASSIFIED.equalsIgnoreCase(entity.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La catégorie « Non classé » ne peut pas être supprimée");
        }
        long productCount = productRepository.countByCategory(entity.getName());
        if (productCount > 0) {
            ensureUnclassifiedExists();
            categoryRepository.reassignProducts(entity.getName(), UNCLASSIFIED);
            promotionRepository.reassignCategory(entity.getName(), UNCLASSIFIED);
        }
        categoryRepository.delete(entity);
        syncSettingsJson();
        auditService.log("CATEGORY_DELETE", "CATEGORY", id.toString(), "Catégorie supprimée : " + entity.getName());
    }

    private void ensureUnclassifiedExists() {
        if (resolve(UNCLASSIFIED).isEmpty()) {
            var u = new CategoryEntity();
            u.setName(UNCLASSIFIED);
            u.setEmoji("🍽️");
            u.setSortOrder(nextSortOrder());
            u.setActive(true);
            categoryRepository.save(u);
        }
    }

    private void seedDefaultCategories() {
        String[] defaults = {"Crêpes", "Fast-food", "Jus naturels", "Smoothies"};
        for (int i = 0; i < defaults.length; i++) {
            var entity = new CategoryEntity();
            entity.setName(defaults[i]);
            entity.setEmoji(defaultEmoji(defaults[i]));
            entity.setSortOrder(i);
            entity.setActive(true);
            categoryRepository.save(entity);
        }
    }

    private int nextSortOrder() {
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .mapToInt(CategoryEntity::getSortOrder)
                .max()
                .orElse(-1) + 1;
    }

    private String defaultEmoji(String name) {
        return DEFAULT_EMOJIS.getOrDefault(name, "🍽️");
    }

    private CategoryDto toDto(CategoryEntity e) {
        return new CategoryDto(
                String.valueOf(e.getId()),
                e.getName(),
                e.getEmoji(),
                e.getSortOrder(),
                e.isActive(),
                productRepository.countByCategory(e.getName())
        );
    }

    private void syncSettingsJson() {
        var names = categoryRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(CategoryEntity::getName)
                .toList();
        settingsService.saveCategoriesJson(names);
    }
}
