package edu.cit.cararag.attendme.features.auth.dto;

import lombok.Data;

@Data
public class GoogleAuthRequest {
    private String idToken;      // ID token from Google (for credential flow)
    private String accessToken;  // Access token from Google (for implicit flow)
}
