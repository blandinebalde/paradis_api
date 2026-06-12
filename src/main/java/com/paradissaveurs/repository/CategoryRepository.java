package com.paradissaveurs.repository;

import com.paradissaveurs.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
    List<CategoryEntity> findAllByOrderBySortOrderAscNameAsc();
    List<CategoryEntity> findAllByActiveTrueOrderBySortOrderAscNameAsc();
    Optional<CategoryEntity> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    long countByNameIgnoreCase(String name);

    @Modifying
    @Query("UPDATE ProductEntity p SET p.category = :newName WHERE p.category = :oldName")
    void reassignProducts(@Param("oldName") String oldName, @Param("newName") String newName);
}
