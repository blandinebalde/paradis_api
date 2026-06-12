package com.paradissaveurs.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "promo_code_usages")
public class PromoCodeUsageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long promoCodeId;
    private String orderId;
    private String customerPhone;
    private Instant usedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPromoCodeId() { return promoCodeId; }
    public void setPromoCodeId(Long promoCodeId) { this.promoCodeId = promoCodeId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
}
