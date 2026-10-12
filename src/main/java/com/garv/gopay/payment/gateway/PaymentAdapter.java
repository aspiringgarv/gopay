package com.garv.gopay.payment.gateway;

import com.garv.gopay.payment.gateway.dto.PaymentRequest;
import com.garv.gopay.payment.gateway.dto.PaymentResult;

public interface PaymentAdapter {

    PaymentResult initiate(PaymentRequest payment);

    PaymentResult capture(PaymentRequest payment);

}
