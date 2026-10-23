package com.garv.gopay.merchant.repositories;

import com.garv.gopay.merchant.entities.RefreshToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @EntityGraph(attributePaths = {"appUser", "appUser.merchant"})
    Optional<RefreshToken> findByTokenId(String tokenId);

    // defensive cleanup when a used/revoked token is presented again: treat it as theft and kill the whole session
    @Modifying
    @Query("update RefreshToken rt set rt.revokedAt = :now where rt.appUser.id = :appUserId and rt.revokedAt is null")
    void revokeAllActiveForAppUser(UUID appUserId, LocalDateTime now);

}
