package com.garv.gopay.payment.statemachine;

import com.garv.gopay.common.enums.PaymentEvent;
import com.garv.gopay.common.enums.PaymentStatus;
import com.garv.gopay.common.exceptions.BusinessRuleViolationException;
import com.garv.gopay.payment.entities.Payment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentStateMachine {

    private static final Map<Transition, PaymentStatus> TRANSITIONS = Map.ofEntries(
            Map.entry(new Transition(PaymentStatus.CREATED, PaymentEvent.AUTHORIZE_ATTEMPT), PaymentStatus.AUTHORIZING),
            Map.entry(new Transition(PaymentStatus.CREATED, PaymentEvent.AUTHORIZE_SUCCESS), PaymentStatus.AUTHORIZED),
            Map.entry(new Transition(PaymentStatus.CREATED, PaymentEvent.AUTHORIZE_FAIL), PaymentStatus.FAILED),
            Map.entry(new Transition(PaymentStatus.CREATED, PaymentEvent.CANCEL), PaymentStatus.CANCELLED),

            Map.entry(new Transition(PaymentStatus.AUTHORIZING, PaymentEvent.AUTHORIZE_SUCCESS), PaymentStatus.AUTHORIZED),
            Map.entry(new Transition(PaymentStatus.AUTHORIZING, PaymentEvent.AUTHORIZE_FAIL), PaymentStatus.FAILED),

            Map.entry(new Transition(PaymentStatus.AUTHORIZED, PaymentEvent.CAPTURE_REQUEST), PaymentStatus.CAPTURING),
            Map.entry(new Transition(PaymentStatus.AUTHORIZED, PaymentEvent.CAPTURE_TIMEOUT), PaymentStatus.AUTH_EXPIRED),
            Map.entry(new Transition(PaymentStatus.AUTHORIZED, PaymentEvent.CANCEL), PaymentStatus.CANCELLED),

            Map.entry(new Transition(PaymentStatus.CAPTURING, PaymentEvent.CAPTURE_SUCCESS), PaymentStatus.CAPTURED),
            Map.entry(new Transition(PaymentStatus.CAPTURING, PaymentEvent.CAPTURE_FAIL), PaymentStatus.FAILED),

            Map.entry(new Transition(PaymentStatus.CAPTURED, PaymentEvent.SETTLE), PaymentStatus.SETTLED),
            Map.entry(new Transition(PaymentStatus.CAPTURED, PaymentEvent.REFUND_INIT), PaymentStatus.PARTIALLY_REFUNDED),
            Map.entry(new Transition(PaymentStatus.CAPTURED, PaymentEvent.REFUND_COMPLETE), PaymentStatus.REFUNDED),

            Map.entry(new Transition(PaymentStatus.SETTLED, PaymentEvent.REFUND_INIT), PaymentStatus.PARTIALLY_REFUNDED),
            Map.entry(new Transition(PaymentStatus.SETTLED, PaymentEvent.REFUND_COMPLETE), PaymentStatus.REFUNDED),

            Map.entry(new Transition(PaymentStatus.PARTIALLY_REFUNDED, PaymentEvent.REFUND_COMPLETE), PaymentStatus.REFUNDED)
    );

    public PaymentStatus transition(Payment payment, PaymentEvent event) {
        PaymentStatus from = payment.getStatus();
        Transition key = new Transition(from, event);
        PaymentStatus to = TRANSITIONS.get(key);

        if (to == null) {
            throw new BusinessRuleViolationException(
                    "INVALID_PAYMENT_STATE_TRANSITION",
                    "No transition for event " + event + " from status " + from
            );
        }


        return to;
    }

    private record Transition(PaymentStatus from, PaymentEvent event) {
    }

}
