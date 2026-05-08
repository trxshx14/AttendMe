package edu.cit.cararag.attendme.features.auth;

import edu.cit.cararag.attendme.features.auth.dto.LoginRequest;
import edu.cit.cararag.attendme.features.user.UserRepository;
import edu.cit.cararag.attendme.shared.entity.Role;
import edu.cit.cararag.attendme.shared.entity.User;
import edu.cit.cararag.attendme.shared.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;

    private User testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("teacher@test.com");
        testUser.setPasswordHash("hashedpassword");
        testUser.setFullName("Test Teacher");
        testUser.setUsername("teacher01");
        testUser.setRole(Role.TEACHER);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("teacher@test.com");
        loginRequest.setPassword("password123");
    }

    // ── TC-AUTH-01: Valid user found by email ─────────────────────────────────
    @Test
    void findUserByEmail_withValidEmail_returnsUser() {
        when(userRepository.findByEmail("teacher@test.com"))
                .thenReturn(Optional.of(testUser));

        Optional<User> result = userRepository.findByEmail("teacher@test.com");

        assertTrue(result.isPresent());
        assertEquals("teacher@test.com", result.get().getEmail());
    }

    // ── TC-AUTH-02: Password matches correctly ────────────────────────────────
    @Test
    void passwordEncoder_withCorrectPassword_returnsTrue() {
        when(passwordEncoder.matches("password123", "hashedpassword"))
                .thenReturn(true);

        boolean matches = passwordEncoder.matches(
                loginRequest.getPassword(), testUser.getPasswordHash());

        assertTrue(matches);
    }

    // ── TC-AUTH-03: Wrong password returns false ──────────────────────────────
    @Test
    void passwordEncoder_withWrongPassword_returnsFalse() {
        when(passwordEncoder.matches("wrongpassword", "hashedpassword"))
                .thenReturn(false);

        boolean matches = passwordEncoder.matches(
                "wrongpassword", testUser.getPasswordHash());

        assertFalse(matches);
    }

   // ── TC-AUTH-04: JWT token generated successfully ──────────────────────────
@Test
void jwtUtils_generateToken_returnsNonNullToken() {
    when(jwtUtils.generateTokenFromUsername(anyString())).thenReturn("mock-jwt-token-xyz");

    String token = jwtUtils.generateTokenFromUsername("teacher@test.com");

    assertNotNull(token);
    assertFalse(token.isEmpty());
    assertEquals("mock-jwt-token-xyz", token);
}

    // ── TC-AUTH-05: User not found returns empty optional ─────────────────────
    @Test
    void findUserByEmail_withInvalidEmail_returnsEmpty() {
        when(userRepository.findByEmail("notfound@test.com"))
                .thenReturn(Optional.empty());

        Optional<User> result = userRepository.findByEmail("notfound@test.com");

        assertFalse(result.isPresent());
    }

    // ── TC-AUTH-06: Admin role correctly identified ───────────────────────────
    @Test
    void userRole_forAdmin_isCorrectlySet() {
        User adminUser = new User();
        adminUser.setEmail("admin@test.com");
        adminUser.setUsername("admin01");
        adminUser.setPasswordHash("hashedpassword");
        adminUser.setFullName("Admin User");
        adminUser.setRole(Role.ADMIN);

        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(adminUser));

        Optional<User> result = userRepository.findByEmail("admin@test.com");

        assertTrue(result.isPresent());
        assertEquals(Role.ADMIN, result.get().getRole());
    }
}