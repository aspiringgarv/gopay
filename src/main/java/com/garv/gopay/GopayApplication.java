package com.garv.gopay;

import com.garv.gopay.payment.simulator.SimulatorConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "securityAuditorAware")
@EnableConfigurationProperties(SimulatorConfig.class)
@EnableScheduling
public class GopayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GopayApplication.class, args);
	}

}
