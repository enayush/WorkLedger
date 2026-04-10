package com.sharmarefrigeration.workledger.ui.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sharmarefrigeration.workledger.data.UserRepository
import com.sharmarefrigeration.workledger.model.User
import com.sharmarefrigeration.workledger.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

sealed class AuthState {
    object Idle : AuthState()
    object InitialChecking : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: com.sharmarefrigeration.workledger.model.User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(context: Context) : ViewModel() {
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(AuthState.InitialChecking)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun setIdle() { _authState.value = AuthState.Idle }

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        val currentUser = auth.currentUser
        // If Firebase remembers an email session, try to log them in automatically
        if (currentUser != null && currentUser.email != null) {
            // Extract the username from the fake email (e.g., "rohit@sharma.local" -> "rohit")
            val username = currentUser.email!!.substringBefore("@")

            // 1. Instantly check if we have a cached profile and allow them in
            val userId = prefs.getString("user_id", null)
            val usernamePrefs = prefs.getString("user_username", null)

            if (userId != null && usernamePrefs != null) {
                try {
                    val cachedUser = User(
                        id = userId,
                        phoneNumber = prefs.getString("user_phone", "") ?: "",
                        name = prefs.getString("user_name", "") ?: "",
                        username = usernamePrefs,
                        role = UserRole.valueOf(prefs.getString("user_role", UserRole.UNKNOWN.name) ?: UserRole.UNKNOWN.name),
                        isActive = prefs.getBoolean("user_is_active", true)
                    )
                    _authState.value = AuthState.Authenticated(cachedUser)
                } catch (e: Exception) {
                    // parsing failed, proceed to normal verification
                }
            }

            // 2. Start background db validation
            verifyUserRole(username)
        } else {
            _authState.value = AuthState.Idle
        }
    }

    fun loginWithUsername(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Secretly format it as an email for Firebase
                val fakeEmail = "${username.trim().lowercase()}@sharma.local"

                // Attempt to sign in
                auth.signInWithEmailAndPassword(fakeEmail, password).await()

                // If successful, verify they are still active in our database
                verifyUserRole(username.trim().lowercase())

            } catch (e: Exception) {
                _authState.value = AuthState.Error("Invalid Username or Password.")
            }
        }
    }

    private fun verifyUserRole(username: String) {
        viewModelScope.launch {
            try {
                val userProfile = withTimeoutOrNull(8000) {
                    userRepository.getUserProfileByUsername(username)
                }

                if (userProfile != null) {
                    if (userProfile.isActive) {
                        prefs.edit()
                            .putString("user_id", userProfile.id)
                            .putString("user_phone", userProfile.phoneNumber)
                            .putString("user_name", userProfile.name)
                            .putString("user_username", userProfile.username)
                            .putString("user_role", userProfile.role.name)
                            .putBoolean("user_is_active", userProfile.isActive)
                            .apply()

                        _authState.value = AuthState.Authenticated(userProfile)
                    } else {
                        auth.signOut()
                        clearCache()
                        _authState.value = AuthState.Error("Your access has been revoked by the Admin.")
                    }
                } else {
                    val hasCachedSession = prefs.getString("user_id", null) != null

                    if (hasCachedSession) {
                        // Do nothing! They are offline, but checkExistingSession() already let them in.
                        // Firebase offline persistence will queue their reads/writes.
                    } else {
                        auth.signOut()
                        clearCache()
                        _authState.value = AuthState.Error("Network error. Please check your connection and log in again.")
                    }
                }
            } catch (e: Exception) {
                val hasCachedSession = prefs.getString("user_id", null) != null
                if (!hasCachedSession) {
                    auth.signOut()
                    clearCache()
                    _authState.value = AuthState.Error("Failed to verify session: ${e.localizedMessage}")
                }
            }
        }
    }

    fun clearCache() {
        prefs.edit().clear().apply()
    }
}