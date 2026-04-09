package com.sharmarefrigeration.workledger.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharmarefrigeration.workledger.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object OtpSent : AuthState()
    data class Authenticated(val user: com.sharmarefrigeration.workledger.model.User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val userRepository = UserRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun setLoading() { _authState.value = AuthState.Loading }
    fun setOtpSent() { _authState.value = AuthState.OtpSent }
    fun setError(message: String) { _authState.value = AuthState.Error(message) }

    fun setIdle() { _authState.value = AuthState.Idle }

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null && currentUser.phoneNumber != null) {
            // Firebase remembers them! Verify their role and log them straight in.
            verifyUserRole(currentUser.phoneNumber!!)
        }
    }

    // Called after Firebase successfully verifies the OTP
    fun verifyUserRole(phoneNumber: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            // Call the new repository function
            val userProfile = userRepository.getUserProfileByPhone(phoneNumber)

            if (userProfile != null && userProfile.isActive) {
                _authState.value = AuthState.Authenticated(userProfile)
            } else {
                _authState.value = AuthState.Error("Number not registered by Admin.")
            }
        }
    }
}