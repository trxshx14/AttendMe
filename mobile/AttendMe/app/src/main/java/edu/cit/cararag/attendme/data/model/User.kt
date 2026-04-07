package edu.cit.cararag.attendme.data.model

data class User(
    val userId: Long,
    val username: String,
    val email: String,
    val fullName: String?,
    val role: String,
    val isActive: Boolean?,
    val isOnline: Boolean?, // ✅ Added
    val lastLogin: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val profilePicUrl: String?
)