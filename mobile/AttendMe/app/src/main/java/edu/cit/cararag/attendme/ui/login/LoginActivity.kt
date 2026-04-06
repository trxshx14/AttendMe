package edu.cit.cararag.attendme.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.textfield.TextInputEditText
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import edu.cit.cararag.attendme.R
import edu.cit.cararag.attendme.data.remote.RetrofitClient
import edu.cit.cararag.attendme.ui.admin.AdminDashboardActivity
import edu.cit.cararag.attendme.ui.teacher.TeacherDashboardActivity
import edu.cit.cararag.attendme.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoogleLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    // ✅ Paste your Web Client ID from Google Cloud Console here
    // Same one used in your React app's GoogleOAuthProvider clientId
    private val googleWebClientId = "119199394548-v3gge1t3omphebmr8p8d8mlcjrib2ubs.apps.googleusercontent.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            navigateToDashboard(sessionManager.getRole() ?: "TEACHER")
            return
        }

        etEmail        = findViewById(R.id.etEmail)
        etPassword     = findViewById(R.id.etPassword)
        btnLogin       = findViewById(R.id.btnLogin)
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin)
        progressBar    = findViewById(R.id.progressBar)
        tvError        = findViewById(R.id.tvError)

        btnLogin.setOnClickListener {
            val email    = etEmail.text.toString()
            val password = etPassword.text.toString()
            viewModel.login(email, password)
        }

        btnGoogleLogin.setOnClickListener {
            launchGoogleSignIn()
        }

        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Loading -> {
                    btnLogin.isEnabled       = false
                    btnGoogleLogin.isEnabled = false
                    progressBar.visibility   = View.VISIBLE
                    tvError.visibility       = View.GONE
                }
                is LoginState.Success -> {
                    progressBar.visibility   = View.GONE
                    btnLogin.isEnabled       = true
                    btnGoogleLogin.isEnabled = true

                    val data = state.data
                    RetrofitClient.setToken(data.accessToken)
                    sessionManager.saveSession(
                        accessToken   = data.accessToken,
                        refreshToken  = data.refreshToken,
                        userId        = data.userId,
                        username      = data.username,
                        fullName      = data.fullName ?: data.username,
                        email         = data.email,
                        role          = data.role,
                        profilePicUrl = data.profilePicUrl
                    )
                    navigateToDashboard(data.role)
                }
                is LoginState.Error -> {
                    progressBar.visibility   = View.GONE
                    btnLogin.isEnabled       = true
                    btnGoogleLogin.isEnabled = true
                    tvError.text             = state.message
                    tvError.visibility       = View.VISIBLE
                }
            }
        }
    }

    private fun launchGoogleSignIn() {
        val credentialManager = CredentialManager.create(this)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(googleWebClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity
                )
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    android.util.Log.d("GoogleLogin", "Got ID token, sending to backend...")
                    viewModel.googleLogin(idToken)
                } else {
                    showError("Unsupported credential type")
                }
            } catch (e: GetCredentialException) {
                android.util.Log.e("GoogleLogin", "Credential error: ${e.message}", e)
                showError("Google sign-in failed: ${e.localizedMessage}")
            } catch (e: Exception) {
                android.util.Log.e("GoogleLogin", "Unexpected error: ${e.message}", e)
                showError("Google sign-in failed. Please try again.")
            }
        }
    }

    private fun showError(message: String) {
        tvError.text       = message
        tvError.visibility = View.VISIBLE
    }

    private fun navigateToDashboard(role: String) {
        val intent = if (role.uppercase() == "ADMIN") {
            Intent(this, AdminDashboardActivity::class.java)
        } else {
            Intent(this, TeacherDashboardActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}