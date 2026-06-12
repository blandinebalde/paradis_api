package com.paradissaveurs.repository;

import com.paradissaveurs.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<OrderEntity, String>, JpaSpecificationExecutor<OrderEntity> {

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM OrderEntity o WHERE o.status = 'delivered'")
    long sumDeliveredRevenue();

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM OrderEntity o WHERE o.status IN ('pending', 'confirmed')")
    long sumForecastRevenue();
}
