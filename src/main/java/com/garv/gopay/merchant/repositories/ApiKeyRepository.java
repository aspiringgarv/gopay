package com.garv.gopay.merchant.repositories;

import com.garv.gopay.merchant.entities.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    List<ApiKey> findByMerchant_Id(UUID merchantId);

    Optional<ApiKey> findByKeyIdAndMerchant_Id(String keyId, UUID merchantId);

}
