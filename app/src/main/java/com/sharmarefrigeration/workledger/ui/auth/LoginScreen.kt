package com.sharmarefrigeration.workledger.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onSendOtp: (String) -> Unit,
    onVerifyOtp: (String) -> Unit
) {
    val authState by viewModel.authState.collectAsState()

    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Service Manager", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        when (authState) {
            is AuthState.Loading -> CircularProgressIndicator()

            is AuthState.OtpSent -> {
                // OTP Field - Restricted to 6 digits
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = {
                        // Only allow numbers and max 6 characters
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            otpCode = it
                        }
                    },
                    label = { Text("Enter OTP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onVerifyOtp(otpCode) },
                    enabled = otpCode.length == 6 // Button only active when 6 digits are entered
                ) {
                    Text("Verify")
                }
            }

            else -> {
                // Phone Number Field - Restricted to 10 digits with a locked +91 prefix
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        // Only allow numbers and max 10 characters
                        if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                            phoneNumber = it
                        }
                    },
                    label = { Text("Phone Number") },
                    prefix = { Text("+91 ") }, // Visually locks the prefix in the UI
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    // We append the +91 under the hood before sending it to Firebase
                    onClick = { onSendOtp("+91$phoneNumber") },
                    enabled = phoneNumber.length == 10 // Button only active when 10 digits are entered
                ) {
                    Text("Send OTP")
                }
            }
        }

        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}