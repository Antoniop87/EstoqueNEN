package com.nen.estoque.EstoqueNEN;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EstoqueNenApplication {

	public static void main(String[] args) {
		SpringApplication.run(EstoqueNenApplication.class, args);
	}

}
