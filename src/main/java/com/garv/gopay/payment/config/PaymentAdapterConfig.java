package com.garv.gopay.payment.config;

import com.garv.gopay.common.enums.PaymentMethod;
import com.garv.gopay.payment.gateway.PaymentAdapter;
import com.garv.gopay.payment.gateway.adapter.CardPaymentAdapter;
import com.garv.gopay.payment.gateway.adapter.NetbankingPaymentAdapter;
import com.garv.gopay.payment.gateway.adapter.UpiPaymentAdapter;
import com.garv.gopay.payment.gateway.adapter.WalletPaymentAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class PaymentAdapterConfig {

    private final CardPaymentAdapter cardPaymentAdapter;
    private final NetbankingPaymentAdapter netbankingPaymentAdapter;
    private final UpiPaymentAdapter upiPaymentAdapter;
    private final WalletPaymentAdapter walletPaymentAdapter;

    @Bean
    public Map<PaymentMethod, PaymentAdapter> paymentAdapterMap() {
        return Map.of(
                PaymentMethod.CARD, cardPaymentAdapter,
                PaymentMethod.NETBANKING, netbankingPaymentAdapter,
                PaymentMethod.UPI, upiPaymentAdapter,
                PaymentMethod.WALLET, walletPaymentAdapter
        );
    }

}
