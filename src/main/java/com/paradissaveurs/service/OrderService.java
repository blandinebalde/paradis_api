package com.paradissaveurs.service;

import com.paradissaveurs.domain.OrderStatus;
import com.paradissaveurs.dto.CreateOrderRequest;
import com.paradissaveurs.dto.CreateOrderResponse;
import com.paradissaveurs.dto.OrderDto;
import com.paradissaveurs.dto.OrderPageDto;
import com.paradissaveurs.dto.OrderStatsDto;
import com.paradissaveurs.dto.OrderStatusCountsDto;
import com.paradissaveurs.dto.OrderTrackDto;
import com.paradissaveurs.dto.PromoValidateRequest;
import com.paradissaveurs.entity.OrderEntity;
import com.paradissaveurs.entity.OrderItemEntity;
import com.paradissaveurs.entity.ProductEntity;
import com.paradissaveurs.mapper.EntityMapper;
import com.paradissaveurs.util.PhoneUtils;
import com.paradissaveurs.repository.OrderRepository;
import com.paradissaveurs.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final String TRACK_DENIED =
            "Commande introuvable — vérifiez le numéro et le téléphone";

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final SettingsService settingsService;
    private final OrderNotificationService notificationService;
    private final FcmPushService fcmPushService;
    private final EntityMapper mapper;
    private final AuditService auditService;
    private final OrderPricingService pricingService;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                        SettingsService settingsService, OrderNotificationService notificationService,
                        FcmPushService fcmPushService, EntityMapper mapper, AuditService auditService,
                        OrderPricingService pricingService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.settingsService = settingsService;
        this.notificationService = notificationService;
        this.fcmPushService = fcmPushService;
        this.mapper = mapper;
        this.auditService = auditService;
        this.pricingService = pricingService;
    }

    @Transactional(readOnly = true)
    public OrderPageDto findPage(String status, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        var pageable = PageRequest.of(safePage, safeSize, sortForStatus(status));

        Specification<OrderEntity> spec = (root, query, cb) -> {
            if (StringUtils.hasText(status)) {
                return cb.equal(root.get("status"), status.trim());
            }
            return cb.conjunction();
        };

        var result = orderRepository.findAll(spec, pageable);
        var items = result.getContent().stream().map(mapper::toOrderDto).toList();
        return new OrderPageDto(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                statusCounts()
        );
    }

    /** En attente / confirmées : plus anciennes en premier pour traiter sans oubli. */
    private Sort sortForStatus(String status) {
        if (OrderStatus.PENDING.equals(status) || OrderStatus.CONFIRMED.equals(status)) {
            return Sort.by(Sort.Direction.ASC, "createdAt");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    @Transactional(readOnly = true)
    public OrderStatsDto getStats() {
        long total = orderRepository.count();
        long cancelled = orderRepository.countByStatus(OrderStatus.CANCELLED);
        int cancelRate = total == 0 ? 0 : (int) Math.round((cancelled * 100.0) / total);
        return new OrderStatsDto(
                orderRepository.sumDeliveredRevenue(),
                orderRepository.sumForecastRevenue(),
                cancelRate,
                statusCounts()
        );
    }

    private OrderStatusCountsDto statusCounts() {
        return new OrderStatusCountsDto(
                orderRepository.countByStatus(OrderStatus.PENDING),
                orderRepository.countByStatus(OrderStatus.CONFIRMED),
                orderRepository.countByStatus(OrderStatus.DELIVERED),
                orderRepository.countByStatus(OrderStatus.CANCELLED),
                orderRepository.count()
        );
    }

    @Transactional(readOnly = true)
    public List<OrderDto> findRecent(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 500);
        var pageable = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findAll(pageable).getContent().stream()
                .map(mapper::toOrderDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderTrackDto trackOrder(String orderId, String phone) {
        if (orderId == null || orderId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, TRACK_DENIED);
        }
        var order = orderRepository.findById(orderId.trim()).orElse(null);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, TRACK_DENIED);
        }

        if (!PhoneUtils.isValidSenegalese(phone)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, TRACK_DENIED);
        }

        String requestPhone = PhoneUtils.normalize(phone);
        String orderPhone = PhoneUtils.normalize(order.getCustomerPhone());
        if (requestPhone.isBlank() || !requestPhone.equals(orderPhone)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, TRACK_DENIED);
        }
        return mapper.toOrderTrackDto(order);
    }

    @Transactional
    public CreateOrderResponse create(CreateOrderRequest request) {
        String normalizedPhone = validateDeliveryInfo(request);
        settingsService.requireEnabledPaymentMethod(request.paymentMethod());

        var cartItems = request.items().stream()
                .map(i -> new PromoValidateRequest.CartItemRequest(i.productId(), i.quantity()))
                .toList();
        var pricing = pricingService.calculateOrder(
                cartItems,
                request.promoCode(),
                request.customer().phone(),
                request.deliveryMode(),
                request.zoneId()
        );

        int totalQty = pricing.lines().stream().mapToInt(l -> l.quantity()).sum();
        var settings = settingsService.getOrCreate();
        int maxPerOrder = settings.getMaxProductsPerOrder() != null ? settings.getMaxProductsPerOrder() : 20;
        if (totalQty > maxPerOrder) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Maximum " + maxPerOrder + " articles par commande — retirez quelques produits du panier");
        }

        if (pricing.subtotal() != request.subtotal()
                || pricing.total() != request.total()
                || pricing.deliveryFee() != request.deliveryFee()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le total a changé — vérifiez votre panier et réessayez");
        }

        for (var line : pricing.lines()) {
            int updated = productRepository.decrementStockIfAvailable(
                    line.product().getId(), line.quantity());
            if (updated == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stock insuffisant pour " + line.product().getName());
            }
        }

        var order = new OrderEntity();
        order.setId(generateUniqueOrderId());
        order.setCustomerName(request.customer().name() != null ? request.customer().name() : "Client");
        order.setCustomerPhone(normalizedPhone);
        order.setCustomerAddress(request.customer().address());
        order.setDeliveryMode(request.deliveryMode());
        order.setZoneId(mapper.parseLongId(request.zoneId()));
        order.setPaymentMethod(request.paymentMethod());
        order.setDeliveryFee(pricing.deliveryFee());
        order.setSubtotalBeforeDiscount(pricing.subtotalBeforePromoCode());
        order.setDiscountAmount(pricing.promoCodeDiscount());
        order.setSubtotal(pricing.subtotal());
        order.setTotal(pricing.total());
        order.setPromoCode(pricing.promoCode());
        order.setStatus(OrderStatus.PENDING);
        order.setNotes(request.notes());
        order.setCreatedAt(Instant.now());

        for (var line : pricing.lines()) {
            var product = line.product();
            var orderItem = new OrderItemEntity();
            orderItem.setOrder(order);
            orderItem.setProductId(product.getId());
            orderItem.setName(product.getName());
            orderItem.setEmoji(product.getEmoji());
            orderItem.setImageUrl(product.getImageUrl());
            orderItem.setOriginalPrice(line.hasProductPromo() ? line.catalogPrice() : null);
            orderItem.setPrice(line.effectivePrice());
            orderItem.setQuantity(line.quantity());
            order.getItems().add(orderItem);
        }

        var saved = orderRepository.save(order);

        if (StringUtils.hasText(pricing.promoCode())) {
            pricingService.recordPromoUsageLocked(
                    pricing.promoCode(), saved.getId(), saved.getCustomerPhone());
        }

        var notif = notificationService.notifyAdmin(saved, settings);
        fcmPushService.notifyNewOrder(saved);
        return new CreateOrderResponse(
                mapper.toOrderDto(saved),
                notif.sent(),
                notif.channel(),
                buildSuccessMessage(saved.getId(), notif.sent())
        );
    }

    private String validateDeliveryInfo(CreateOrderRequest request) {
        String normalizedPhone = PhoneUtils.requireValidSenegalese(request.customer().phone());

        if ("delivery".equals(request.deliveryMode())) {
            if (!StringUtils.hasText(request.zoneId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Choisissez une zone de livraison");
            }
            if (!StringUtils.hasText(request.customer().address())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Indiquez votre adresse complète pour la livraison");
            }
            pricingService.validateDeliveryZone(request.zoneId());
        } else if ("pickup".equals(request.deliveryMode())) {
            // collecte — pas de zone requise
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Mode de récupération invalide — choisissez livraison ou collecte");
        }
        return normalizedPhone;
    }

    private String generateUniqueOrderId() {
        for (int attempt = 0; attempt < 8; attempt++) {
            String id = "CMD-" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 10).toUpperCase();
            if (!orderRepository.existsById(id)) {
                return id;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Impossible de créer la commande — réessayez");
    }

    private String buildSuccessMessage(String orderId, boolean adminNotified) {
        if (adminNotified) {
            return "Commande " + orderId + " enregistrée ! La boutique a été prévenue et vous contactera bientôt.";
        }
        return "Commande " + orderId + " enregistrée ! Nous vous contacterons très bientôt pour confirmer.";
    }

    @Transactional
    public OrderDto updateStatus(String id, String newStatus) {
        if (!OrderStatus.isValid(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut invalide");
        }

        var order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable"));

        String current = order.getStatus();
        if (current.equals(newStatus)) {
            return mapper.toOrderDto(order);
        }

        if (OrderStatus.isFinal(current)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Impossible de modifier une commande « " + OrderStatus.label(current)
                            + " » — état définitif");
        }

        if (!OrderStatus.canTransition(current, newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Transition interdite : « " + OrderStatus.label(current)
                            + " » ne peut pas passer à « " + OrderStatus.label(newStatus) + " »");
        }

        if (OrderStatus.CANCELLED.equals(newStatus)) {
            restoreStock(order);
        }

        if (OrderStatus.DELIVERED.equals(newStatus)) {
            order.setDeliveredAt(Instant.now());
        }

        order.setStatus(newStatus);
        var saved = orderRepository.save(order);

        String auditDetail = OrderStatus.label(current) + " → " + OrderStatus.label(newStatus);
        if (OrderStatus.CANCELLED.equals(newStatus)) {
            auditDetail += " — stock restauré";
        }
        auditService.log("ORDER_STATUS", "ORDER", id, auditDetail);

        return mapper.toOrderDto(saved);
    }

    private void restoreStock(OrderEntity order) {
        for (OrderItemEntity item : order.getItems()) {
            if (item.getProductId() == null) continue;
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            });
        }
    }
}
