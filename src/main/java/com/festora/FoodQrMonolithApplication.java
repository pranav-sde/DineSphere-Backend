package com.festora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration.class})
@EnableScheduling
@EnableAsync
@EnableCaching
@ConfigurationPropertiesScan
public class FoodQrMonolithApplication {

	public static void main(String[] args) {
		try {
			SpringApplication.run(FoodQrMonolithApplication.class, args);
		} catch (Throwable t) {
			System.out.println("CRITICAL STARTUP ERROR:");
			t.printStackTrace(System.out);
			t.printStackTrace(System.err);
			System.out.flush();
			System.err.flush();
			throw t;
		}
	}
}
