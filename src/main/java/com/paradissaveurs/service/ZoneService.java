package com.paradissaveurs.service;

import com.paradissaveurs.dto.ZoneDto;
import com.paradissaveurs.dto.ZoneRequest;
import com.paradissaveurs.entity.DeliveryZoneEntity;
import com.paradissaveurs.mapper.EntityMapper;
import com.paradissaveurs.repository.DeliveryZoneRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ZoneService {

    private final DeliveryZoneRepository zoneRepository;
    private final EntityMapper mapper;
    private final AuditService auditService;

    public ZoneService(DeliveryZoneRepository zoneRepository, EntityMapper mapper, AuditService auditService) {
        this.zoneRepository = zoneRepository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    public List<ZoneDto> findAll() {
        return zoneRepository.findAllByOrderByNameAsc().stream()
                .map(mapper::toZoneDto)
                .toList();
    }

    /** Frais de livraison pour une zone valide — rejette les identifiants inconnus. */
    public int requireDeliveryFee(String zoneId) {
        Long zid = parseZoneId(zoneId);
        if (zid == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zone de livraison invalide");
        }
        return zoneRepository.findById(zid)
                .map(z -> z.getFee() != null ? z.getFee() : 0)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Zone de livraison introuvable"));
    }

    private Long parseZoneId(String zoneId) {
        if (zoneId == null || zoneId.isBlank()) return null;
        try {
            return Long.parseLong(zoneId.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public ZoneDto create(ZoneRequest request) {
        var entity = new DeliveryZoneEntity();
        entity.setName(request.name());
        entity.setFee(request.fee());
        var saved = zoneRepository.save(entity);
        auditService.log("ZONE_CREATE", "ZONE", saved.getId().toString(), "Zone créée : " + saved.getName());
        return mapper.toZoneDto(saved);
    }

    public void delete(Long id) {
        var entity = zoneRepository.findById(id).orElse(null);
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Zone introuvable");
        }
        zoneRepository.deleteById(id);
        auditService.log("ZONE_DELETE", "ZONE", id.toString(), "Zone supprimée : " + entity.getName());
    }
}
