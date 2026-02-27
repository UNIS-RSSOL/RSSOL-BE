package com.example.unis_rssol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UnisRssolApplication {

	public static void main(String[] args) {
		SpringApplication.run(UnisRssolApplication.class, args);

		System.out.println("DB_URL=" + System.getenv("DB_URL"));
		System.out.println("DB_USERNAME=" + System.getenv("DB_USERNAME"));
	}

}
