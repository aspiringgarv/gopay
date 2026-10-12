package com.garv.gopay.payment.services.impl;

import com.garv.gopay.common.enums.Actor;
import com.garv.gopay.common.enums.PaymentEvent;
import com.garv.gopay.common.enums.PaymentStatus;
import com.garv.gopay.payment.entities.Payment;
import com.garv.gopay.payment.entities.PaymentTransitionLog;
import com.garv.gopay.payment.repositories.PaymentTransitionLogRepository;
import com.garv.gopay.payment.services.PaymentTransitionService;
import com.garv.gopay.payment.statemachine.PaymentStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentTransitionServiceImpl implements PaymentTransitionService {

    private final PaymentStateMachine paymentStateMachine;
    private final PaymentTransitionLogRepository paymentTransitionLogRepository;

    @Override
    @Transactional
    public PaymentStatus apply(Payment payment, PaymentEvent event, Actor actor, String reason) {
        PaymentStatus fromStatus = payment.getStatus();
        PaymentStatus toStatus = paymentStateMachine.transition(payment, event);
        payment.setStatus(toStatus);

        PaymentTransitionLog transitionLog = PaymentTransitionLog.builder()
                .payment(payment)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .paymentEvent(event)
                .actor(actor)
                .reason(reason)
                .occurredAt(LocalDateTime.now())
                .build();

        paymentTransitionLogRepository.save(transitionLog);

        return toStatus;
    }

}
