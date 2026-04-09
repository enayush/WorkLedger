package com.sharmarefrigeration.workledger.ui

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sharmarefrigeration.workledger.model.User
import com.sharmarefrigeration.workledger.model.UserRole
import com.sharmarefrigeration.workledger.ui.accountant.AccountantScreen
import com.sharmarefrigeration.workledger.ui.accountant.AccountantViewModel
import com.sharmarefrigeration.workledger.ui.admin.AdminScreen
import com.sharmarefrigeration.workledger.ui.admin.AdminViewModel
import com.sharmarefrigeration.workledger.ui.employee.EmployeeScreen
import com.sharmarefrigeration.workledger.ui.employee.EmployeeViewModel
import com.sharmarefrigeration.workledger.ui.employee.LogWorkScreen
import com.sharmarefrigeration.workledger.ui.employee.WorkLedgerScreen
import com.sharmarefrigeration.workledger.ui.profile.ProfileScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    currentUser: User,
    employeeViewModel: EmployeeViewModel,
    accountantViewModel: AccountantViewModel,
    adminViewModel: AdminViewModel,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()

    // This tells us what screen the user is currently looking at
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val isEmployeeLoading by employeeViewModel.isLoading.collectAsState()

    val topBarTitle = when (currentRoute) {
        "dashboard" -> {
            when (currentUser.role) {
                UserRole.ADMIN -> "Pending Approvals"
                UserRole.ACCOUNTANT -> "Pending Billing"
                UserRole.EMPLOYEE -> "Tasks"
                else -> "Dashboard"
            }
        }
        "profile" -> "Profile"
        "log_work" -> "Log Completed Work"
        "employee_history" -> "My Work Ledger"
        "accountant_history" -> "Billing Archive"
        else -> "Service Manager"
    }

    Scaffold(
        topBar = {
            if (currentRoute != "dashboard" && currentRoute != "profile") {
                TopAppBar(
                    title = { Text(topBarTitle) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    navigationIcon = {
                        if (currentRoute == "log_work") {
                            IconButton(
                                onClick = {
                                    if (isEmployeeLoading) {
                                        Toast.makeText(context, "Please wait, saving job details...", Toast.LENGTH_SHORT).show()
                                    } else {
                                        navController.popBackStack()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = if (isEmployeeLoading) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else LocalContentColor.current
                                )
                            }
                        }
                        if(currentRoute == "employee_history" || currentRoute == "accountant_history") {
                            IconButton(
                                onClick = { navController.popBackStack() }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            // We only show the bottom bar on the main tabs!
            if (currentRoute == "dashboard" || currentRoute == "profile") {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        selected = currentRoute == "dashboard",
                        onClick = { navController.navigate("dashboard") { launchSingleTop = true } }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        selected = currentRoute == "profile",
                        onClick = { navController.navigate("profile") { launchSingleTop = true } }
                    )
                }
            }
        }
    ) { paddingValues ->
        // The NavHost swaps the screens inside the Scaffold
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(paddingValues)
        ) {

            // 1. The Dashboard Route (Changes based on who is logged in!)
            composable("dashboard") {
                when (currentUser.role) {
                    UserRole.ADMIN -> AdminScreen(viewModel = adminViewModel)
                    UserRole.ACCOUNTANT -> AccountantScreen(viewModel = accountantViewModel)
                    UserRole.EMPLOYEE -> EmployeeScreen(
                        viewModel = employeeViewModel,
                        onLogNewWorkClick = { navController.navigate("log_work") }
                    )
                    UserRole.UNKNOWN -> { /* Show error */ }
                }
            }

            // 2. The Shared Profile Route
            composable("profile") {
                ProfileScreen(
                    user = currentUser,
                    adminViewModel = if (currentUser.role == UserRole.ADMIN) adminViewModel else null,
                    accountantViewModel= if (currentUser.role == UserRole.ACCOUNTANT) accountantViewModel else null,
                    onNavigateToHistory = {
                        if (currentUser.role == UserRole.EMPLOYEE) {
                            navController.navigate("employee_history")
                        } else if (currentUser.role == UserRole.ACCOUNTANT) {
                            navController.navigate("accountant_history")
                        }
                    },
                    onLogout = onLogout
                )
            }

            // 3. The Camera Form Route (Only employees go here)
            composable("log_work") {
                LogWorkScreen(
                    viewModel = employeeViewModel,
                    onBack = { navController.popBackStack() },
                    onTaskSaved = { navController.popBackStack() }
                )
            }

            composable("employee_history") {
                WorkLedgerScreen(
                    viewModel = employeeViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("accountant_history") {
                com.sharmarefrigeration.workledger.ui.accountant.BillingArchiveScreen(
                    viewModel = accountantViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
