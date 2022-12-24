package com.kevin.appleshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.kevin.mqclient", "com.kevin.appleshop"})
public class AppleshopApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppleshopApplication.class, args);
	}

}
