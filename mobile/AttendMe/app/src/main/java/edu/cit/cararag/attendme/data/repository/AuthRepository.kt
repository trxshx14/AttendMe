package edu.cit.cararag.attendme.data.repository

import edu.cit.cararag.attendme.data.model.GoogleLoginRequest
import edu.cit.cararag.attendme.data.model.LoginRequest
import edu.cit.cararag.attendme.data.model.LoginResponse
import edu.cit.cararag.attendme.data.remote.RetrofitClient

class AuthRepository {

    private val api = RetrofitClient.instance

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success && body.data != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(body.error?.message ?: "Login failed"))
                }
            } else {
                Result.failure(Exception("Invalid email or password"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server. Check your connection."))
        }
    }

    suspend fun googleLogin(accessToken: String): Result<LoginResponse> {
        return try {
            val response = api.googleLogin(
                GoogleLoginRequest(idToken = accessToken) // ✅ Android gives idToken, send as idToken
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success && body.data != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception(body.error?.message ?: "Google login failed"))
                }
            } else {
                Result.failure(Exception("Google login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server. Check your connection."))
        }
    }
}