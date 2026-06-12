package com.paradissaveurs.service;

import com.paradissaveurs.config.AppProperties;
import com.paradissaveurs.dto.ProductDto;
import com.paradissaveurs.dto.ProductRequest;
import com.paradissaveurs.entity.ProductEntity;
import com.paradissaveurs.mapper.EntityMapper;
import com.paradissaveurs.repository.OrderItemRepository;
import com.paradissaveurs.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.paradissaveurs.util.SafeImageUploadValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private static final String UNCLASSIFIED = "Non classé";

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final EntityMapper mapper;
    private final Path uploadDir;
    private final AuditService auditService;
    private final CategoryService categoryService;
    private final OrderPricingService pricingService;

    public ProductService(ProductRepository productRepository, OrderItemRepository orderItemRepository,
                          EntityMapper mapper, AppProperties appProperties, AuditService auditService,
                          CategoryService categoryService, OrderPricingService pricingService) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.mapper = mapper;
        this.uploadDir = Paths.get(appProperties.getUploadDir()).toAbsolutePath().normalize();
        this.auditService = auditService;
        this.categoryService = categoryService;
        this.pricingService = pricingService;
    }

    @Transactional(readOnly = true)
    public List<ProductDto> findAllPublic() {
        var activePromos = pricingService.getActivePromotions();
        return productRepository.findAllByActiveTrueOrderByFeaturedDescNameAsc().stream()
                .map(p -> {
                    var ep = pricingService.getEffectivePrice(p, activePromos);
                    return mapper.toProductDto(p, ep.effectivePrice(),
                            ep.onPromo() ? ep.catalogPrice() : null, ep.onPromo(), ep.promoScope());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDto> findAllForAdmin() {
        return productRepository.findAllByOrderByNameAsc().stream()
                .map(mapper::toProductDto)
                .toList();
    }

    @Transactional
    public ProductDto create(ProductRequest request) {
        validateProductRequest(request);
        var entity = new ProductEntity();
        entity.setActive(request.active() == null || request.active());
        entity.setFeatured(Boolean.TRUE.equals(request.featured()));
        mapper.applyProductRequest(entity, request);
        entity.setCategory(categoryService.canonicalName(request.category()));
        var saved = productRepository.save(entity);
        auditService.log("PRODUCT_CREATE", "PRODUCT", saved.getId().toString(), "Produit créé : " + saved.getName());
        return mapper.toProductDto(saved);
    }

    @Transactional
    public ProductDto update(Long id, ProductRequest request) {
        validateProductRequest(request);
        var entity = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable"));
        mapper.applyProductRequest(entity, request);
        entity.setCategory(categoryService.canonicalName(request.category()));
        var saved = productRepository.save(entity);
        auditService.log("PRODUCT_UPDATE", "PRODUCT", id.toString(), "Produit modifié : " + saved.getName());
        return mapper.toProductDto(saved);
    }

    @Transactional
    public ProductDto patchStock(Long id, int stock) {
        if (stock < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le stock ne peut pas être négatif");
        }
        var entity = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable"));
        entity.setStock(stock);
        var saved = productRepository.save(entity);
        auditService.log("PRODUCT_STOCK", "PRODUCT", id.toString(),
                "Stock « " + saved.getName() + " » → " + stock);
        return mapper.toProductDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        var entity = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable"));
        if (orderItemRepository.existsByProductId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce produit figure dans des commandes passées — désactivez-le plutôt que de le supprimer.");
        }
        productRepository.deleteById(id);
        auditService.log("PRODUCT_DELETE", "PRODUCT", id.toString(), "Produit supprimé : " + entity.getName());
    }

    @Transactional
    public void reassignCategory(String fromCategory, String toCategory) {
        if (fromCategory == null || fromCategory.isBlank()) return;
        String target = (toCategory == null || toCategory.isBlank()) ? UNCLASSIFIED : toCategory.trim();
        productRepository.reassignCategory(fromCategory.trim(), target);
    }

    @Transactional
    public ProductDto uploadImage(Long id, MultipartFile file) {
        var entity = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable"));

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier requis");
        }

        String ext = SafeImageUploadValidator.validateAndExtension(file);

        try {
            Files.createDirectories(uploadDir);
            String filename = "product-" + id + "-" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path target = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), target);
            entity.setImageUrl("/uploads/" + filename);
            return mapper.toProductDto(productRepository.save(entity));
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur upload");
        }
    }

    private void validateCategory(String category) {
        if (UNCLASSIFIED.equalsIgnoreCase(category != null ? category.trim() : "")) return;
        categoryService.validateCategoryName(category);
    }

   
    private void validateProductRequest(ProductRequest request) {
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indiquez le nom du produit");
        }
        if (request.name().trim().length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom ne peut pas dépasser 120 caractères");
        }
        validateCategory(request.category());
        if (request.price() == null || request.price() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le prix doit être supérieur à 0 FCFA");
        }
        if (request.stock() == null || request.stock() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le stock ne peut pas être négatif");
        }
        if (request.description() != null && request.description().length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La description ne peut pas dépasser 500 caractères");
        }
    }
}
