package com.paradissaveurs.repository;

import com.paradissaveurs.entity.ProductPromotionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductPromotionRepository extends JpaRepository<ProductPromotionEntity, Long> {
    List<ProductPromotionEntity> findAllByOrderByCreatedAtDesc();
    List<ProductPromotionEntity> findAllByActiveTrue();

    @Modifying
    @Query("UPDATE ProductPromotionEntity p SET p.category = :newName WHERE p.category = :oldName")
    void reassignCategory(@Param("oldName") String oldName, @Param("newName") String newName);
}
