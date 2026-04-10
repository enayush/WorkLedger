package com.sharmarefrigeration.workledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.sharmarefrigeration.workledger.ui.auth.AuthViewModel
import com.sharmarefrigeration.workledger.ui.auth.LoginScreen

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    // Instantiate the ViewModels at the Activity level
    private val authViewModel: AuthViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return AuthViewModel(applicationContext) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
    private val employeeViewModel: com.sharmarefrigeration.workledger.ui.employee.EmployeeViewModel by viewModels()
    private val accountantViewModel: com.sharmarefrigeration.workledger.ui.accountant.AccountantViewModel by viewModels()
    private val adminViewModel: com.sharmarefrigeration.workledger.ui.admin.AdminViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        setContent {
            MaterialTheme {
                val authState by authViewModel.authState.collectAsStateWithLifecycle()

                when (val state = authState) {
                    is com.sharmarefrigeration.workledger.ui.auth.AuthState.InitialChecking -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("loading...", color = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                    is com.sharmarefrigeration.workledger.ui.auth.AuthState.Authenticated -> {
                        // User is logged in! Pass their data into the Shell.
                        val currentUser = state.user

                        com.sharmarefrigeration.workledger.ui.AppShell(
                            currentUser = currentUser,
                            employeeViewModel = employeeViewModel,
                            accountantViewModel = accountantViewModel,
                            adminViewModel = adminViewModel,
                            onLogout = {
                                auth.signOut() // Log out of Firebase
                                authViewModel.clearCache() // clear saved preferences
                                authViewModel.setIdle() // Reset viewmodel
                            }
                        )
                    }
                    else -> {
                        // User is NOT logged in. Show the new Username/Password Screen.
                        LoginScreen(viewModel = authViewModel)
                    }
                }
            }
        }
    }
}