package com.garv.gopay.vault.repositories;

import com.garv.gopay.vault.entities.CardToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardTokenRepository extends JpaRepository<CardToken, UUID> {

    Optional<CardToken> findByTokenAndRevokedAtIsNull(String token);

}
