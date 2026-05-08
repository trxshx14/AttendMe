package edu.cit.cararag.attendme.shared.model

data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?,
    val error: ErrorData?,
    val timestamp: String?
)

data class ErrorData(
    val code: String?,
    val message: String?,
    val details: Any?
)