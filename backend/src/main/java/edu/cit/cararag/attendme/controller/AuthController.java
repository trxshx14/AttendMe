package edu.cit.cararag.attendme.controller;

import edu.cit.cararag.attendme.dto.requestdto.GoogleAuthRequest;
import edu.cit.cararag.attendme.dto.requestdto.LoginRequest;
import edu.cit.cararag.attendme.dto.requestdto.RegisterRequest;
import edu.cit.cararag.attendme.dto.response.ApiResponse;
import edu.cit.cararag.attendme.dto.response.JwtResponse;
import edu.cit.cararag.attendme.dto.response.UserResponse;
import edu.cit.cararag.attendme.security.JwtUtils;
import edu.cit.cararag.attendme.service.AuthService;
import edu.cit.cararag.attendme.service.GoogleAuthService;
import edu.cit.cararag.attendme.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private GoogleAuthService googleAuthService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request) {
        System.out.println("✅ AuthController.login() called!");
        try {
            JwtResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        System.out.println("✅ AuthController.register() called!");
        try {
            UserResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("User registered successfully", response));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Registration failed: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        System.out.println("✅ AuthController.me() called!");
        try {
            UserResponse response = authService.getCurrentUser();
            return ResponseEntity.ok(ApiResponse.success("Current user retrieved", response));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to get current user: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            // ✅ Read token directly from header — works even when JWT filter skips this endpoint
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @AuthenticationPrincipal UserDetails userDetails) {
        System.out.println("✅ AuthController.logout() called!");
        try {
            String username = null;

            // ✅ Try @AuthenticationPrincipal first (works when JWT filter processes request)
            if (userDetails != null) {
                username = userDetails.getUsername();
                System.out.println("✅ Got username from SecurityContext: " + username);
            }
            // ✅ Fallback: parse token manually from header
            else if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    username = jwtUtils.getUserNameFromJwtToken(token);
                    System.out.println("✅ Got username from token header: " + username);
                } catch (Exception e) {
                    System.err.println("Could not parse token: " + e.getMessage());
                }
            }

            if (username != null && !username.isBlank()) {
                userService.setUserOnline(username, false);
                System.out.println("✅ User marked offline: " + username);
            } else {
                System.err.println("⚠️ Could not determine username for logout");
            }
        } catch (Exception e) {
            System.err.println("Logout error: " + e.getMessage());
        }
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<JwtResponse>> googleAuth(@RequestBody GoogleAuthRequest request) {
        System.out.println("✅ AuthController.googleAuth() called!");
        try {
            JwtResponse response;

            if (StringUtils.hasText(request.getAccessToken())) {
                System.out.println("🔵 Using access_token flow");
                response = googleAuthService.loginWithAccessToken(request.getAccessToken());
            } else if (StringUtils.hasText(request.getIdToken())) {
                System.out.println("🔵 Using id_token flow");
                response = googleAuthService.loginWithIdToken(request.getIdToken());
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Either accessToken or idToken is required"));
            }

            // ✅ Mark user online after Google login
            userService.setUserOnline(response.getUsername(), true);

            return ResponseEntity.ok(ApiResponse.success("Google login successful", response));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Google authentication failed: " + e.getMessage()));
        }
    }

    @GetMapping("/test")
    public String test() {
        return "AuthController is working!";
    }
}