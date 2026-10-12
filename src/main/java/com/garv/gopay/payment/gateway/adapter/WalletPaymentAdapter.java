package com.garv.gopay.payment.gateway.adapter;

import com.garv.gopay.payment.gateway.PaymentAdapter;
import com.garv.gopay.payment.gateway.dto.PaymentRequest;
import com.garv.gopay.payment.gateway.dto.PaymentResult;
import org.springframework.stereotype.Component;

@Component
public class WalletPaymentAdapter implements PaymentAdapter {

    @Override
    public PaymentResult initiate(PaymentRequest payment) {
        return new PaymentResult.Pending(null);
    }

    @Override
    public PaymentResult capture(PaymentRequest payment) {
        return new PaymentResult.Success(null);
    }

}
