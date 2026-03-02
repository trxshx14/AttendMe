package com.attendme.service.impl;

import com.attendme.dto.requestdto.LoginRequest;
import com.attendme.dto.requestdto.RegisterRequest;
import com.attendme.dto.response.JwtResponse;
import com.attendme.dto.response.UserResponse;
import com.attendme.entity.User;
import com.attendme.exception.ResourceNotFoundException;
import com.attendme.exception.UnauthorizedException;
import com.attendme.repository.UserRepository;
import com.attendme.security.JwtUtils;
import com.attendme.service.AuthService;
import com.attendme.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImp implements AuthService {

    @Autowired(required = false)
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired(required = false)
    private JwtUtils jwtUtils;

    @Override
    public JwtResponse login(LoginRequest request) {
        System.out.println("========== AUTH SERVICE LOGIN ==========");
        System.out.println("1. Login attempt for username: '" + request.getUsername() + "'");
        System.out.println("2. Password length: " + (request.getPassword() != null ? request.getPassword().length() : 0));
        
        // Check if AuthenticationManager is null
        if (authenticationManager == null) {
            System.err.println("❌ AuthenticationManager is NULL!");
            System.err.println("   Check if SecurityConfig is properly configured");
        } else {
            System.out.println("✅ AuthenticationManager is present");
        }
        
        // Check if JwtUtils is null
        if (jwtUtils == null) {
            System.err.println("❌ JwtUtils is NULL!");
        } else {
            System.out.println("✅ JwtUtils is present");
        }
        
        // Check if user exists in database
        try {
            User user = userRepository.findByUsername(request.getUsername()).orElse(null);
            if (user == null) {
                System.err.println("❌ User NOT found in database: " + request.getUsername());
                System.err.println("   Check if users table has data");
            } else {
                System.out.println("✅ User found in database:");
                System.out.println("   - ID: " + user.getUserId());
                System.out.println("   - Username: " + user.getUsername());
                System.out.println("   - Email: " + user.getEmail());
                System.out.println("   - Role: " + user.getRole());
                System.out.println("   - Password Hash: " + user.getPasswordHash().substring(0, 20) + "...");
            }
        } catch (Exception e) {
            System.err.println("❌ Database error: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Try to authenticate
        try {
            System.out.println("3. Attempting authentication...");
            
            if (authenticationManager == null) {
                throw new IllegalStateException("AuthenticationManager is not configured");
            }
            
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            
            System.out.println("4. Authentication successful!");
            System.out.println("   - Authenticated: " + authentication.isAuthenticated());
            System.out.println("   - Principal: " + authentication.getPrincipal());
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            if (jwtUtils == null) {
                throw new IllegalStateException("JwtUtils is not configured");
            }
            
            String jwt = jwtUtils.generateJwtToken(authentication);
            System.out.println("5. JWT generated successfully");
            
            User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", request.getUsername()));
            
            userService.updateLastLogin(user.getUsername());
            System.out.println("6. Last login updated");
            
            System.out.println("✅ Login completed successfully!");
            
            return new JwtResponse(
                jwt,
                "dummy-refresh-token",
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
            );
            
        } catch (BadCredentialsException e) {
            System.err.println("❌ Bad credentials: " + e.getMessage());
            throw new UnauthorizedException("Invalid username or password");
        } catch (UsernameNotFoundException e) {
            System.err.println("❌ Username not found: " + e.getMessage());
            throw new ResourceNotFoundException("User", "username", request.getUsername());
        } catch (IllegalStateException e) {
            System.err.println("❌ Configuration error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Server configuration error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Unexpected error: " + e.getMessage());
        }
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        return userService.registerUser(request);
    }

    @Override
    public UserResponse getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
            return UserResponse.fromUser(user);
        }
        
        throw new UnauthorizedException("No authenticated user found");
    }
}