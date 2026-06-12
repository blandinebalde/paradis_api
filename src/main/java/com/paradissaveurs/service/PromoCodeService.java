package com.paradissaveurs.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paradissaveurs.domain.PromoCodeType;
import com.paradissaveurs.dto.PromoCodeDto;
import com.paradissaveurs.dto.PromoCodeRequest;
import com.paradissaveurs.entity.PromoCodeEntity;
import com.paradissaveurs.entity.PromoCodeUsageEntity;
import com.paradissaveurs.repository.PromoCodeRepository;
import com.paradissaveurs.repository.PromoCodeUsageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
public class PromoCodeService {

    private final PromoCodeRepository repository;
    private final PromoCodeUsageRepository usageRepository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public PromoCodeService(PromoCodeRepository repository,
                            PromoCodeUsageRepository usageRepository,
                            ObjectMapper objectMapper,
                            AuditService auditService) {
        this.repository = repository;
        this.usageRepository = usageRepository;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PromoCodeDto> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PromoCodeUsageEntity> findUsages(Long promoCodeId) {
        return usageRepository.findByPromoCodeIdOrderByUsedAtDesc(promoCodeId);
    }

    @Transactional
    public PromoCodeDto create(PromoCodeRequest request) {
        validateRequest(request, null);
        var entity = new PromoCodeEntity();
        applyRequest(entity, request);
        entity.setCode(normalizeCode(request.code()));
        entity.setCreatedAt(Instant.now());
        if (request.active() != null) entity.setActive(request.active());
        var saved = repository.save(entity);
        auditService.log("PROMO_CODE_CREATE", "PROMO_CODE", saved.getId().toString(),
                "Code promo : " + saved.getCode());
        return toDto(saved);
    }

    @Transactional
    public PromoCodeDto update(Long id, PromoCodeRequest request) {
        validateRequest(request, id);
        var entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Code promo introuvable"));
        applyRequest(entity, request);
        entity.setCode(normalizeCode(request.code()));
        if (request.active() != null) entity.setActive(request.active());
        var saved = repository.save(entity);
        auditService.log("PROMO_CODE_UPDATE", "PROMO_CODE", id.toString(), "Code modifié : " + saved.getCode());
        return toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code promo introuvable");
        }
        repository.deleteById(id);
        auditService.log("PROMO_CODE_DELETE", "PROMO_CODE", id.toString(), "Code promo supprimé");
    }

    private void validateRequest(PromoCodeRequest request, Long excludeId) {
        if (!PromoCodeType.isValid(request.discountType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de réduction invalide");
        }
        if (PromoCodeType.FREE_DELIVERY.equals(request.discountType()) && request.discountValue() != null) {
            // discountValue optional for free delivery
        } else if (!PromoCodeType.FREE_DELIVERY.equals(request.discountType())
                && (request.discountValue() == null || request.discountValue() < 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indiquez la valeur de la réduction");
        }
        if (PromoCodeType.PERCENTAGE.equals(request.discountType()) && request.discountValue() != null
                && request.discountValue() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le pourcentage ne peut pas dépasser 100");
        }
        String code = normalizeCode(request.code());
        if (code.length() < 3 || code.length() > 32) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le code doit contenir entre 3 et 32 caractères");
        }
        var existing = repository.findByCodeIgnoreCase(code);
        if (existing.isPresent() && (excludeId == null || !existing.get().getId().equals(excludeId))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce code existe déjà");
        }
        if (request.startDate() != null && request.endDate() != null
                && request.startDate().isAfter(request.endDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La date de début doit être antérieure à la date de fin");
        }
    }

    private void applyRequest(PromoCodeEntity entity, PromoCodeRequest request) {
        entity.setDiscountType(request.discountType());
        entity.setDiscountValue(PromoCodeType.FREE_DELIVERY.equals(request.discountType())
                ? null : request.discountValue());
        entity.setMinOrderAmount(request.minOrderAmount() != null ? request.minOrderAmount() : 0);
        entity.setEligibleProductIdsJson(writeList(request.eligibleProductIds()));
        entity.setEligibleCategoriesJson(writeList(request.eligibleCategories()));
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setMaxUsesTotal(request.maxUsesTotal());
        entity.setMaxUsesPerPhone(request.maxUsesPerPhone());
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private PromoCodeDto toDto(PromoCodeEntity e) {
        return new PromoCodeDto(
                String.valueOf(e.getId()),
                e.getCode(),
                e.getDiscountType(),
                e.getDiscountValue(),
                e.getMinOrderAmount(),
                readStringList(e.getEligibleProductIdsJson()),
                readStringList(e.getEligibleCategoriesJson()),
                e.getStartDate(),
                e.getEndDate(),
                e.getMaxUsesTotal(),
                e.getMaxUsesPerPhone(),
                e.getUsageCount(),
                e.isActive()
        );
    }

    private String writeList(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list != null ? list : Collections.emptyList());
        } catch (Exception ex) {
            return "[]";
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }
}
