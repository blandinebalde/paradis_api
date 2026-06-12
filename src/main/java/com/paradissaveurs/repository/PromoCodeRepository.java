package com.paradissaveurs.repository;

import com.paradissaveurs.entity.PromoCodeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PromoCodeRepository extends JpaRepository<PromoCodeEntity, Long> {
    List<PromoCodeEntity> findAllByOrderByCreatedAtDesc();
    Optional<PromoCodeEntity> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PromoCodeEntity p WHERE UPPER(p.code) = UPPER(:code)")
    Optional<PromoCodeEntity> findByCodeIgnoreCaseForUpdate(@Param("code") String code);
}
