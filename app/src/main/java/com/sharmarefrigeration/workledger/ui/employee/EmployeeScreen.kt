package com.sharmarefrigeration.workledger.ui.employee

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sharmarefrigeration.workledger.model.Task
import com.sharmarefrigeration.workledger.model.TaskType
import com.sharmarefrigeration.workledger.ui.components.SubmittedTaskCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeScreen(
    viewModel: EmployeeViewModel,
    onLogNewWorkClick: (String?) -> Unit
) {
    val assignedTasks by viewModel.assignedTasks.collectAsState()
    val submittedTasks by viewModel.submittedTasks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // State to handle viewing a submitted task details
    var taskToView by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick ={ onLogNewWorkClick(null) },
                containerColor = MaterialTheme.colorScheme.primary
            ){
                Icon(Icons.Default.Add, contentDescription = "Log Ad-Hoc Work")
            }
        }
    ) { paddingValues ->
        // Replaced SwipeRefreshBox with a simple Box
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (assignedTasks.isEmpty() && submittedTasks.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tasks found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (assignedTasks.isEmpty() && submittedTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // --- ASSIGNED TASKS ---
                    item {
                        Text("Assigned Tasks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }

                    if (assignedTasks.isEmpty()) {
                        item {
                            Text("No pending tasks.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(assignedTasks, key = { it.id }) { task ->
                            AssignedTaskCard(task = task, onCompleteClick = { onLogNewWorkClick(task.id) })
                        }
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // --- RECENTLY SUBMITTED ---
                    item {
                        Text("Recently Submitted", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }

                    if (submittedTasks.isEmpty()) {
                        item {
                            Text("No recent submissions.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(submittedTasks, key = { it.id }) { task ->
                            SubmittedTaskCard(task = task, onClick = { taskToView = task })
                        }
                    }
                }
            }
        }
    }

    // --- POPUP DIALOG ---
    taskToView?.let { task ->
        ViewSubmittedTaskDialog(
            task = task,
            onDismiss = { taskToView = null }
        )
    }
}

@Composable
fun AssignedTaskCard(task: Task, onCompleteClick: () -> Unit) {
    Card(
        onClick = onCompleteClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Company Name & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.companyName.ifEmpty { "Pending Assignment" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "PENDING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body Row: Address with Icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = task.companyAddress.ifEmpty { "Address TBD" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Admin Instructions
            if (task.workToBeDone.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Admin Instructions:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(task.workToBeDone, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Footer Row: Action Instruction
            Text(
                text = "Tap to log work and complete",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}


@Composable
fun ViewSubmittedTaskDialog(task: Task, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Job Details", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                DetailRow(label = "Company / Client", value = task.companyName)
                DetailRow(label = "Location", value = task.companyAddress)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailRow(label = "Date", value = task.localDateString)
                    DetailRow(label = "Type", value = if (task.type == TaskType.BILL) "Billable" else "AMC (Free)")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Work Performed", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(task.workDone, style = MaterialTheme.typography.bodyMedium)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Job Card / Reference", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                if (task.jobCardId.isNullOrBlank()) {
                    Text("No Job Card issued", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                } else {
                    Text(task.jobCardId, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}