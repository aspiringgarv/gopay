package com.garv.gopay.payment.gateway;

import com.garv.gopay.common.enums.PaymentMethod;
import com.garv.gopay.payment.gateway.dto.PaymentRequest;
import com.garv.gopay.payment.gateway.dto.PaymentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PaymentGatewayRouter {

    private final Map<PaymentMethod, PaymentAdapter> paymentAdapterMap;

    public PaymentResult initiate(PaymentRequest paymentRequest) {
        return resolve(paymentRequest.method()).initiate(paymentRequest);
    }

    public PaymentResult capture(PaymentRequest paymentRequest) {
        return resolve(paymentRequest.method()).capture(paymentRequest);
    }

    private PaymentAdapter resolve(PaymentMethod method) {
        PaymentAdapter adapter = paymentAdapterMap.get(method);
        if (adapter == null) {
            throw new IllegalStateException("No PaymentAdapter configured for method: " + method);
        }
        return adapter;
    }

}
