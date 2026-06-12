package com.paradissaveurs.service;

import com.paradissaveurs.dto.DeviceRegisterRequest;
import com.paradissaveurs.entity.AdminDeviceTokenEntity;
import com.paradissaveurs.repository.AdminDeviceTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class DeviceTokenService {

    private final AdminDeviceTokenRepository repository;

    public DeviceTokenService(AdminDeviceTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void register(String adminUsername, DeviceRegisterRequest request) {
        var entity = repository.findByToken(request.token())
                .orElseGet(AdminDeviceTokenEntity::new);
        entity.setToken(request.token());
        entity.setPlatform(request.platform() != null ? request.platform() : "unknown");
        entity.setAdminUsername(adminUsername);
        entity.setRegisteredAt(Instant.now());
        repository.save(entity);
    }

    @Transactional
    public void unregister(String token) {
        repository.findByToken(token).ifPresent(repository::delete);
    }
}
