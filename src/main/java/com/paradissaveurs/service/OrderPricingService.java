package com.paradissaveurs.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paradissaveurs.domain.DiscountType;
import com.paradissaveurs.domain.PromoCodeType;
import com.paradissaveurs.domain.PromotionScope;
import com.paradissaveurs.dto.*;
import com.paradissaveurs.entity.ProductEntity;
import com.paradissaveurs.entity.ProductPromotionEntity;
import com.paradissaveurs.entity.PromoCodeEntity;
import com.paradissaveurs.repository.ProductPromotionRepository;
import com.paradissaveurs.repository.ProductRepository;
import com.paradissaveurs.repository.PromoCodeRepository;
import com.paradissaveurs.repository.PromoCodeUsageRepository;
import com.paradissaveurs.util.CategoryMatcher;
import com.paradissaveurs.util.PhoneUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class OrderPricingService {

    /** Message unique côté client — évite l'énumération de codes promo. */
    public static final String PROMO_REJECTED_MESSAGE = "Code promo invalide ou non applicable";

    private final ProductRepository productRepository;
    private final ProductPromotionRepository promotionRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeUsageRepository promoCodeUsageRepository;
    private final ZoneService zoneService;
    private final ObjectMapper objectMapper;

    public OrderPricingService(ProductRepository productRepository,
                               ProductPromotionRepository promotionRepository,
                               PromoCodeRepository promoCodeRepository,
                               PromoCodeUsageRepository promoCodeUsageRepository,
                               ZoneService zoneService,
                               ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.promotionRepository = promotionRepository;
        this.promoCodeRepository = promoCodeRepository;
        this.promoCodeUsageRepository = promoCodeUsageRepository;
        this.zoneService = zoneService;
        this.objectMapper = objectMapper;
    }

    public record EffectivePrice(int catalogPrice, int effectivePrice, boolean onPromo, String promoScope) {}

    @Transactional(readOnly = true)
    public EffectivePrice getEffectivePrice(ProductEntity product, List<ProductPromotionEntity> activePromos) {
        int catalog = product.getPrice() != null ? product.getPrice() : 0;
        var best = findBestPromotion(product, activePromos);
        if (best == null) {
            return new EffectivePrice(catalog, catalog, false, null);
        }
        int effective = applyDiscount(catalog, best.getDiscountType(), best.getDiscountValue());
        if (effective >= catalog) {
            return new EffectivePrice(catalog, catalog, false, null);
        }
        return new EffectivePrice(catalog, effective, true, best.getScope());
    }

    @Transactional(readOnly = true)
    public List<ProductPromotionEntity> getActivePromotions() {
        Instant now = Instant.now();
        return promotionRepository.findAllByActiveTrue().stream()
                .filter(p -> isWithinDates(p.getStartDate(), p.getEndDate(), now))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderPricingResult calculateOrder(List<PromoValidateRequest.CartItemRequest> items,
                                             String promoCode,
                                             String phone,
                                             String deliveryMode,
                                             String zoneId) {
        var activePromos = getActivePromotions();
        var lines = buildLines(items, activePromos);
        boolean hasProductPromo = lines.stream().anyMatch(PricedLineItem::hasProductPromo);
        int subtotalBeforePromoCode = lines.stream().mapToInt(PricedLineItem::lineTotal).sum();

        int promoDiscount = 0;
        String appliedCode = null;
        boolean freeDelivery = false;

        if (StringUtils.hasText(promoCode)) {
            if (hasProductPromo) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ce code ne peut pas être combiné avec des promotions produits en cours");
            }
            var codeEntity = validatePromoCode(promoCode.trim(), phone, lines, subtotalBeforePromoCode);
            appliedCode = codeEntity.getCode();
            var eligibleSubtotal = eligibleSubtotal(codeEntity, lines);
            promoDiscount = computeCodeDiscount(codeEntity, eligibleSubtotal);
            freeDelivery = PromoCodeType.FREE_DELIVERY.equals(codeEntity.getDiscountType());
        }

        int subtotal = Math.max(0, subtotalBeforePromoCode - promoDiscount);
        int deliveryFee = resolveDeliveryFee(deliveryMode, zoneId, freeDelivery);
        int total = subtotal + deliveryFee;

        return new OrderPricingResult(
                lines, subtotalBeforePromoCode, promoDiscount, subtotal,
                deliveryFee, total, appliedCode, freeDelivery, hasProductPromo
        );
    }

    @Transactional(readOnly = true)
    public OrderQuoteResponse quoteOrder(OrderQuoteRequest request) {
        var cartItems = request.items().stream()
                .map(i -> new PromoValidateRequest.CartItemRequest(i.productId(), i.quantity()))
                .toList();
        var pricing = calculateOrder(
                cartItems,
                request.promoCode(),
                request.phone(),
                request.deliveryMode(),
                request.zoneId()
        );
        int originalSubtotal = pricing.lines().stream()
                .mapToInt(l -> l.catalogPrice() * l.quantity())
                .sum();
        int productPromoSavings = pricing.lines().stream()
                .mapToInt(l -> (l.catalogPrice() - l.effectivePrice()) * l.quantity())
                .sum();
        return new OrderQuoteResponse(
                originalSubtotal,
                productPromoSavings,
                pricing.subtotalBeforePromoCode(),
                pricing.promoCodeDiscount(),
                pricing.subtotal(),
                pricing.deliveryFee(),
                pricing.total(),
                pricing.hasProductPromo(),
                pricing.freeDelivery(),
                pricing.promoCode()
        );
    }

    @Transactional(readOnly = true)
    public PromoValidateResponse validatePromo(PromoValidateRequest request) {
        try {
            var result = calculateOrder(
                    request.items(),
                    request.code(),
                    request.phone(),
                    request.deliveryMode(),
                    request.zoneId()
            );
            var code = promoCodeRepository.findByCodeIgnoreCase(request.code().trim()).orElse(null);
            return new PromoValidateResponse(
                    true,
                    "Code promo appliqué",
                    code != null ? code.getCode() : request.code().trim().toUpperCase(),
                    code != null ? code.getDiscountType() : null,
                    result.promoCodeDiscount(),
                    result.subtotalBeforePromoCode(),
                    result.subtotal(),
                    result.deliveryFee(),
                    result.total(),
                    result.freeDelivery()
            );
        } catch (ResponseStatusException ex) {
            return new PromoValidateResponse(
                    false,
                    PROMO_REJECTED_MESSAGE,
                    null, null, 0, 0, 0, 0, 0, false
            );
        }
    }

    @Transactional
    public void recordPromoUsage(PromoCodeEntity code, String orderId, String phone) {
        recordPromoUsageLocked(code.getCode(), orderId, phone);
    }

    /**
     * Enregistre l'utilisation d'un code promo avec verrou pessimiste
     * (re-vérifie les limites sous transaction de commande).
     */
    @Transactional
    public void recordPromoUsageLocked(String code, String orderId, String phone) {
        if (!StringUtils.hasText(code)) return;
        var entity = promoCodeRepository.findByCodeIgnoreCaseForUpdate(code.trim())
                .orElseThrow(() -> promoRejected());

        if (entity.getMaxUsesTotal() != null && entity.getUsageCount() >= entity.getMaxUsesTotal()) {
            throw promoRejected();
        }

        String normalizedPhone = PhoneUtils.normalize(phone);
        if (entity.getMaxUsesPerPhone() != null && !normalizedPhone.isBlank()) {
            long usedByPhone = promoCodeUsageRepository.countByPromoCodeIdAndCustomerPhone(
                    entity.getId(), normalizedPhone);
            if (usedByPhone >= entity.getMaxUsesPerPhone()) {
                throw promoRejected();
            }
        }

        var usage = new com.paradissaveurs.entity.PromoCodeUsageEntity();
        usage.setPromoCodeId(entity.getId());
        usage.setOrderId(orderId);
        usage.setCustomerPhone(normalizedPhone);
        usage.setUsedAt(Instant.now());
        promoCodeUsageRepository.save(usage);
        entity.setUsageCount(entity.getUsageCount() + 1);
        promoCodeRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public PromoCodeEntity findPromoCodeForOrder(String code) {
        if (!StringUtils.hasText(code)) return null;
        return promoCodeRepository.findByCodeIgnoreCase(code.trim()).orElse(null);
    }

    /** Vérifie qu'une zone de livraison existe (utilisé avant création commande). */
    public void validateDeliveryZone(String zoneId) {
        zoneService.requireDeliveryFee(zoneId);
    }

    private List<PricedLineItem> buildLines(List<PromoValidateRequest.CartItemRequest> items,
                                            List<ProductPromotionEntity> activePromos) {
        List<PromoValidateRequest.CartItemRequest> merged = mergeCartItems(items);
        List<PricedLineItem> lines = new ArrayList<>();
        for (var item : merged) {
            Long productId = parseLongId(item.productId());
            if (productId == null || item.quantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produit invalide dans le panier");
            }
            var product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produit introuvable"));
            if (!product.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Le produit « " + product.getName() + " » n'est plus disponible");
            }
            var ep = getEffectivePrice(product, activePromos);
            lines.add(new PricedLineItem(product, item.quantity(), ep.catalogPrice(), ep.effectivePrice(), ep.onPromo()));
        }
        return lines;
    }

    /** Fusionne les lignes dupliquées (même productId) pour pricing et stock cohérents. */
    private List<PromoValidateRequest.CartItemRequest> mergeCartItems(
            List<PromoValidateRequest.CartItemRequest> items) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        Map<Long, String> productIds = new LinkedHashMap<>();
        for (var item : items) {
            Long productId = parseLongId(item.productId());
            if (productId == null || item.quantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produit invalide dans le panier");
            }
            quantities.merge(productId, item.quantity(), Integer::sum);
            productIds.putIfAbsent(productId, item.productId());
        }
        return quantities.entrySet().stream()
                .map(e -> new PromoValidateRequest.CartItemRequest(productIds.get(e.getKey()), e.getValue()))
                .toList();
    }

    private PromoCodeEntity validatePromoCode(String code, String phone, List<PricedLineItem> lines, int subtotal) {
        var entity = promoCodeRepository.findByCodeIgnoreCase(code)
                .orElseThrow(this::promoRejected);

        if (!entity.isActive()) {
            throw promoRejected();
        }

        Instant now = Instant.now();
        if (entity.getStartDate() != null && now.isBefore(entity.getStartDate())) {
            throw promoRejected();
        }
        if (entity.getEndDate() != null && now.isAfter(entity.getEndDate())) {
            throw promoRejected();
        }

        int minAmount = entity.getMinOrderAmount() != null ? entity.getMinOrderAmount() : 0;
        if (subtotal < minAmount) {
            throw promoRejected();
        }

        if (entity.getMaxUsesTotal() != null && entity.getUsageCount() >= entity.getMaxUsesTotal()) {
            throw promoRejected();
        }

        String normalizedPhone = PhoneUtils.normalize(phone);
        if (entity.getMaxUsesPerPhone() != null && !normalizedPhone.isBlank()) {
            long usedByPhone = promoCodeUsageRepository.countByPromoCodeIdAndCustomerPhone(entity.getId(), normalizedPhone);
            if (usedByPhone >= entity.getMaxUsesPerPhone()) {
                throw promoRejected();
            }
        }

        var eligibleSubtotal = eligibleSubtotal(entity, lines);
        if (eligibleSubtotal <= 0 && hasEligibilityRestrictions(entity)) {
            throw promoRejected();
        }

        return entity;
    }

    private ResponseStatusException promoRejected() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, PROMO_REJECTED_MESSAGE);
    }

    private int eligibleSubtotal(PromoCodeEntity code, List<PricedLineItem> lines) {
        var productIds = parseIdList(code.getEligibleProductIdsJson());
        var categories = parseStringList(code.getEligibleCategoriesJson());
        if (productIds.isEmpty() && categories.isEmpty()) {
            return lines.stream().mapToInt(PricedLineItem::lineTotal).sum();
        }
        return lines.stream()
                .filter(line -> {
                    Long pid = line.product().getId();
                    if (!productIds.isEmpty() && productIds.contains(pid)) return true;
                    return !categories.isEmpty() && categories.stream()
                            .anyMatch(cat -> CategoryMatcher.matches(cat, line.product().getCategory()));
                })
                .mapToInt(PricedLineItem::lineTotal)
                .sum();
    }

    private boolean hasEligibilityRestrictions(PromoCodeEntity code) {
        return !parseIdList(code.getEligibleProductIdsJson()).isEmpty()
                || !parseStringList(code.getEligibleCategoriesJson()).isEmpty();
    }

    private int computeCodeDiscount(PromoCodeEntity code, int eligibleSubtotal) {
        if (eligibleSubtotal <= 0) return 0;
        return switch (code.getDiscountType()) {
            case PromoCodeType.PERCENTAGE -> {
                int pct = code.getDiscountValue() != null ? code.getDiscountValue() : 0;
                yield Math.min(eligibleSubtotal, eligibleSubtotal * pct / 100);
            }
            case PromoCodeType.FIXED -> {
                int fixed = code.getDiscountValue() != null ? code.getDiscountValue() : 0;
                yield Math.min(eligibleSubtotal, fixed);
            }
            case PromoCodeType.FREE_DELIVERY -> 0;
            default -> 0;
        };
    }

    private int resolveDeliveryFee(String deliveryMode, String zoneId, boolean freeDelivery) {
        if (freeDelivery || !"delivery".equals(deliveryMode)) return 0;
        return zoneService.requireDeliveryFee(zoneId);
    }

    private ProductPromotionEntity findBestPromotion(ProductEntity product, List<ProductPromotionEntity> promos) {
        ProductPromotionEntity best = null;
        int bestPrice = Integer.MAX_VALUE;
        int catalog = product.getPrice() != null ? product.getPrice() : 0;

        for (var promo : promos) {
            if (!matchesProduct(promo, product)) continue;
            int price = applyDiscount(catalog, promo.getDiscountType(), promo.getDiscountValue());
            if (price < bestPrice) {
                bestPrice = price;
                best = promo;
            }
        }
        return best;
    }

    private boolean matchesProduct(ProductPromotionEntity promo, ProductEntity product) {
        if (PromotionScope.PRODUCT.equals(promo.getScope())) {
            return promo.getProductId() != null && promo.getProductId().equals(product.getId());
        }
        if (PromotionScope.CATEGORY.equals(promo.getScope())) {
            return CategoryMatcher.matches(promo.getCategory(), product.getCategory());
        }
        return false;
    }

    public int applyDiscount(int catalogPrice, String discountType, Integer discountValue) {
        if (catalogPrice <= 0 || discountValue == null || discountValue <= 0) return catalogPrice;
        if (DiscountType.PERCENTAGE.equals(discountType)) {
            int pct = Math.min(discountValue, 100);
            return Math.max(0, catalogPrice - (catalogPrice * pct / 100));
        }
        if (DiscountType.FIXED.equals(discountType)) {
            return Math.max(0, catalogPrice - discountValue);
        }
        return catalogPrice;
    }

    private boolean isWithinDates(Instant start, Instant end, Instant now) {
        if (start != null && now.isBefore(start)) return false;
        if (end != null && now.isAfter(end)) return false;
        return true;
    }

    /** @deprecated Préférer {@link PhoneUtils#normalize(String)} */
    @Deprecated
    public static String normalizePhone(String phone) {
        return PhoneUtils.normalize(phone);
    }

    private Long parseLongId(String id) {
        if (id == null || id.isBlank()) return null;
        try {
            return Long.parseLong(id.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<Long> parseIdList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<String> raw = objectMapper.readValue(json, new TypeReference<>() {});
            return raw.stream().map(this::parseLongId).filter(Objects::nonNull).toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }
}
