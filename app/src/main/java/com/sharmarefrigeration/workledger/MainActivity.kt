package com.sharmarefrigeration.workledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.sharmarefrigeration.workledger.ui.auth.AuthViewModel
import com.sharmarefrigeration.workledger.ui.auth.LoginScreen
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private var storedVerificationId: String? = null

    // Instantiate the ViewModel at the Activity level
    private val authViewModel: AuthViewModel by viewModels()

    private val employeeViewModel: com.sharmarefrigeration.workledger.ui.employee.EmployeeViewModel by viewModels()
    private val accountantViewModel: com.sharmarefrigeration.workledger.ui.accountant.AccountantViewModel by viewModels()
    private val adminViewModel: com.sharmarefrigeration.workledger.ui.admin.AdminViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        setContent {
            MaterialTheme {
                val authState by authViewModel.authState.collectAsState()

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

                    if (authState is com.sharmarefrigeration.workledger.ui.auth.AuthState.Authenticated) {
                        // User is logged in! Pass their data into the Shell.
                        val currentUser = (authState as com.sharmarefrigeration.workledger.ui.auth.AuthState.Authenticated).user

                        com.sharmarefrigeration.workledger.ui.AppShell(
                            currentUser = currentUser,
                            employeeViewModel = employeeViewModel,
                            accountantViewModel = accountantViewModel,
                            adminViewModel = adminViewModel,
                            onLogout = {
                                auth.signOut() // Log out of Firebase
                                authViewModel.setIdle() // Reset viewmodel
                            }
                        )
                    } else {
                        // User is NOT logged in. Show Login Screen.
                        LoginScreen(
                            viewModel = authViewModel,
                            onSendOtp = { phoneNumber -> sendOtp(phoneNumber) },
                            onVerifyOtp = { otpCode -> verifyCode(otpCode) }
                        )
                    }
                }
            }
        }
    }

    private fun sendOtp(phoneNumber: String) {
        authViewModel.setLoading()

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this)                 // Activity (for callback binding)
            .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This happens if Android auto-reads the SMS. We sign in immediately.
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // This happens on invalid numbers, or if you forgot the SHA-1 key!
            authViewModel.setError(e.localizedMessage ?: "Verification Failed")
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            // The SMS was sent successfully! Save the ID and update the UI.
            storedVerificationId = verificationId
            authViewModel.setOtpSent()
        }
    }

    private fun verifyCode(code: String) {
        authViewModel.setLoading()
        val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null && user.phoneNumber != null) {
                        // ✅ Pass the phone number here!
                        authViewModel.verifyUserRole(user.phoneNumber!!)
                    }
                } else {
                    authViewModel.setError("Invalid OTP Code.")
                }
            }
    }
}