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
import edu.cit.cararag.attendme.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AttendanceRepository attendanceRepository;

    @Override
    public UserResponse registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername()))
            throw new DuplicateResourceException("User", "username", request.getUsername());
        if (userRepository.existsByEmail(request.getEmail()))
            throw new DuplicateResourceException("User", "email", request.getEmail());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null && request.getRole().equalsIgnoreCase("ADMIN")
                ? Role.ADMIN : Role.TEACHER);
        user.setIsActive(true);
        user.setIsOnline(false);
        return UserResponse.fromUser(userRepository.save(user));
    }

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
            catch (IllegalArgumentException e) { throw new ResourceNotFoundException("Role", "name", request.getRole()); }
        }
        if (request.getIsActive() != null)
            user.setIsActive(request.getIsActive());

        return UserResponse.fromUser(userRepository.save(user));
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

    // ✅ NEW
    @Override
    public void setUserOnline(String username, boolean online) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        user.setIsOnline(online);
        userRepository.save(user);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
}