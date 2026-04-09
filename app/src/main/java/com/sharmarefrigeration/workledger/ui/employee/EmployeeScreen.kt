package com.sharmarefrigeration.workledger.ui.employee

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sharmarefrigeration.workledger.model.Task
import com.sharmarefrigeration.workledger.model.TaskType
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeScreen(
    viewModel: EmployeeViewModel,
    onLogNewWorkClick: () -> Unit
) {
    val assignedTasks by viewModel.assignedTasks.collectAsState()
    val submittedTasks by viewModel.submittedTasks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // State to handle viewing a submitted task details
    var taskToView by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onLogNewWorkClick) {
                Icon(Icons.Default.Add, contentDescription = "Log Ad-Hoc Work")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading && assignedTasks.isEmpty() && submittedTasks.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text("Assigned Tasks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }

                        if (assignedTasks.isEmpty()) {
                            item {
                                Text("No pending tasks.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(assignedTasks) { task ->
                                AssignedTaskCard(task)
                            }
                        }

                        item { Spacer(modifier = Modifier.height(8.dp)) }

                        item {
                            Text("Recently Submitted", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }

                        if (submittedTasks.isEmpty()) {
                            item {
                                Text("No recent submissions.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(submittedTasks) { task ->
                                SubmittedTaskCard(task = task, onClick = { taskToView = task })
                            }
                        }
                    }
                }
            }
        }

    taskToView?.let { task ->
        ViewSubmittedTaskDialog(
            task = task,
            onDismiss = { taskToView = null }
        )
    }
}

@Composable
fun AssignedTaskCard(task: Task) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(task.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
        }
    }
}

@Composable
fun SubmittedTaskCard(task: Task, onClick: () -> Unit) {
    val timeString = task.completedAt?.let {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(it)
    } ?: "Unknown time"

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                Text("Submitted at $timeString", style = MaterialTheme.typography.bodySmall)

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (task.type == TaskType.BILL) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = if (task.type == TaskType.BILL) "BILLABLE" else "AMC",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ViewSubmittedTaskDialog(task: Task, onDismiss: () -> Unit) {
    var showImage by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Job Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Title: ${task.title}", fontWeight = FontWeight.Bold)
                Text("Notes: ${task.description}")

                if (task.jobCardImageUrl != null) {
                    if (showImage) {
                        AsyncImage(
                            model = task.jobCardImageUrl,
                            contentDescription = "Job Card",
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        OutlinedButton(onClick = { showImage = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Load Job Card Image")
                        }
                    }
                } else {
                    Text("No image was attached to this job.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}