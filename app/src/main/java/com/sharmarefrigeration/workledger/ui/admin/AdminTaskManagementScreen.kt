package com.sharmarefrigeration.workledger.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sharmarefrigeration.workledger.model.Task
import com.sharmarefrigeration.workledger.ui.components.SwipeRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTaskManagementScreen(
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val unassignedTasks by viewModel.manageUnassignedTasks.collectAsState()
    val assignedTasks by viewModel.manageAssignedTasks.collectAsState()
    val isLoading by viewModel.isManageTasksLoading.collectAsState()
    val context = LocalContext.current

    var isSwipeRefreshing by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }

    // Fetch data when screen opens
    LaunchedEffect(Unit) {
        viewModel.fetchManageableTasks()
    }

    // Stop swipe spinner when loading finishes
    LaunchedEffect(isLoading) {
        if (!isLoading) isSwipeRefreshing = false
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (isLoading) {
                        Toast.makeText(context, "Please wait...", Toast.LENGTH_SHORT).show()
                    } else {
                        onBack()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Manage & Delete Tasks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // --- REFRESHABLE LIST ---
            SwipeRefreshBox(
                isRefreshing = isSwipeRefreshing,
                onRefresh = {
                    isSwipeRefreshing = true
                    viewModel.fetchManageableTasks()
                },
                modifier = Modifier.weight(1f).clipToBounds()
            ) {
                if (unassignedTasks.isEmpty() && assignedTasks.isEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No active or unassigned tasks.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // --- UNASSIGNED SECTION ---
                        if (unassignedTasks.isNotEmpty()) {
                            item {
                                Text("Unassigned Tasks", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            items(unassignedTasks, key = { it.id }) { task ->
                                ManageTaskCard(task = task, onDeleteClick = { taskToDelete = task })
                            }
                            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                        }

                        // --- ACTIVE ROSTER SECTION ---
                        if (assignedTasks.isNotEmpty()) {
                            item {
                                Text("Active Roster (Assigned)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            items(assignedTasks, key = { it.id }) { task ->
                                ManageTaskCard(task = task, onDeleteClick = { taskToDelete = task })
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DELETE CONFIRMATION DIALOG ---
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { if (!isLoading) taskToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Task?") },
            text = {
                Column {
                    Text("Are you sure you want to permanently delete this task? This cannot be undone.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(task.companyName, fontWeight = FontWeight.Bold)
                    if (task.employeeName.isNotBlank()) {
                        Text("Assigned to: ${task.employeeName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteManageableTask(
                            taskId = task.id,
                            onSuccess = {
                                Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show()
                                taskToDelete = null
                            },
                            onError = { Toast.makeText(context, "Failed to delete task", Toast.LENGTH_SHORT).show() }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onError)
                    else Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }, enabled = !isLoading) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ManageTaskCard(task: Task, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(task.companyName, fontWeight = FontWeight.Bold)
                if (task.employeeName.isNotBlank()) {
                    Text("Tech: ${task.employeeName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                if (task.workToBeDone.isNotBlank()) {
                    Text(task.workToBeDone, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Task", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}