package com.garv.gopay.payment.services;

import com.garv.gopay.common.enums.Actor;
import com.garv.gopay.common.enums.PaymentEvent;
import com.garv.gopay.common.enums.PaymentStatus;
import com.garv.gopay.payment.entities.Payment;
import lombok.NonNull;

public interface PaymentTransitionService {

    PaymentStatus apply(@NonNull Payment payment, @NonNull PaymentEvent event, Actor actor, String reason);

}
