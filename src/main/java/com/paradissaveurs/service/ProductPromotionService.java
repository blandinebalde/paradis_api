package com.paradissaveurs.service;

import com.paradissaveurs.domain.DiscountType;
import com.paradissaveurs.domain.PromotionScope;
import com.paradissaveurs.dto.ProductPromotionDto;
import com.paradissaveurs.dto.ProductPromotionRequest;
import com.paradissaveurs.dto.PublicPromotionDto;
import com.paradissaveurs.entity.ProductEntity;
import com.paradissaveurs.entity.ProductPromotionEntity;
import com.paradissaveurs.mapper.EntityMapper;
import com.paradissaveurs.repository.ProductPromotionRepository;
import com.paradissaveurs.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class ProductPromotionService {

    private final ProductPromotionRepository repository;
    private final ProductRepository productRepository;
    private final OrderPricingService pricingService;
    private final CategoryService categoryService;
    private final EntityMapper mapper;
    private final AuditService auditService;

    public ProductPromotionService(ProductPromotionRepository repository,
                                   ProductRepository productRepository,
                                   OrderPricingService pricingService,
                                   CategoryService categoryService,
                                   EntityMapper mapper,
                                   AuditService auditService) {
        this.repository = repository;
        this.productRepository = productRepository;
        this.pricingService = pricingService;
        this.categoryService = categoryService;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ProductPromotionDto> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    /** Promotions actives pour le client (produit ou catégorie entière). */
    @Transactional(readOnly = true)
    public List<PublicPromotionDto> findActivePublic() {
        return pricingService.getActivePromotions().stream()
                .map(this::toPublicDto)
                .toList();
    }

    @Transactional
    public ProductPromotionDto create(ProductPromotionRequest request) {
        validateRequest(request);
        var entity = new ProductPromotionEntity();
        applyRequest(entity, request);
        entity.setCreatedAt(Instant.now());
        if (request.active() != null) entity.setActive(request.active());
        var saved = repository.save(entity);
        auditService.log("PROMO_CREATE", "PRODUCT_PROMO", saved.getId().toString(),
                "Promotion produit : " + (saved.getName() != null ? saved.getName() : saved.getScope()));
        return toDto(saved);
    }

    @Transactional
    public ProductPromotionDto update(Long id, ProductPromotionRequest request) {
        validateRequest(request);
        var entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion introuvable"));
        applyRequest(entity, request);
        if (request.active() != null) entity.setActive(request.active());
        var saved = repository.save(entity);
        auditService.log("PROMO_UPDATE", "PRODUCT_PROMO", id.toString(), "Promotion modifiée");
        return toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion introuvable");
        }
        repository.deleteById(id);
        auditService.log("PROMO_DELETE", "PRODUCT_PROMO", id.toString(), "Promotion supprimée");
    }

    private void validateRequest(ProductPromotionRequest request) {
        if (!PromotionScope.isValid(request.scope())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Portée invalide — produit ou catégorie");
        }
        if (!DiscountType.isValid(request.discountType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de réduction invalide");
        }
        if (DiscountType.PERCENTAGE.equals(request.discountType()) && request.discountValue() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le pourcentage ne peut pas dépasser 100");
        }
        if (PromotionScope.PRODUCT.equals(request.scope())) {
            Long pid = mapper.parseLongId(request.productId());
            if (pid == null || !productRepository.existsById(pid)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produit cible invalide");
            }
        } else if (PromotionScope.CATEGORY.equals(request.scope())) {
            if (request.category() == null || request.category().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indiquez la catégorie cible");
            }
            categoryService.validateCategoryName(request.category());
        }
        if (request.startDate() != null && request.endDate() != null
                && request.startDate().isAfter(request.endDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La date de début doit être antérieure à la date de fin");
        }
    }

    private void applyRequest(ProductPromotionEntity entity, ProductPromotionRequest request) {
        entity.setName(request.name() != null ? request.name().trim() : null);
        entity.setScope(request.scope());
        entity.setProductId(PromotionScope.PRODUCT.equals(request.scope())
                ? mapper.parseLongId(request.productId()) : null);
        entity.setCategory(PromotionScope.CATEGORY.equals(request.scope())
                ? categoryService.canonicalName(request.category()) : null);
        entity.setDiscountType(request.discountType());
        entity.setDiscountValue(request.discountValue());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
    }

    private ProductPromotionDto toDto(ProductPromotionEntity e) {
        Integer previewOriginal = null;
        Integer previewPromo = null;
        if (PromotionScope.PRODUCT.equals(e.getScope()) && e.getProductId() != null) {
            var productOpt = productRepository.findById(e.getProductId());
            if (productOpt.isPresent()) {
                var p = productOpt.get();
                previewOriginal = p.getPrice();
                previewPromo = pricingService.applyDiscount(p.getPrice(), e.getDiscountType(), e.getDiscountValue());
            }
        } else if (PromotionScope.CATEGORY.equals(e.getScope()) && e.getCategory() != null) {
            var sample = productRepository.findAllByActiveTrueOrderByFeaturedDescNameAsc().stream()
                    .filter(p -> categoryService.matches(p.getCategory(), e.getCategory()))
                    .findFirst();
            if (sample.isPresent()) {
                var p = sample.get();
                previewOriginal = p.getPrice();
                previewPromo = pricingService.applyDiscount(p.getPrice(), e.getDiscountType(), e.getDiscountValue());
            }
        }
        return new ProductPromotionDto(
                String.valueOf(e.getId()),
                e.getName(),
                e.getScope(),
                e.getProductId() != null ? String.valueOf(e.getProductId()) : null,
                e.getCategory(),
                e.getDiscountType(),
                e.getDiscountValue(),
                e.getStartDate(),
                e.getEndDate(),
                e.isActive(),
                previewOriginal,
                previewPromo
        );
    }

    private PublicPromotionDto toPublicDto(ProductPromotionEntity e) {
        int affected = 0;
        if (PromotionScope.CATEGORY.equals(e.getScope()) && e.getCategory() != null) {
            affected = (int) categoryService.countActiveProducts(e.getCategory());
        } else if (PromotionScope.PRODUCT.equals(e.getScope()) && e.getProductId() != null) {
            affected = productRepository.findById(e.getProductId())
                    .filter(ProductEntity::isActive)
                    .map(p -> 1)
                    .orElse(0);
        }
        return new PublicPromotionDto(
                String.valueOf(e.getId()),
                e.getName(),
                e.getScope(),
                e.getProductId() != null ? String.valueOf(e.getProductId()) : null,
                e.getCategory(),
                e.getDiscountType(),
                e.getDiscountValue(),
                affected
        );
    }
}
