package com.garv.gopay.payment.repositories;

import com.garv.gopay.common.enums.PaymentStatus;
import com.garv.gopay.payment.entities.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrder_IdAndMerchantId(UUID orderId, UUID merchantId);

    List<Payment> findByMerchantId(UUID merchantId);

    Optional<Payment> findByIdAndMerchantId(UUID id, UUID merchantId);

    // row-level lock (SELECT ... FOR UPDATE) — use this instead of the plain lookup above when the
    // caller is about to read-then-write the same row, so a concurrent request on the same payment
    // blocks until this transaction commits instead of racing it. Only takes effect inside a
    // @Transactional method; the lock is held for that transaction's duration, not past it.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findByIdAndMerchantIdForUpdate(UUID id, UUID merchantId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByStatusAndUpdatedAtAfter(PaymentStatus status, LocalDateTime updatedAtAfter);

}
