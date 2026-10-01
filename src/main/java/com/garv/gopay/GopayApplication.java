package com.garv.gopay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class GopayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GopayApplication.class, args);
	}

}
