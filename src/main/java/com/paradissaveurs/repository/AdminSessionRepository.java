package com.paradissaveurs.repository;

import com.paradissaveurs.entity.AdminSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AdminSessionRepository extends JpaRepository<AdminSessionEntity, Long> {

    Optional<AdminSessionEntity> findByJti(String jti);

    @Modifying
    @Query("UPDATE AdminSessionEntity s SET s.revokedAt = :now WHERE s.jti = :jti AND s.revokedAt IS NULL")
    int revokeByJti(@Param("jti") String jti, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE AdminSessionEntity s SET s.revokedAt = :now WHERE s.userId = :userId AND s.revokedAt IS NULL")
    int revokeAllByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM AdminSessionEntity s WHERE s.expiresAt < :cutoff OR s.revokedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
