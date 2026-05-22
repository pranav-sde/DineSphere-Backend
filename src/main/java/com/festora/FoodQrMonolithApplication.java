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
		SpringApplication.run(FoodQrMonolithApplication.class, args);
	}
}
