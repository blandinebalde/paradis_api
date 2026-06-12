package com.paradissaveurs.repository;

import com.paradissaveurs.entity.DeliveryZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeliveryZoneRepository extends JpaRepository<DeliveryZoneEntity, Long> {
    List<DeliveryZoneEntity> findAllByOrderByNameAsc();
}
