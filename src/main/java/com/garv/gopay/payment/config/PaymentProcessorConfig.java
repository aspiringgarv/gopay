package com.garv.gopay.payment.config;

import com.garv.gopay.common.enums.PaymentMethod;
import com.garv.gopay.payment.processor.PaymentProcessor;
import com.garv.gopay.payment.processor.strategy.CardPaymentProcessor;
import com.garv.gopay.payment.processor.strategy.NetbankingPaymentProcessor;
import com.garv.gopay.payment.processor.strategy.UpiPaymentProcessor;
import com.garv.gopay.payment.processor.strategy.WalletPaymentProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class PaymentProcessorConfig {

    private final CardPaymentProcessor cardPaymentProcessor;
    private final NetbankingPaymentProcessor netbankingPaymentProcessor;
    private final UpiPaymentProcessor upiPaymentProcessor;
    private final WalletPaymentProcessor walletPaymentProcessor;

    @Bean
    public Map<PaymentMethod, PaymentProcessor> paymentProcessorMap() {
        return Map.of(
                PaymentMethod.CARD, cardPaymentProcessor,
                PaymentMethod.NETBANKING, netbankingPaymentProcessor,
                PaymentMethod.UPI, upiPaymentProcessor,
                PaymentMethod.WALLET, walletPaymentProcessor
        );
    }

}
