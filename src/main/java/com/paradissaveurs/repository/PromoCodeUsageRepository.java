package com.paradissaveurs.repository;

import com.paradissaveurs.entity.PromoCodeUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromoCodeUsageRepository extends JpaRepository<PromoCodeUsageEntity, Long> {
    long countByPromoCodeId(Long promoCodeId);
    long countByPromoCodeIdAndCustomerPhone(Long promoCodeId, String customerPhone);
    List<PromoCodeUsageEntity> findByPromoCodeIdOrderByUsedAtDesc(Long promoCodeId);
}
