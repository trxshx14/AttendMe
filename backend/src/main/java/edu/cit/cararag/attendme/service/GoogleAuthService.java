package edu.cit.cararag.attendme.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import edu.cit.cararag.attendme.dto.response.JwtResponse;
import edu.cit.cararag.attendme.entity.Role;
import edu.cit.cararag.attendme.entity.User;
import edu.cit.cararag.attendme.repository.UserRepository;
import edu.cit.cararag.attendme.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Value("${google.client.id}")
    private String googleClientId;

    /**
     * Handles Google login using the access_token flow (useGoogleLogin implicit flow).
     * Fetches user info from Google's userinfo endpoint using the access token.
     */
    public JwtResponse loginWithAccessToken(String accessToken) {
        // Fetch user info from Google
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + accessToken;

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map<String, Object> userInfo = response.getBody();

        if (userInfo == null || userInfo.get("email") == null) {
            throw new RuntimeException("Failed to fetch user info from Google");
        }

        String email    = (String) userInfo.get("email");
        String name     = (String) userInfo.get("name");
        String googleId = (String) userInfo.get("sub");
        String picture  = (String) userInfo.get("picture");

        return findOrCreateUser(email, name, googleId, picture);
    }

    /**
     * Handles Google login using ID token (credential response flow).
     */
    public JwtResponse loginWithIdToken(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new RuntimeException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String email    = payload.getEmail();
            String name     = (String) payload.get("name");
            String googleId = payload.getSubject();
            String picture  = (String) payload.get("picture");

            return findOrCreateUser(email, name, googleId, picture);

        } catch (Exception e) {
            throw new RuntimeException("Google token verification failed: " + e.getMessage());
        }
    }

    /**
     * Finds existing user by email or Google ID, or creates a new one.
     * Then issues JWT tokens.
     */
    private JwtResponse findOrCreateUser(String email, String name, String googleId, String picture) {
        // Try to find existing user
        Optional<User> existingUser = userRepository.findByEmail(email);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Update Google ID and profile pic if not set
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
            }
            if (user.getProfilePicUrl() == null && picture != null) {
                user.setProfilePicUrl(picture);
            }
            userRepository.save(user);
        } else {
            // Create new user from Google account
            user = new User();
            user.setEmail(email);
            user.setFullName(name != null ? name : email.split("@")[0]);
            user.setUsername(generateUsername(email));
            user.setGoogleId(googleId);
            user.setProfilePicUrl(picture);
            user.setPasswordHash("GOOGLE_AUTH_" + googleId); // placeholder — not used for login
            user.setRole(Role.TEACHER); // default role for Google sign-ins
            user.setIsActive(true);
            userRepository.save(user);
        }

        // Load UserDetails and generate JWT
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken  = jwtUtils.generateJwtToken(userDetails);
        String refreshToken = jwtUtils.generateRefreshToken(userDetails);

        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .profilePicUrl(user.getProfilePicUrl())
                .build();
    }

    private String generateUsername(String email) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        // Ensure uniqueness
        String username = base;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = base + counter++;
        }
        return username;
    }
}