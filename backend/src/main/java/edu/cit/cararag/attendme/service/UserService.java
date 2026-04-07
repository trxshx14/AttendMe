package edu.cit.cararag.attendme.service;

import edu.cit.cararag.attendme.dto.requestdto.RegisterRequest;
import edu.cit.cararag.attendme.dto.requestdto.UserUpdateRequest;
import edu.cit.cararag.attendme.dto.response.UserResponse;
import edu.cit.cararag.attendme.entity.User;

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