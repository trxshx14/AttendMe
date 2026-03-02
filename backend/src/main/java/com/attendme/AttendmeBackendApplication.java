package com.attendme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.attendme"})
public class AttendmeBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AttendmeBackendApplication.class, args);
		System.out.println("AttendMe is successful!!");
	}
}