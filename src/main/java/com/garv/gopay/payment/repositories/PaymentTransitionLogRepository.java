package com.garv.gopay.payment.repositories;

import com.garv.gopay.payment.entities.PaymentTransitionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentTransitionLogRepository extends JpaRepository<PaymentTransitionLog, UUID> {

    List<PaymentTransitionLog> findByPayment_IdOrderByOccurredAtAsc(UUID paymentId);

}
