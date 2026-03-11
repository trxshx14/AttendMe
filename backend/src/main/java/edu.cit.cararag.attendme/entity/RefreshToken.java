package edu.cit.cararag.attendme.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;
    
    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // AFTER
    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    // AFTER
    @Column(name = "is_revoked", columnDefinition = "boolean default false")
    private boolean isRevoked = false;
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
