package com.attendme.config;

import com.attendme.entity.Role;
import com.attendme.entity.User;
import com.attendme.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create admin account if none exists
        if (userRepository.count() == 0) {
            // Create Admin
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@attendme.com");
            admin.setPasswordHash(passwordEncoder.encode("Admin123!"));
            admin.setFullName("System Administrator");
            admin.setRole(Role.ADMIN);
            admin.setIsActive(true);
            userRepository.save(admin);
            
            // Create Teacher
            User teacher = new User();
            teacher.setUsername("teacher");
            teacher.setEmail("teacher@school.edu");
            teacher.setPasswordHash(passwordEncoder.encode("teacher123"));
            teacher.setFullName("Ms. Elena Reyes");
            teacher.setRole(Role.TEACHER);
            teacher.setIsActive(true);
            userRepository.save(teacher);
            
            System.out.println("✅ Demo accounts created:");
            System.out.println("   Admin: admin@attendme.com / Admin123!");
            System.out.println("   Teacher: teacher@school.edu / teacher123");
        }
    }
}