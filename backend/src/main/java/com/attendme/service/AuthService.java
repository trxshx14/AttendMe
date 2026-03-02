package com.attendme.service;

import com.attendme.dto.requestdto.LoginRequest;
import com.attendme.dto.requestdto.RegisterRequest;
import com.attendme.dto.response.JwtResponse;
import com.attendme.dto.response.UserResponse;

public interface AuthService {
    JwtResponse login(LoginRequest request);
    UserResponse register(RegisterRequest request);
    UserResponse getCurrentUser();
}