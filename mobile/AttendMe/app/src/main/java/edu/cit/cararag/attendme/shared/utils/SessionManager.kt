package edu.cit.cararag.attendme.shared.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "attendme_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        const val KEY_ACCESS_TOKEN  = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID       = "user_id"
        const val KEY_USERNAME      = "username"
        const val KEY_FULL_NAME     = "full_name"
        const val KEY_EMAIL         = "email"
        const val KEY_ROLE          = "role"
        const val KEY_PROFILE_PIC   = "profile_pic_url"
    }

    fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: Long,
        username: String,
        fullName: String,
        email: String,
        role: String,
        profilePicUrl: String? = null
    ) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN,  accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_USER_ID,         userId)
            putString(KEY_USERNAME,      username)
            putString(KEY_FULL_NAME,     fullName)
            putString(KEY_EMAIL,         email)
            putString(KEY_ROLE,          role)
            putString(KEY_PROFILE_PIC,   profilePicUrl)
            apply()
        }
    }

    fun getAccessToken(): String?  = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun getUserId(): Long          = prefs.getLong(KEY_USER_ID, -1L)
    fun getUsername(): String?     = prefs.getString(KEY_USERNAME, null)
    fun getFullName(): String?     = prefs.getString(KEY_FULL_NAME, null)
    fun getEmail(): String?        = prefs.getString(KEY_EMAIL, null)
    fun getRole(): String?         = prefs.getString(KEY_ROLE, null)
    fun getProfilePicUrl(): String? = prefs.getString(KEY_PROFILE_PIC, null)

    fun isLoggedIn(): Boolean = getAccessToken() != null

    fun isAdmin(): Boolean    = getRole()?.uppercase() == "ADMIN"
    fun isTeacher(): Boolean  = getRole()?.uppercase() == "TEACHER"

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}