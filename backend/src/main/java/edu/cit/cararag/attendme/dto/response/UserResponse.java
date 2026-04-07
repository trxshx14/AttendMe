package edu.cit.cararag.attendme.dto.response;

import edu.cit.cararag.attendme.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private Role role;
    private Boolean isActive;
    private Boolean isOnline; // ✅ Added
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String profilePicUrl;

    public static UserResponse fromUser(edu.cit.cararag.attendme.entity.User user) {
        if (user == null) return null;

        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isActive(user.getIsActive() != null ? user.getIsActive() : false)
                .isOnline(user.getIsOnline() != null ? user.getIsOnline() : false) // ✅ Added
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .profilePicUrl(user.getProfilePicUrl())
                .build();
    }
}