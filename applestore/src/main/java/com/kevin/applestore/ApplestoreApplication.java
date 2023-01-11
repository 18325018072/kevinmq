package com.kevin.applestore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.kevin.mqclient", "com.kevin.applestore"})
public class ApplestoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApplestoreApplication.class, args);
	}

}
