package com.sharmarefrigeration.workledger.ui

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
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
import com.sharmarefrigeration.workledger.ui.accountant.*
import com.sharmarefrigeration.workledger.ui.admin.*
import com.sharmarefrigeration.workledger.ui.employee.*
import com.sharmarefrigeration.workledger.ui.profile.ProfileScreen
import androidx.navigation.navArgument
import androidx.navigation.NavType

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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    val isEmployeeLoading by employeeViewModel.isLoading.collectAsState()

    // Determine the start destination based on role
    val startDest = when (currentUser.role) {
        UserRole.ADMIN -> "admin_ops"
        UserRole.ACCOUNTANT -> "acc_create"
        else -> "dashboard"
    }

    Scaffold(
        bottomBar = {
            val isMainTab = currentRoute in listOf("dashboard", "profile", "acc_create", "acc_distribute", "acc_collect", "admin_ops", "admin_approvals", "admin_directory")

            if (isMainTab) {
                NavigationBar {
                    // --- ADMIN TABS ---
                    when (currentUser.role) {
                        UserRole.ADMIN -> {
                            NavigationBarItem(
                                icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Ops") },
                                label = { Text("Ops", maxLines = 1) },
                                selected = currentRoute == "admin_ops",
                                onClick = {
                                    navController.navigate("admin_ops") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = "Approvals") },
                                label = { Text("Approve", maxLines = 1) },
                                selected = currentRoute == "admin_approvals",
                                onClick = {
                                    navController.navigate("admin_approvals") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.People, contentDescription = "Team") },
                                label = { Text("Team", maxLines = 1) },
                                selected = currentRoute == "admin_directory",
                                onClick = {
                                    navController.navigate("admin_directory") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                label = { Text("Profile", maxLines = 1) },
                                selected = currentRoute == "profile",
                                onClick = {
                                    navController.navigate("profile") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        // --- ACCOUNTANT TABS ---
                        UserRole.ACCOUNTANT -> {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Receipt, contentDescription = "Create") },
                                label = { Text("Create", maxLines = 1) },
                                selected = currentRoute == "acc_create",
                                onClick = {
                                    navController.navigate("acc_create") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send") },
                                label = { Text("Send", maxLines = 1) },
                                selected = currentRoute == "acc_distribute",
                                onClick = {
                                    navController.navigate("acc_distribute") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Payments, contentDescription = "Collect") },
                                label = { Text("Collect", maxLines = 1) },
                                selected = currentRoute == "acc_collect",
                                onClick = {
                                    navController.navigate("acc_collect") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                label = { Text("Profile", maxLines = 1) },
                                selected = currentRoute == "profile",
                                onClick = {
                                    navController.navigate("profile") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        // --- EMPLOYEE TABS ---
                        else -> {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                                label = { Text("Dashboard") },
                                selected = currentRoute == "dashboard",
                                onClick = {
                                    navController.navigate("dashboard") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                label = { Text("Profile") },
                                selected = currentRoute == "profile",
                                onClick = {
                                    navController.navigate("profile") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(paddingValues)
        ) {
            // --- PROFILE (Shared) ---
            composable("profile") {
                ProfileScreen(
                    user = currentUser,
                    adminViewModel = if (currentUser.role == UserRole.ADMIN) adminViewModel else null,
                    accountantViewModel = if (currentUser.role == UserRole.ACCOUNTANT) accountantViewModel else null,
                    onNavigateToTaskManagement = { if (currentUser.role == UserRole.ADMIN) navController.navigate("admin_task_management") },
                    onNavigateToReports = { if (currentUser.role == UserRole.ADMIN) navController.navigate("admin_reports") },
                    onNavigateToAddUser = { navController.navigate("admin_add_user") },
                    onNavigateToHistory = {
                        if (currentUser.role == UserRole.EMPLOYEE) navController.navigate("employee_history")
                        else if (currentUser.role == UserRole.ACCOUNTANT) navController.navigate("accountant_history")
                    },
                    onLogout = onLogout
                )
            }

            // --- ADMIN ROUTES ---
            composable("admin_ops") { AdminOperationsScreen(adminViewModel) }
            composable("admin_approvals") { AdminApprovalsScreen(adminViewModel) }

            composable("admin_directory") {
                AdminDirectoryScreen(
                    viewModel = adminViewModel,
                    onTechnicianClick = { employeeId ->
                        // Navigate to the Technician's Task Ledger
                        navController.navigate("admin_view_employee_history/$employeeId")
                    },
                    onAccountantClick = { employeeId ->
                        // Navigate to the Accountant's Billing Archive
                        navController.navigate("admin_view_accountant_history/$employeeId")
                    }
                )
            }

            composable(
                route = "admin_view_employee_history/{employeeId}",
                arguments = listOf(navArgument("employeeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val employeeId = backStackEntry.arguments?.getString("employeeId")

                WorkLedgerScreen(
                    viewModel = employeeViewModel,
                    targetUserId = employeeId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "admin_view_accountant_history/{employeeId}",
                arguments = listOf(navArgument("employeeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val employeeId = backStackEntry.arguments?.getString("employeeId")

                BillingArchiveScreen(
                    viewModel = accountantViewModel,
                    targetUserId = employeeId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("admin_task_management") {
                AdminTaskManagementScreen(
                    viewModel = adminViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("admin_reports") {
                AdminReportsScreen(
                    viewModel = adminViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("admin_add_user") {
                AddUserScreen(
                    viewModel = adminViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // --- ACCOUNTANT ROUTES ---
            composable("acc_create") { CreateBillScreen(accountantViewModel) }
            composable("acc_distribute") { DistributeBillScreen(accountantViewModel) }
            composable("acc_collect") { CollectPaymentScreen(accountantViewModel) }
            composable("accountant_history") { BillingArchiveScreen(accountantViewModel, onBack = { navController.popBackStack() }) }

            // --- EMPLOYEE ROUTES ---
            composable("dashboard") {
                EmployeeScreen(
                    viewModel = employeeViewModel,
                    onLogNewWorkClick = { taskId ->
                        if (taskId != null) navController.navigate("log_work?taskId=$taskId")
                        else navController.navigate("log_work") // Ad-hoc route
                    }
                )
            }
            composable(
                route = "log_work?taskId={taskId}",
                arguments = listOf(androidx.navigation.navArgument("taskId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId")

                LogWorkScreen(
                    viewModel = employeeViewModel,
                    employeeName = currentUser.name,
                    taskIdToComplete = taskId, // <--- Pass it down
                    onBack = { navController.popBackStack() },
                    onTaskSaved = { navController.popBackStack() }
                )
            }
            composable("employee_history") { WorkLedgerScreen(employeeViewModel, onBack = { navController.popBackStack() }) }
        }
    }
}