package com.paradissaveurs.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paradissaveurs.dto.*;
import com.paradissaveurs.entity.*;
import com.paradissaveurs.repository.AdminUserRepository;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class EntityMapper {

    private final ObjectMapper objectMapper;
    private final AdminUserRepository adminUserRepository;

    public EntityMapper(ObjectMapper objectMapper, AdminUserRepository adminUserRepository) {
        this.objectMapper = objectMapper;
        this.adminUserRepository = adminUserRepository;
    }

    public ProductDto toProductDto(ProductEntity e) {
        return toProductDto(e, e.getPrice(), null, false, null);
    }

    public ProductDto toProductDto(ProductEntity e, int effectivePrice, Integer originalPrice,
                                   boolean onPromo, String promoScope) {
        return new ProductDto(
                String.valueOf(e.getId()),
                e.getName(),
                e.getCategory(),
                effectivePrice,
                e.getStock(),
                e.getEmoji(),
                e.getDescription(),
                e.getImageUrl(),
                e.isActive(),
                e.isFeatured(),
                originalPrice,
                onPromo,
                onPromo ? promoScope : null
        );
    }

    public ZoneDto toZoneDto(DeliveryZoneEntity e) {
        return new ZoneDto(String.valueOf(e.getId()), e.getName(), e.getFee());
    }

    public SettingsDto toSettingsDto(ShopSettingsEntity e, boolean includeAdminUser) {
        return new SettingsDto(
                e.getMaxProductsPerOrder(),
                e.getShopName(),
                e.getSlogan(),
                e.getShopAddress(),
                e.getShopPhone(),
                e.getWaveNumber(),
                e.getOmNumber(),
                e.getAdminNotifyPhone(),
                e.getNotifyViaWhatsApp(),
                e.getNotifyViaSms(),
                e.getWave(),
                e.getOm(),
                e.getCash(),
                parseCategories(e.getCategoriesJson()),
                includeAdminUser ? adminUserRepository.findAll().stream().findFirst()
                        .map(AdminUserEntity::getUsername).orElse("admin") : null
        );
    }

    public OrderDto toOrderDto(OrderEntity e) {
        List<OrderItemDto> items = e.getItems().stream()
                .map(i -> new OrderItemDto(
                        i.getProductId() != null ? String.valueOf(i.getProductId()) : null,
                        i.getName(),
                        i.getEmoji(),
                        i.getImageUrl(),
                        i.getOriginalPrice(),
                        i.getPrice(),
                        i.getQuantity()
                ))
                .toList();

        return new OrderDto(
                e.getId(),
                new OrderDto.CustomerDto(e.getCustomerName(), e.getCustomerPhone(), e.getCustomerAddress()),
                e.getDeliveryMode(),
                e.getZoneId() != null ? String.valueOf(e.getZoneId()) : null,
                e.getPaymentMethod(),
                e.getDeliveryFee(),
                e.getSubtotalBeforeDiscount(),
                e.getDiscountAmount(),
                e.getSubtotal(),
                e.getTotal(),
                e.getPromoCode(),
                e.getStatus(),
                e.getNotes(),
                e.getCreatedAt(),
                e.getDeliveredAt(),
                items
        );
    }

    public OrderTrackDto toOrderTrackDto(OrderEntity e) {
        List<OrderItemDto> items = e.getItems().stream()
                .map(i -> new OrderItemDto(
                        i.getProductId() != null ? String.valueOf(i.getProductId()) : null,
                        i.getName(),
                        i.getEmoji(),
                        i.getImageUrl(),
                        i.getOriginalPrice(),
                        i.getPrice(),
                        i.getQuantity()
                ))
                .toList();

        return new OrderTrackDto(
                e.getId(),
                e.getStatus(),
                e.getDeliveryMode(),
                e.getPaymentMethod(),
                e.getDeliveryFee(),
                e.getSubtotal(),
                e.getTotal(),
                e.getCreatedAt(),
                e.getDeliveredAt(),
                items
        );
    }

    public void applyProductRequest(ProductEntity e, ProductRequest req) {
        e.setName(req.name().trim());
        e.setCategory(req.category().trim());
        e.setPrice(req.price());
        e.setStock(req.stock());
        e.setEmoji(req.emoji());
        e.setDescription(req.description());
        if (req.imageUrl() != null) {
            e.setImageUrl(req.imageUrl());
        }
        if (req.active() != null) {
            e.setActive(req.active());
        }
        if (req.featured() != null) {
            e.setFeatured(req.featured());
        }
    }

    public void applySettings(ShopSettingsEntity e, SettingsDto dto) {
        if (dto.maxProductsPerOrder() != null) e.setMaxProductsPerOrder(dto.maxProductsPerOrder());
        if (dto.shopName() != null) e.setShopName(dto.shopName());
        if (dto.slogan() != null) e.setSlogan(dto.slogan());
        if (dto.shopAddress() != null) e.setShopAddress(dto.shopAddress());
        if (dto.shopPhone() != null) e.setShopPhone(dto.shopPhone());
        if (dto.waveNumber() != null) e.setWaveNumber(dto.waveNumber());
        if (dto.omNumber() != null) e.setOmNumber(dto.omNumber());
        if (dto.adminNotifyPhone() != null) e.setAdminNotifyPhone(dto.adminNotifyPhone());
        if (dto.notifyViaWhatsApp() != null) e.setNotifyViaWhatsApp(dto.notifyViaWhatsApp());
        if (dto.notifyViaSms() != null) e.setNotifyViaSms(dto.notifyViaSms());
        if (dto.wave() != null) e.setWave(dto.wave());
        if (dto.om() != null) e.setOm(dto.om());
        if (dto.cash() != null) e.setCash(dto.cash());
        if (dto.categories() != null) e.setCategoriesJson(writeCategories(dto.categories()));
    }

    public List<String> parseCategories(String json) {
        if (json == null || json.isBlank()) {
            return List.of("Crêpes", "Fast-food", "Jus naturels", "Smoothies");
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return List.of("Crêpes", "Fast-food", "Jus naturels", "Smoothies");
        }
    }

    public String writeCategories(List<String> categories) {
        try {
            return objectMapper.writeValueAsString(categories != null ? categories : Collections.emptyList());
        } catch (Exception ex) {
            return "[]";
        }
    }

    public Long parseLongId(String id) {
        if (id == null || id.isBlank()) return null;
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
