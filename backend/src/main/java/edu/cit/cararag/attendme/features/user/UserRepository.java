package edu.cit.cararag.attendme.features.user;

import edu.cit.cararag.attendme.shared.entity.User;
import edu.cit.cararag.attendme.shared.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByGoogleId(String googleId);
    
    Boolean existsByUsername(String username);
    
    Boolean existsByEmail(String email);
    
    List<User> findByRole(Role role);
    
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.userId IN " +
      "(SELECT DISTINCT s.teacher.userId FROM SchoolClass s)")
    List<User> findTeachersWithClasses(@Param("role") Role role);
}