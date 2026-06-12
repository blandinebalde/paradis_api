package com.paradissaveurs.repository;

import com.paradissaveurs.entity.AdminDeviceTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdminDeviceTokenRepository extends JpaRepository<AdminDeviceTokenEntity, Long> {
    Optional<AdminDeviceTokenEntity> findByToken(String token);
}
