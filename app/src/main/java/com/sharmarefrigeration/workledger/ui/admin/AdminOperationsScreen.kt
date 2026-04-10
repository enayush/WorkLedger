package com.sharmarefrigeration.workledger.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharmarefrigeration.workledger.model.Task
import com.sharmarefrigeration.workledger.model.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOperationsScreen(viewModel: AdminViewModel) {
    val unassignedTasks by viewModel.unassignedTasks.collectAsStateWithLifecycle()
    val assignedTasks by viewModel.assignedTasks.collectAsStateWithLifecycle()
    val employees by viewModel.technicians.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var taskToAssign by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateTaskDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Task")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            // --- UNASSIGNED POOL ---
            if (unassignedTasks.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, tint = MaterialTheme.colorScheme.error, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unassigned Tasks (${unassignedTasks.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
                items(unassignedTasks, key = { it.id }) { task ->
                    UnassignedTaskCard(task = task, onClick = { taskToAssign = task })
                }
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            }

            // --- TODAY'S ACTIVE ROSTER ---
            item {
                Text("Active Roster", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (assignedTasks.isEmpty()) {
                item { Text("No technicians currently working on assigned tasks.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(assignedTasks, key = { it.id }) { task ->
                    AdminAssignedTaskCard(task = task)
                }
            }
        }
    }

    // --- QUICK ASSIGN DIALOG ---
    taskToAssign?.let { task ->
        var selectedTech by remember { mutableStateOf<User?>(null) }
        var isAssigning by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isAssigning) taskToAssign = null },
            title = { Text("Assign Task") },
            text = {
                Column {
                    Text(task.companyName, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    EmployeeDropdown(employees = employees, selectedUser = selectedTech, onUserSelected = { selectedTech = it })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isAssigning) return@Button
                        selectedTech?.let { tech ->
                            isAssigning = true
                            viewModel.quickAssignTask(task, tech)
                            taskToAssign = null
                        }
                    },
                    enabled = selectedTech != null && !isAssigning
                ) {
                    if (isAssigning) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Assign")
                    }
                }
            },
            dismissButton = { TextButton(onClick = { taskToAssign = null }, enabled = !isAssigning) { Text("Cancel") } }
        )
    }

    // --- FULL CREATE TASK DIALOG ---
    if (showCreateTaskDialog) {
        var companyName by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var workNeeded by remember { mutableStateOf("") }
        var selectedTech by remember { mutableStateOf<User?>(null) }
        val dateString = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }

        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showCreateTaskDialog = false },
            title = { Text("Create New Work Order") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Company Name") }, singleLine = true)
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address/Location") }, maxLines = 2)
                    OutlinedTextField(value = workNeeded, onValueChange = { workNeeded = it }, label = { Text("Work Required (Optional)") })

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Assign to (Leave blank to pool):", style = MaterialTheme.typography.labelMedium)
                    EmployeeDropdown(employees = employees, selectedUser = selectedTech, onUserSelected = { selectedTech = it })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isSubmitting) return@Button
                        isSubmitting = true
                        viewModel.createNewTask(companyName, address, workNeeded, dateString, selectedTech) {
                            showCreateTaskDialog = false
                        }
                    },
                    enabled = companyName.isNotBlank() && !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Create Task")
                    }
                }
            },
            dismissButton = { TextButton(onClick = { showCreateTaskDialog = false }, enabled = !isSubmitting) { Text("Cancel") } }
        )
    }
}

// --- REUSABLE DROPDOWN COMPONENT ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDropdown(employees: List<User>, selectedUser: User?, onUserSelected: (User?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedUser?.name ?: "Leave Unassigned",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Leave Unassigned", color = MaterialTheme.colorScheme.error) },
                onClick = { onUserSelected(null); expanded = false }
            )
            employees.forEach { tech ->
                DropdownMenuItem(
                    text = { Text(tech.name) },
                    onClick = { onUserSelected(tech); expanded = false }
                )
            }
        }
    }
}

@Composable
fun UnassignedTaskCard(task: Task, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        text = "UNASSIGNED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap to assign to technician",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun AdminAssignedTaskCard(task: Task) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.companyName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = task.employeeName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = task.companyAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = task.workToBeDone.ifEmpty { "work not mentioned" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
