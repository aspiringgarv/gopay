package com.garv.gopay.payment.simulator;

import com.garv.gopay.common.enums.PaymentStatus;
import com.garv.gopay.common.util.RandomizerUtil;
import com.garv.gopay.payment.entities.Payment;
import com.garv.gopay.payment.repositories.PaymentRepository;
import com.garv.gopay.payment.services.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class BankCallbackSimulator {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final SimulatorConfig simulatorConfig;

    @Scheduled(fixedDelayString = "${payment.simulator.polling-interval-ms}")
    public void simulateCallback() {
        LocalDateTime windowStart = LocalDateTime.now().minus(lookbackWindow());
        List<Payment> duePayments = paymentRepository.findByStatusAndUpdatedAtAfter(PaymentStatus.AUTHORIZING, windowStart);

        for (Payment payment : duePayments) {
            processCallback(payment);
        }
    }

    // the window must outlast the longest possible callback delay (plus one poll), otherwise a payment ages out before it is due
    private Duration lookbackWindow() {
        long longestDelayMs = simulatorConfig.getMethod().values().stream()
                .mapToLong(SimulatorConfig.MethodSimulatorConfig::getMaxDelayMs)
                .max()
                .orElse(0);
        if (simulatorConfig.getChaosMode() == ChaosMode.SLOW) {
            longestDelayMs *= 2;
        }
        return Duration.ofMillis(longestDelayMs + simulatorConfig.getPollingIntervalMs());
    }

    public void processCallback(Payment payment) {
        SimulatorConfig.MethodSimulatorConfig methodSimulatorConfig = simulatorConfig.getMethodConfig(payment.getMethod());

        if (methodSimulatorConfig == null) {
            log.warn("No simulator config for method {}, skipping paymentId {}", payment.getMethod(), payment.getId());
            return;
        }

        LocalDateTime dueAt = dueAt(payment, methodSimulatorConfig);
        if (LocalDateTime.now().isBefore(dueAt)) {
            log.info("Payment {} is not due for callback yet. Due at: {}", payment.getId(), dueAt);
            return;
        }

        switch (simulatorConfig.getChaosMode()) {
            case ALWAYS_SUCCESS -> resolve(payment, true);
            case ALWAYS_FAIL -> resolve(payment, false);
            case HIGH_FAILURE_RATE -> resolveByRoll(payment, methodSimulatorConfig.getSuccessRate() / 2);
            case NORMAL, SLOW -> resolveByRoll(payment, methodSimulatorConfig.getSuccessRate());
        }
    }

    private void resolveByRoll(Payment payment, int successRate) {
        int fixedRoll = Math.abs(payment.getId().hashCode()) % 101;
        resolve(payment, fixedRoll < successRate);
    }

    private void resolve(Payment payment, boolean approve) {
        String method = payment.getMethod().name();
        if (approve) {
            String bankRef = method + "_" + RandomizerUtil.randomBase64(16);
            paymentService.resolveAuthorization(payment.getId(), true, bankRef, null, null);
        } else {
            paymentService.resolveAuthorization(payment.getId(), false, null, method + "_DECLINED", "Bank callback declined the transaction");
        }
    }

    private LocalDateTime dueAt(Payment payment, SimulatorConfig.MethodSimulatorConfig methodSimulatorConfig) {
        long range = methodSimulatorConfig.getMaxDelayMs() - methodSimulatorConfig.getMinDelayMs();
        long delayMs = methodSimulatorConfig.getMinDelayMs();
        if (range > 0) {
            delayMs += Math.abs(payment.getId().hashCode()) % range;
        }
        if (simulatorConfig.getChaosMode() == ChaosMode.SLOW) {
            delayMs *= 2;
        }
        return payment.getUpdatedAt().plusNanos(delayMs * 1_000_000L);
    }

}
