package com.garv.gopay.payment.repositories;

import com.garv.gopay.payment.entities.OrderRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<OrderRecord, UUID> {

    boolean existsByMerchantIdAndReceipt(UUID merchantId, String receipt);

    Optional<OrderRecord> findByIdAndMerchantId(UUID id, UUID merchantId);

    // row-level lock (SELECT ... FOR UPDATE) — use this instead of the plain lookup above when the
    // caller is about to read-then-write the same row, so a concurrent request on the same order
    // blocks until this transaction commits instead of racing it. Only takes effect inside a
    // @Transactional method; the lock is held for that transaction's duration, not past it.
    // Explicit @Query because derived-name parsing can't handle the "ForUpdate" suffix: it splits
    // it as MerchantId + ForUpdate and tries (and fails) to resolve "forUpdate" as a nested
    // property on UUID.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderRecord o where o.id = :id and o.merchantId = :merchantId")
    Optional<OrderRecord> findByIdAndMerchantIdForUpdate(@Param("id") UUID id, @Param("merchantId") UUID merchantId);

    boolean existsByIdAndMerchantId(UUID id, UUID merchantId);

}
