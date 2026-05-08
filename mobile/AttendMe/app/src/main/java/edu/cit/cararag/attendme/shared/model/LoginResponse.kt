package edu.cit.cararag.attendme.shared.model

data class LoginResponse(
    val success: Boolean,
    val message: String?,
    val data: JwtData?,
    val error: ErrorData?,
    val timestamp: String?
)

data class JwtData(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String?,
    val userId: Long,
    val username: String,
    val email: String,
    val fullName: String?,
    val role: String,
    val profilePicUrl: String?
)