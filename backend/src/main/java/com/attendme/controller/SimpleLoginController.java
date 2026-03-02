package com.attendme.controller;

import com.attendme.entity.User;
import com.attendme.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/simple")
public class SimpleLoginController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> credentials) {
        Map<String, Object> response = new HashMap<>();
        
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        System.out.println("🔐 Simple login attempt: " + username);
        
        User user = userRepository.findByUsername(username).orElse(null);
        
        if (user != null) {
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("userId", user.getUserId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
        } else {
            response.put("success", false);
            response.put("message", "User not found");
        }
        
        return response;
    }
    
    @GetMapping("/test")
    public String test() {
        return "Simple controller is working!";
    }
}