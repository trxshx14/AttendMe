package edu.cit.cararag.attendme.features.user;

import edu.cit.cararag.attendme.shared.dto.ApiResponse;
import edu.cit.cararag.attendme.features.user.dto.UserUpdateRequest;
import edu.cit.cararag.attendme.features.user.dto.UserResponse;
import edu.cit.cararag.attendme.features.user.SupabaseStorageService;
import edu.cit.cararag.attendme.features.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    public UserController() {
        System.out.println("🎯🎯🎯 UserController INSTANTIATED! 🎯🎯🎯");
    }

    @GetMapping("/ping")
    public String ping() {
        return "UserController is working!";
    }

    @GetMapping
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        System.out.println("========== GET /api/users ==========");
        try {
            List<UserResponse> users = userService.getAllUsers();
            System.out.println("✅ Found " + users.size() + " users");
            return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
        } catch (Exception e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
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
    // @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
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

    /* ── Profile Picture Upload ─────────────────────────── */
    @PostMapping("/{id}/profile-picture")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProfilePicture(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("File is empty"));
            }
            if (!file.getContentType().startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Only image files are allowed"));
            }
            if (file.getSize() > 2 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("File size must be less than 2MB"));
            }

            // Upload to Supabase Storage
            String imageUrl = supabaseStorageService.uploadProfilePicture(file);

            // Save URL to user record
            userService.updateProfilePicture(id, imageUrl);

            return ResponseEntity.ok(ApiResponse.success(
                    "Profile picture uploaded successfully",
                    Map.of("profilePicUrl", imageUrl)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to upload profile picture: " + e.getMessage()));
        }
    }

    @GetMapping("/test")
    public String test() {
        return "UserController is working!";
    }
}