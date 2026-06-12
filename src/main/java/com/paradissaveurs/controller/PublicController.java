package com.paradissaveurs.controller;

import com.paradissaveurs.dto.*;
import com.paradissaveurs.mapper.EntityMapper;
import com.paradissaveurs.service.*;
import com.paradissaveurs.util.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PublicController {

    private final ProductService productService;
    private final ZoneService zoneService;
    private final SettingsService settingsService;
    private final OrderService orderService;
    private final AuthService authService;
    private final OrderPricingService pricingService;
    private final ProductPromotionService productPromotionService;
    private final CategoryService categoryService;
    private final RateLimitService rateLimitService;

    public PublicController(ProductService productService, ZoneService zoneService,
                            SettingsService settingsService, OrderService orderService,
                            AuthService authService, OrderPricingService pricingService,
                            ProductPromotionService productPromotionService,
                            CategoryService categoryService,
                            RateLimitService rateLimitService) {
        this.productService = productService;
        this.zoneService = zoneService;
        this.settingsService = settingsService;
        this.orderService = orderService;
        this.authService = authService;
        this.pricingService = pricingService;
        this.productPromotionService = productPromotionService;
        this.categoryService = categoryService;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/products")
    public List<ProductDto> products() {
        return productService.findAllPublic();
    }

    @GetMapping("/promotions/public")
    public List<PublicPromotionDto> publicPromotions() {
        return productPromotionService.findActivePublic();
    }

    @GetMapping("/zones")
    public List<ZoneDto> zones() {
        return zoneService.findAll();
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return categoryService.findAllPublic();
    }

    @GetMapping("/settings/public")
    public SettingsDto publicSettings() {
        return settingsService.getPublicSettings();
    }

    @PostMapping("/orders/track")
    public OrderTrackDto trackOrder(@Valid @RequestBody TrackOrderRequest request,
                                     HttpServletRequest http) {
        rateLimitService.checkLimit("track:" + ClientIpUtils.resolve(http), 15, Duration.ofMinutes(1));
        return orderService.trackOrder(request.orderId(), request.phone());
    }

    @PostMapping("/orders/quote")
    public OrderQuoteResponse quoteOrder(@Valid @RequestBody OrderQuoteRequest request,
                                         HttpServletRequest http) {
        rateLimitService.checkLimit("quote:" + ClientIpUtils.resolve(http), 30, Duration.ofMinutes(1));
        return pricingService.quoteOrder(request);
    }

    @PostMapping("/orders")
    public CreateOrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request,
                                           HttpServletRequest http) {
        rateLimitService.checkLimit("order:" + ClientIpUtils.resolve(http), 10, Duration.ofMinutes(1));
        return orderService.create(request);
    }

    @PostMapping("/promo/validate")
    public PromoValidateResponse validatePromo(@Valid @RequestBody PromoValidateRequest request,
                                               HttpServletRequest http) {
        rateLimitService.checkLimit("promo:" + ClientIpUtils.resolve(http), 20, Duration.ofMinutes(1));
        return pricingService.validatePromo(request);
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        rateLimitService.checkLimit("login:" + ClientIpUtils.resolve(http), 5, Duration.ofMinutes(1));
        return authService.login(request);
    }
}
