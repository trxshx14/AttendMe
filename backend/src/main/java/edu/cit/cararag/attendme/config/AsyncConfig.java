package edu.cit.cararag.attendme.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Enables @Async on EmailServiceImpl.sendWelcomeEmail()
    // so email sending doesn't block the API response
}