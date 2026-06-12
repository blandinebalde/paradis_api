package com.paradissaveurs.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "promo_codes", indexes = @Index(columnList = "code", unique = true))
public class PromoCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String code;

    private String discountType;
    private Integer discountValue;
    private Integer minOrderAmount;
    private String eligibleProductIdsJson;
    private String eligibleCategoriesJson;
    private Instant startDate;
    private Instant endDate;
    private Integer maxUsesTotal;
    private Integer maxUsesPerPhone;
    private int usageCount;
    private boolean active = true;
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
    public Integer getDiscountValue() { return discountValue; }
    public void setDiscountValue(Integer discountValue) { this.discountValue = discountValue; }
    public Integer getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(Integer minOrderAmount) { this.minOrderAmount = minOrderAmount; }
    public String getEligibleProductIdsJson() { return eligibleProductIdsJson; }
    public void setEligibleProductIdsJson(String eligibleProductIdsJson) { this.eligibleProductIdsJson = eligibleProductIdsJson; }
    public String getEligibleCategoriesJson() { return eligibleCategoriesJson; }
    public void setEligibleCategoriesJson(String eligibleCategoriesJson) { this.eligibleCategoriesJson = eligibleCategoriesJson; }
    public Instant getStartDate() { return startDate; }
    public void setStartDate(Instant startDate) { this.startDate = startDate; }
    public Instant getEndDate() { return endDate; }
    public void setEndDate(Instant endDate) { this.endDate = endDate; }
    public Integer getMaxUsesTotal() { return maxUsesTotal; }
    public void setMaxUsesTotal(Integer maxUsesTotal) { this.maxUsesTotal = maxUsesTotal; }
    public Integer getMaxUsesPerPhone() { return maxUsesPerPhone; }
    public void setMaxUsesPerPhone(Integer maxUsesPerPhone) { this.maxUsesPerPhone = maxUsesPerPhone; }
    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
