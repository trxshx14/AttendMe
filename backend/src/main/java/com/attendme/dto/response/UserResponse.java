package com.attendme.dto.response;

import com.attendme.entity.Role;
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
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    
    
    /**
     * Static factory method to create UserResponse from User entity
     * @param user the User entity
     * @return UserResponse DTO
     */
    public static UserResponse fromUser(com.attendme.entity.User user) {
    if (user == null) {
        return null;
    }
    
    return UserResponse.builder()
            .userId(user.getUserId())
            .username(user.getUsername())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole())
            .isActive(user.getIsActive() != null ? user.getIsActive() : false)
            .lastLogin(user.getLastLogin())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
}
}
