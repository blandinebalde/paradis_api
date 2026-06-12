package com.paradissaveurs.repository;

import com.paradissaveurs.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {
    boolean existsByProductId(Long productId);
}
