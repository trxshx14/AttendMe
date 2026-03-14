package edu.cit.cararag.attendme.service.impl;

import edu.cit.cararag.attendme.dto.requestdto.LoginRequest;
import edu.cit.cararag.attendme.dto.requestdto.RegisterRequest;
import edu.cit.cararag.attendme.dto.response.JwtResponse;
import edu.cit.cararag.attendme.dto.response.UserResponse;
import edu.cit.cararag.attendme.entity.User;
import edu.cit.cararag.attendme.exception.ResourceNotFoundException;
import edu.cit.cararag.attendme.exception.UnauthorizedException;
import edu.cit.cararag.attendme.repository.UserRepository;
import edu.cit.cararag.attendme.security.JwtUtils;
import edu.cit.cararag.attendme.service.AuthService;
import edu.cit.cararag.attendme.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

        String identifier = (request.getEmail() != null && !request.getEmail().isBlank())
                ? request.getEmail()
                : request.getUsername();

        System.out.println("1. Login attempt for: '" + identifier + "'");

        if (authenticationManager == null) throw new IllegalStateException("AuthenticationManager is not configured");
        if (jwtUtils == null)             throw new IllegalStateException("JwtUtils is not configured");

        try {
            User user = userRepository.findByEmail(identifier)
                    .or(() -> userRepository.findByUsername(identifier))
                    .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

            System.out.println("✅ User found: " + user.getUsername() + " | Role: " + user.getRole());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword())
            );

            System.out.println("✅ Authentication successful!");
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = jwtUtils.generateJwtToken(authentication);
            System.out.println("✅ JWT generated");

            userService.updateLastLogin(user.getUsername());
            System.out.println("✅ Last login updated");

            return JwtResponse.builder()
                    .accessToken(jwt)
                    .refreshToken("dummy-refresh-token")
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole().name())
                    .profilePicUrl(user.getProfilePicUrl())
                    .build();

        } catch (DisabledException e) {
            System.err.println("❌ Account disabled: " + e.getMessage());
            throw new UnauthorizedException("Account is deactivated. Please contact your administrator.");
        } catch (BadCredentialsException e) {
            System.err.println("❌ Bad credentials: " + e.getMessage());
            throw new UnauthorizedException("Invalid email or password");
        } catch (UnauthorizedException e) {
            throw e;
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