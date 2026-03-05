package com.attendme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.boot.CommandLineRunner;

@SpringBootApplication
public class AttendmeBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AttendmeBackendApplication.class, args);
        System.out.println("AttendMe is successful!!");
    }
    
    @Bean
    public CommandLineRunner commandLineRunner(RequestMappingHandlerMapping handlerMapping) {
        return args -> {
            System.out.println("=== REGISTERED REQUEST MAPPINGS ===");
            handlerMapping.getHandlerMethods().forEach((key, value) -> {
                System.out.println(key + " -> " + value);
            });
        };
    }
}