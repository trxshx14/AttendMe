package edu.cit.cararag.attendme.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.cit.cararag.attendme.data.model.JwtData
import edu.cit.cararag.attendme.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("Email and password are required")
            return
        }
        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            val result = repository.login(email.trim(), password)
            result.fold(
                onSuccess = { response ->
                    if (response.success && response.data != null) {
                        _loginState.value = LoginState.Success(response.data)
                    } else {
                        _loginState.value = LoginState.Error(
                            response.error?.message ?: "Login failed"
                        )
                    }
                },
                onFailure = { exception ->
                    _loginState.value = LoginState.Error(
                        exception.message ?: "An error occurred"
                    )
                }
            )
        }
    }

    fun googleLogin(accessToken: String) {
        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            val result = repository.googleLogin(accessToken)
            result.fold(
                onSuccess = { response ->
                    if (response.success && response.data != null) {
                        _loginState.value = LoginState.Success(response.data)
                    } else {
                        _loginState.value = LoginState.Error(
                            response.error?.message ?: "Google login failed"
                        )
                    }
                },
                onFailure = { exception ->
                    _loginState.value = LoginState.Error(
                        exception.message ?: "Google login failed"
                    )
                }
            )
        }
    }
}

sealed class LoginState {
    object Loading : LoginState()
    data class Success(val data: JwtData) : LoginState()
    data class Error(val message: String) : LoginState()
}