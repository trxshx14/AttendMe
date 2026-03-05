package com.attendme.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;  // ADD THIS IMPORT
import com.fasterxml.jackson.annotation.JsonProperty; // ADD THIS IMPORT

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;
    
    @Column(unique = true, nullable = false, length = 50)
    private String username;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(unique = true, nullable = false, length = 100)
    private String email;
    
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;
    
    @Column(name = "google_id", unique = true)
    private String googleId;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @JsonIgnore  // ADD THIS - prevents serialization of classes
    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SchoolClass> classes = new ArrayList<>();
    
    @JsonIgnore  // ADD THIS - prevents serialization of refreshTokens
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RefreshToken> refreshTokens = new ArrayList<>();
    
    @JsonProperty("isActive")  // ADD THIS - ensures proper JSON naming
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    // Helper methods
    public void addClass(SchoolClass schoolClass) {
        classes.add(schoolClass);
        schoolClass.setTeacher(this);
    }
    
    public void addRefreshToken(RefreshToken refreshToken) {
        refreshTokens.add(refreshToken);
        refreshToken.setUser(this);
    }
}