package com.festora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class FoodQrMonolithApplication {

	public static void main(String[] args) {
		SpringApplication.run(FoodQrMonolithApplication.class, args);
	}

}
