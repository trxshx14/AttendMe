package edu.cit.cararag.attendme.dto.requestdto;

import lombok.Data;

@Data
public class GoogleAuthRequest {
    private String idToken;      // ID token from Google (for credential flow)
    private String accessToken;  // Access token from Google (for implicit flow)
}
