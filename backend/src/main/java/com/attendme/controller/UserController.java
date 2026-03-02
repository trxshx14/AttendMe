package com.attendme.controller;

import com.attendme.dto.response.ApiResponse;
import com.attendme.dto.requestdto.UserUpdateRequest;
import com.attendme.dto.response.UserResponse;
import com.attendme.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        try {
            List<UserResponse> users = userService.getAllUsers();
            return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to retrieve users: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        try {
            UserResponse user = userService.getUserById(id);
            return ResponseEntity.ok(ApiResponse.success("User found", user));
        } catch (Exception e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("User not found: " + e.getMessage()));
        }
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByRole(@PathVariable String role) {
        try {
            List<UserResponse> users = userService.getUsersByRole(role);
            return ResponseEntity.ok(ApiResponse.success("Users by role retrieved", users));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to retrieve users: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @RequestBody UserUpdateRequest request) {
        try {
            UserResponse user = userService.updateUser(id, request);
            return ResponseEntity.ok(ApiResponse.success("User updated successfully", user));
        } catch (Exception e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Failed to update user: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Failed to delete user: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(@PathVariable Long id) {
        try {
            UserResponse user = userService.activateUser(id);
            return ResponseEntity.ok(ApiResponse.success("User activated successfully", user));
        } catch (Exception e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Failed to activate user: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(@PathVariable Long id) {
        try {
            UserResponse user = userService.deactivateUser(id);
            return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", user));
        } catch (Exception e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Failed to deactivate user: " + e.getMessage()));
        }
    }

    @GetMapping("/test")
public String test() {
    return "UserController is working!";
}
}