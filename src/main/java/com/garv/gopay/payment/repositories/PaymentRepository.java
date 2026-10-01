package com.garv.gopay.payment.repositories;

import com.garv.gopay.payment.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrder_IdAndMerchantId(UUID orderId, UUID merchantId);

}
