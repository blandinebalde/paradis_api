package com.paradissaveurs.controller;

import com.paradissaveurs.dto.*;
import com.paradissaveurs.entity.AuditLogEntity;
import com.paradissaveurs.mapper.EntityMapper;
import com.paradissaveurs.service.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ProductService productService;
    private final ZoneService zoneService;
    private final SettingsService settingsService;
    private final OrderService orderService;
    private final AuthService authService;
    private final DeviceTokenService deviceTokenService;
    private final UserService userService;
    private final AuditService auditService;
    private final ProductPromotionService productPromotionService;
    private final PromoCodeService promoCodeService;
    private final CategoryService categoryService;
    private final EntityMapper mapper;

    public AdminController(ProductService productService, ZoneService zoneService,
                           SettingsService settingsService, OrderService orderService,
                           AuthService authService, DeviceTokenService deviceTokenService,
                           UserService userService, AuditService auditService,
                           ProductPromotionService productPromotionService,
                           PromoCodeService promoCodeService,
                           CategoryService categoryService,
                           EntityMapper mapper) {
        this.productService = productService;
        this.zoneService = zoneService;
        this.settingsService = settingsService;
        this.orderService = orderService;
        this.authService = authService;
        this.deviceTokenService = deviceTokenService;
        this.userService = userService;
        this.auditService = auditService;
        this.productPromotionService = productPromotionService;
        this.promoCodeService = promoCodeService;
        this.categoryService = categoryService;
        this.mapper = mapper;
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('PERM_ORDERS_READ')")
    public OrderPageDto orders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return orderService.findPage(status, page, size);
    }

    @GetMapping("/orders/stats")
    @PreAuthorize("hasAuthority('PERM_ORDERS_READ')")
    public OrderStatsDto orderStats() {
        return orderService.getStats();
    }

    @GetMapping("/orders/recent")
    @PreAuthorize("hasAuthority('PERM_ORDERS_READ')")
    public List<OrderDto> recentOrders(@RequestParam(defaultValue = "200") int limit) {
        return orderService.findRecent(limit);
    }

    @PatchMapping("/orders/{id}/status")
    @PreAuthorize("hasAuthority('PERM_ORDERS_WRITE')")
    public OrderDto updateOrderStatus(@PathVariable String id, @Valid @RequestBody StatusUpdateRequest request) {
        return orderService.updateStatus(id, request.status());
    }

    @GetMapping("/products")
    @PreAuthorize("hasAuthority('PERM_PRODUCTS_WRITE')")
    public List<ProductDto> listProducts() {
        return productService.findAllForAdmin();
    }

    @PostMapping("/products")
    @PreAuthorize("hasAuthority('PERM_PRODUCTS_WRITE')")
    public ProductDto createProduct(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasAuthority('PERM_PRODUCTS_WRITE')")
    public ProductDto updateProduct(@PathVariable String id, @Valid @RequestBody ProductRequest request) {
        Long longId = mapper.parseLongId(id);
        if (longId == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "ID produit invalide");
        return productService.update(longId, request);
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasAuthority('PERM_PRODUCTS_WRITE')")
    public void deleteProduct(@PathVariable String id) {
        Long longId = mapper.parseLongId(id);
        if (longId == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "ID produit invalide");
        productService.delete(longId);
    }

    @PatchMapping("/products/{id}/stock")
    @PreAuthorize("hasAuthority('PERM_PRODUCTS_WRITE')")
    public ProductDto patchProductStock(@PathVariable String id, @Valid @RequestBody StockPatchRequest request) {
        Long longId = mapper.parseLongId(id);
        if (longId == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "ID produit invalide");
        return productService.patchStock(longId, request.stock());
    }

    @PostMapping("/products/{id}/image")
    @PreAuthorize("hasAuthority('PERM_PRODUCTS_WRITE')")
    public ProductDto uploadProductImage(@PathVariable String id, @RequestParam("file") MultipartFile file) {
        Long longId = mapper.parseLongId(id);
        if (longId == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "ID produit invalide");
        return productService.uploadImage(longId, file);
    }

    @GetMapping("/promotions/products")
    @PreAuthorize("hasAuthority('PERM_PRODUCTS_WRITE')")
    public List<ProductPromotionDto> listProductPromotions() {
        return productPromotionService.findAll();
    }

    @PostMapping("/promotions/products")
    @PreAuthorize("hasAuthority('PERM_PRODUCTS_WRITE')")
    public ProductPromotionDto createProductPromotion(@Valid @RequestBody ProductPromotionRequest request) {
        return productPromotionService.create(request);
    }

    @PutMapping("/promotions/products/{id}")
    @PreAuthorize("hasAuthority('PERM_PRODUCTS_WRITE')")
    public ProductPromotionDto updateProductPromotion(@PathVariable String id,
                                                      @Valid @RequestBody ProductPromotionRequest request) {
        Long longId = mapper.parseLongId(id);
        if (longId == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "ID promotion invalide");
        return productPromotionService.update(longId, request);
    }

    @DeleteMapping("/promotions/products/{id}")
    @PreAuthorize("hasAuthority('PERM_PRODUCTS_WRITE')")
    public void deleteProductPromotion(@PathVariable String id) {
        Long longId = mapper.parseLongId(id);
        if (longId == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "ID promotion invalide");
        productPromotionService.delete(longId);
    }

    @GetMapping("/promotions/codes")
    @PreAuthorize("hasAuthority('PERM_PRODUCTS_WRITE')")
    public List<PromoCodeDto> listPromoCodes() {
        return promoCodeService.findAll();
    }

    @PostMapping("/promotions/codes")
    @PreAuthorize("hasAuthority('PERM_PRODUCTS_WRITE')")
    public PromoCodeDto createPromoCode(@Valid @RequestBody PromoCodeRequest request) {
        return promoCodeService.create(request);
    }

    @PutMapping("/promotions/codes/{id}")
    @PreAuthorize("hasAuthority('PERM_PRODUCTS_WRITE')")
    public PromoCodeDto updatePromoCode(@PathVariable String id, @Valid @RequestBody PromoCodeRequest request) {
        Long longId = mapper.parseLongId(id);
        if (longId == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "ID code invalide");
        return promoCodeService.update(longId, request);
    }

    @DeleteMapping("/promotions/codes/{id}")
    @PreAuthorize("hasAuthority('PERM_PRODUCTS_WRITE')")
    public void deletePromoCode(@PathVariable String id) {
        Long longId = mapper.parseLongId(id);
        if (longId == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "ID code invalide");
        promoCodeService.delete(longId);
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('PERM_PRODUCTS_WRITE')")
    public List<CategoryDto> listCategories() {
        return categoryService.findAllAdmin();
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAuthority('PERM_PRODUCTS_WRITE')")
    public CategoryDto createCategory(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('PERM_PRODUCTS_WRITE')")
    public CategoryDto updateCategory(@PathVariable String id, @Valid @RequestBody CategoryRequest request) {
        Long longId = mapper.parseLongId(id);
        if (longId == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "ID catégorie invalide");
        return categoryService.update(longId, request);
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('PERM_PRODUCTS_WRITE')")
    public void deleteCategory(@PathVariable String id) {
        Long longId = mapper.parseLongId(id);
        if (longId == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "ID catégorie invalide");
        categoryService.delete(longId);
    }

    @GetMapping("/promotions/codes/{id}/usages")
    @PreAuthorize("hasAuthority('PERM_PRODUCTS_WRITE')")
    public List<PromoCodeUsageDto> promoCodeUsages(@PathVariable String id) {
        Long longId = mapper.parseLongId(id);
        if (longId == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "ID code invalide");
        return promoCodeService.findUsages(longId).stream()
                .map(u -> new PromoCodeUsageDto(
                        String.valueOf(u.getId()),
                        u.getOrderId(),
                        u.getCustomerPhone(),
                        u.getUsedAt()
                ))
                .toList();
    }

    @PostMapping("/zones")
    @PreAuthorize("hasAuthority('PERM_ZONES_WRITE')")
    public ZoneDto createZone(@Valid @RequestBody ZoneRequest request) {
        return zoneService.create(request);
    }

    @DeleteMapping("/zones/{id}")
    @PreAuthorize("hasAuthority('PERM_ZONES_WRITE')")
    public void deleteZone(@PathVariable String id) {
        Long longId = mapper.parseLongId(id);
        if (longId == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "ID zone invalide");
        zoneService.delete(longId);
    }

    @GetMapping("/settings")
    @PreAuthorize("hasAuthority('PERM_SETTINGS_WRITE')")
    public SettingsDto settings() {
        return settingsService.getAdminSettings();
    }

    @PutMapping("/settings")
    @PreAuthorize("hasAuthority('PERM_SETTINGS_WRITE')")
    public SettingsDto updateSettings(@RequestBody SettingsDto dto) {
        return settingsService.update(dto);
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public void changePassword(Authentication auth, @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(auth.getName(), request.currentPassword(), request.newPassword());
    }

    @PostMapping("/devices")
    @PreAuthorize("hasAuthority('PERM_ORDERS_READ')")
    public void registerDevice(Authentication auth, @Valid @RequestBody DeviceRegisterRequest request) {
        deviceTokenService.register(auth.getName(), request);
    }

    @DeleteMapping("/devices")
    @PreAuthorize("hasAuthority('PERM_ORDERS_READ')")
    public void unregisterDevice(@RequestParam String token) {
        deviceTokenService.unregister(token);
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('PERM_USERS_MANAGE')")
    public List<UserDto> listUsers() {
        return userService.findAll();
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('PERM_USERS_MANAGE')")
    public UserDto getUser(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('PERM_USERS_MANAGE')")
    public UserDto createUser(@Valid @RequestBody UserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('PERM_USERS_MANAGE')")
    public UserDto updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('PERM_USERS_MANAGE')")
    public void deleteUser(@PathVariable Long id) {
        userService.delete(id);
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('PERM_USERS_MANAGE')")
    public List<PermissionInfoDto> permissions() {
        return PermissionInfoDto.all();
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('PERM_AUDIT_READ')")
    public AuditPageDto audit(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        var result = auditService.search(username, action, entityType, entityId, platform, search, from, to, page, size);
        var items = result.getContent().stream().map(this::toAuditDto).toList();
        return new AuditPageDto(items, page, size, result.getTotalElements(), result.getTotalPages());
    }

    @GetMapping("/audit/actions")
    @PreAuthorize("hasAuthority('PERM_AUDIT_READ')")
    public List<String> auditActions() {
        return auditService.distinctActions();
    }

    private AuditLogDto toAuditDto(AuditLogEntity e) {
        return new AuditLogDto(
                e.getId(), e.getUserId(), e.getUsername(), e.getAction(),
                e.getEntityType(), e.getEntityId(), e.getDetails(),
                e.getPlatform(), e.getIpAddress(), e.getCreatedAt()
        );
    }
}
