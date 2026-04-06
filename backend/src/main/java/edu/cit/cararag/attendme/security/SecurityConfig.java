package edu.cit.cararag.attendme.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList(
        "http://localhost:3000",      // Web app
        "http://localhost:8888",      // Local backend
        "http://10.0.2.2:8888",       // Android emulator → your machine
        "http://10.0.2.2:3000"        // Android emulator → web
    ));
    configuration.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
    ));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Auth - fully public
                .requestMatchers("/api/auth/**").permitAll()
                // Test endpoints - fully public
                .requestMatchers("/api/test/**").permitAll()
                .requestMatchers("/api/test-auth/**").permitAll()
                .requestMatchers("/simple-test").permitAll()
                .requestMatchers("/api/users/ping").permitAll()
                .requestMatchers("/api/users/test").permitAll()
                // GET users - public (no role needed to list/view)
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users/**").permitAll()
                // Write/delete on users - must be authenticated (role checked by @PreAuthorize)
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/users/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/users/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/users/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/users/**").authenticated()
                // Everything else requires auth
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}