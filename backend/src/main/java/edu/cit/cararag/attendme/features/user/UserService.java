package edu.cit.cararag.attendme.features.user;

import edu.cit.cararag.attendme.features.auth.dto.RegisterRequest;
import edu.cit.cararag.attendme.features.user.dto.UserUpdateRequest;
import edu.cit.cararag.attendme.features.user.dto.UserResponse;
import edu.cit.cararag.attendme.shared.entity.User;

import java.util.List;

public interface UserService {

    UserResponse registerUser(RegisterRequest request);
    UserResponse getUserById(Long id);
    UserResponse getUserByUsername(String username);
    UserResponse getUserByEmail(String email);
    List<UserResponse> getAllUsers();
    List<UserResponse> getUsersByRole(String role);
    List<UserResponse> getTeachersWithClasses();
    UserResponse updateUser(Long id, UserUpdateRequest request);
    void deleteUser(Long id);
    UserResponse activateUser(Long id);
    UserResponse deactivateUser(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    User getCurrentUser();
    void updateLastLogin(String username);
    void updateProfilePicture(Long id, String profilePicUrl);

    // ✅ NEW
    void setUserOnline(String username, boolean online);
}