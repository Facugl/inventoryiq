package com.inventoryiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InventoryIqApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryIqApplication.class, args);
	}

}
