package edu.cit.cararag.attendme.service.impl;

import edu.cit.cararag.attendme.dto.requestdto.RegisterRequest;
import edu.cit.cararag.attendme.dto.requestdto.UserUpdateRequest;
import edu.cit.cararag.attendme.dto.response.UserResponse;
import edu.cit.cararag.attendme.entity.Role;
import edu.cit.cararag.attendme.entity.User;
import edu.cit.cararag.attendme.exception.DuplicateResourceException;
import edu.cit.cararag.attendme.exception.ResourceNotFoundException;
import edu.cit.cararag.attendme.repository.AttendanceRepository;
import edu.cit.cararag.attendme.repository.UserRepository;
import edu.cit.cararag.attendme.service.EmailService;
import edu.cit.cararag.attendme.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AttendanceRepository attendanceRepository;
    private final EmailService emailService;

    // ─── Register ─────────────────────────────────────────────────────────────

    @Override
    public UserResponse registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername()))
            throw new DuplicateResourceException("User", "username", request.getUsername());
        if (userRepository.existsByEmail(request.getEmail()))
            throw new DuplicateResourceException("User", "email", request.getEmail());

        String rawPassword = (request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword()
                : generateTemporaryPassword();

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(request.getRole() != null && request.getRole().equalsIgnoreCase("ADMIN")
                ? Role.ADMIN : Role.TEACHER);
        user.setIsActive(true);
        user.setIsOnline(false);

        User savedUser = userRepository.save(user);

        // ── Email 1: Welcome email on account creation ──
        try {
            emailService.sendWelcomeEmail(
                    savedUser.getEmail(),
                    savedUser.getFullName(),
                    savedUser.getUsername(),
                    rawPassword
            );
        } catch (Exception e) {
            log.warn("Welcome email could not be sent to {}: {}", savedUser.getEmail(), e.getMessage());
        }

        return UserResponse.fromUser(savedUser);
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    @Override
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = findUserById(id);

        if (request.getUsername() != null && !request.getUsername().isEmpty()) {
            if (!request.getUsername().equals(user.getUsername()) &&
                    userRepository.existsByUsername(request.getUsername()))
                throw new DuplicateResourceException("User", "username", request.getUsername());
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (!request.getEmail().equals(user.getEmail()) &&
                    userRepository.existsByEmail(request.getEmail()))
                throw new DuplicateResourceException("User", "email", request.getEmail());
            user.setEmail(request.getEmail());
        }
        if (request.getFullName() != null && !request.getFullName().isEmpty())
            user.setFullName(request.getFullName());
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            try { user.setRole(Role.valueOf(request.getRole().toUpperCase())); }
            catch (IllegalArgumentException e) {
                throw new ResourceNotFoundException("Role", "name", request.getRole());
            }
        }
        if (request.getIsActive() != null)
            user.setIsActive(request.getIsActive());

        // ── Email 2: Notify teacher if admin updated their password ──
        String rawNewPassword = null;
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            rawNewPassword = request.getPassword();
            user.setPasswordHash(passwordEncoder.encode(rawNewPassword));
        }

        User savedUser = userRepository.save(user);

        if (rawNewPassword != null) {
            try {
                emailService.sendPasswordUpdatedEmail(
                        savedUser.getEmail(),
                        savedUser.getFullName(),
                        savedUser.getUsername(),
                        rawNewPassword
                );
            } catch (Exception e) {
                log.warn("Password updated email could not be sent to {}: {}",
                        savedUser.getEmail(), e.getMessage());
            }
        }

        return UserResponse.fromUser(savedUser);
    }

    // ─── Other methods (unchanged) ────────────────────────────────────────────

    @Override
    public UserResponse getUserById(Long id) {
        return UserResponse.fromUser(findUserById(id));
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        return UserResponse.fromUser(userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username)));
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        return UserResponse.fromUser(userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email)));
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> getUsersByRole(String role) {
        try {
            Role userRole = Role.valueOf(role.toUpperCase());
            return userRepository.findByRole(userRole).stream()
                    .map(UserResponse::fromUser)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Role", "name", role);
        }
    }

    @Override
    public List<UserResponse> getTeachersWithClasses() {
        return userRepository.findTeachersWithClasses(Role.TEACHER).stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        attendanceRepository.nullifyMarkedBy(id);
        userRepository.delete(user);
    }

    @Override
    public UserResponse activateUser(Long id) {
        User user = findUserById(id);
        user.setIsActive(true);
        return UserResponse.fromUser(userRepository.save(user));
    }

    @Override
    public UserResponse deactivateUser(Long id) {
        User user = findUserById(id);
        user.setIsActive(false);
        return UserResponse.fromUser(userRepository.save(user));
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = principal instanceof UserDetails
                ? ((UserDetails) principal).getUsername()
                : principal.toString();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Override
    public void updateLastLogin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public void updateProfilePicture(Long id, String profilePicUrl) {
        User user = findUserById(id);
        user.setProfilePicUrl(profilePicUrl);
        userRepository.save(user);
    }

    @Override
    public void setUserOnline(String username, boolean online) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        user.setIsOnline(online);
        userRepository.save(user);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private String generateTemporaryPassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}