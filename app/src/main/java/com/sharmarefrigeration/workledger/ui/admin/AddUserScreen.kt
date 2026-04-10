package com.sharmarefrigeration.workledger.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.sharmarefrigeration.workledger.model.UserRole
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserScreen(viewModel: AdminViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.EMPLOYEE) }

    // If this has a value, it means creation was successful
    var generatedPassword by remember { mutableStateOf<String?>(null) }

    val isFormValid = name.isNotBlank() && phone.length == 10 && username.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register Team Member") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isLoading) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (generatedPassword == null) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.registerNewUser(
                                context = context,
                                name = name,
                                phoneNumber = phone,
                                username = username,
                                role = role,
                                onSuccess = { password -> generatedPassword = password },
                                onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                            )
                        },
                        enabled = isFormValid && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Create Account")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (generatedPassword != null) {
            // --- SUCCESS STATE ---
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Account Created!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Share these login details securely. The password cannot be recovered once you leave this screen.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Username", style = MaterialTheme.typography.labelMedium)
                        Text(username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Password", style = MaterialTheme.typography.labelMedium)
                        Text(generatedPassword!!, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("Done")
                }
            }
        } else {
            // --- INPUT FORM ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // --- PERSONAL DETAILS ---
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Personal Details", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name", style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = MaterialTheme.shapes.medium
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { if (it.length <= 10) phone = it },
                            label = { Text("Phone Number", style = MaterialTheme.typography.bodySmall) },
                            prefix = { Text("+91 ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }

                // --- ACCOUNT DETAILS ---
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Account Details", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it.replace(" ", "").lowercase() }, // Prevent spaces
                            label = { Text("App Username", style = MaterialTheme.typography.bodySmall) },
                            placeholder = { Text("e.g. rohit_tech", style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = MaterialTheme.shapes.medium
                        )

                        Text("Select Role:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = role == UserRole.EMPLOYEE,
                                    onClick = { role = UserRole.EMPLOYEE },
                                    modifier = Modifier.size(36.dp)
                                )
                                Text("Technician", style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = role == UserRole.ACCOUNTANT,
                                    onClick = { role = UserRole.ACCOUNTANT },
                                    modifier = Modifier.size(36.dp)
                                )
                                Text("Accountant", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}