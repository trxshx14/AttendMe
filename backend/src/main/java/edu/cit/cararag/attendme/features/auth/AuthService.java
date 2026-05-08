package edu.cit.cararag.attendme.features.auth;

import edu.cit.cararag.attendme.features.auth.dto.LoginRequest;
import edu.cit.cararag.attendme.features.auth.dto.RegisterRequest;
import edu.cit.cararag.attendme.features.auth.dto.JwtResponse;
import edu.cit.cararag.attendme.features.user.dto.UserResponse;

public interface AuthService {
    JwtResponse login(LoginRequest request);
    UserResponse register(RegisterRequest request);
    UserResponse getCurrentUser();
}