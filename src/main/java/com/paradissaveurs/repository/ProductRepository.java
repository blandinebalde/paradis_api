package com.paradissaveurs.repository;

import com.paradissaveurs.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    List<ProductEntity> findAllByOrderByNameAsc();

    List<ProductEntity> findAllByActiveTrueOrderByFeaturedDescNameAsc();

    long countByCategory(String category);

    long countByCategoryAndActiveTrue(String category);

    @Modifying
    @Query("UPDATE ProductEntity p SET p.category = :newCat WHERE p.category = :oldCat")
    void reassignCategory(@Param("oldCat") String oldCat, @Param("newCat") String newCat);

    /** Décrémente le stock uniquement si disponible (atomique). Retourne 1 si OK, 0 sinon. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductEntity p SET p.stock = p.stock - :qty WHERE p.id = :id AND p.active = true AND p.stock >= :qty")
    int decrementStockIfAvailable(@Param("id") Long id, @Param("qty") int qty);
}
