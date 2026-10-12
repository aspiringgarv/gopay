package com.garv.gopay.payment.repositories;

import com.garv.gopay.common.enums.PaymentStatus;
import com.garv.gopay.payment.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
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

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByStatusAndUpdatedAtAfter(PaymentStatus status, LocalDateTime updatedAtAfter);

}
