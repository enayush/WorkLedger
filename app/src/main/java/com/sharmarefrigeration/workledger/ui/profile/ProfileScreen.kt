package com.sharmarefrigeration.workledger.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sharmarefrigeration.workledger.model.User
import com.sharmarefrigeration.workledger.model.UserRole
import com.sharmarefrigeration.workledger.ui.accountant.AccountantViewModel
import com.sharmarefrigeration.workledger.ui.admin.AddUserDialog
import com.sharmarefrigeration.workledger.ui.admin.AdminViewModel
import com.sharmarefrigeration.workledger.ui.components.ProfileActionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User,
    adminViewModel: AdminViewModel? = null, // Only passed if user is an Admin
    accountantViewModel: AccountantViewModel?,
    onNavigateToHistory: () -> Unit,
    onLogout: () -> Unit,
) {
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold() { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // User Avatar
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(100.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.padding(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // User Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = user.phoneNumber,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Role Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = user.role.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Role-Specific Sections
                if (user.role == UserRole.ADMIN && adminViewModel != null) {
                    Text(
                        text = "Admin Controls",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ProfileActionItem(
                        title = "Register New Employee",
                        icon = Icons.Default.PersonAdd,
                        onClick = { showAddUserDialog = true }
                    )
                }

                if (user.role == UserRole.ACCOUNTANT && accountantViewModel != null) {
                    val totalValue by accountantViewModel.todayTotalValue.collectAsState()
                    val billsProcessed by accountantViewModel.todayBillsProcessed.collectAsState()

                    Text(
                        text = "End of Day Summary",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Total Value", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("₹${totalValue}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Bills Processed", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text("$billsProcessed", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ProfileActionItem(
                        title = "View Full Billing Archive",
                        icon = Icons.Default.History,
                        onClick = onNavigateToHistory
                    )
                }

                if (user.role == UserRole.EMPLOYEE) {
                    Text(
                        text = "My Tools",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ProfileActionItem(
                        title = "My Work History",
                        icon = Icons.Default.History,
                        onClick = onNavigateToHistory
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )

                @Suppress("DEPRECATION")
                ProfileActionItem(
                    title = "Log Out",
                    icon = Icons.Default.ExitToApp,
                    onClick = { showLogoutDialog = true },
                    titleColor = MaterialTheme.colorScheme.error,
                    iconTint = MaterialTheme.colorScheme.error,
                    showArrow = false
                )
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out of your account?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Log Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddUserDialog && adminViewModel != null) {
        val isLoading by adminViewModel.isLoading.collectAsState()
        AddUserDialog(
            isSubmitting = isLoading,
            onDismiss = { showAddUserDialog = false },
            onSubmit = { name, phone, role ->
                adminViewModel.registerNewUser(
                    name = name,
                    phoneNumber = phone,
                    role = role,
                    onSuccess = { showAddUserDialog = false },
                    onError = { /* Show error */ }
                )
            }
        )
    }
}
