package com.garv.gopay.payment.simulator;

import com.garv.gopay.common.enums.PaymentMethod;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "payment.simulator")
public class SimulatorConfig {

    private long pollingIntervalMs;
    private ChaosMode chaosMode;
    private Map<PaymentMethod, MethodSimulatorConfig> method;

    public MethodSimulatorConfig getMethodConfig(PaymentMethod paymentMethod) {
        return method.get(paymentMethod);
    }

    @Getter
    @Setter
    public static class MethodSimulatorConfig {
        private long minDelayMs;
        private long maxDelayMs;
        private int successRate;
    }

}
