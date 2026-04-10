package com.sharmarefrigeration.workledger.ui.employee

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sharmarefrigeration.workledger.model.TaskType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.ui.draw.scale

@Composable
fun LogWorkScreen(
    viewModel: EmployeeViewModel,
    employeeName: String, // Passed from AppShell!
    taskIdToComplete: String? = null,
    onBack: () -> Unit,
    onTaskSaved: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Form State
    var companyName by remember { mutableStateOf("") }
    var companyAddress by remember { mutableStateOf("") }
    var workDone by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TaskType.AMC) }
    var workToBeDone by remember { mutableStateOf("") }
    // Job Card State
    var hasJobCard by remember { mutableStateOf(true) }
    var jobCardId by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(taskIdToComplete) {
        if (taskIdToComplete != null) {
            val task = viewModel.getAssignedTaskById(taskIdToComplete)
            if (task != null) {
                companyName = task.companyName
                companyAddress = task.companyAddress
                workToBeDone = task.workToBeDone
            }
        }
    }

    // Generate Local Date safely formatting the exact day on the device
    val localDateString = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
    }

    BackHandler(enabled = isLoading) {
        Toast.makeText(context, "Please wait, saving job details...", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        bottomBar = {
            val isFormValid = companyName.isNotBlank() && workDone.isNotBlank() && (!hasJobCard || jobCardId.isNotBlank())
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = {
                        if (isSubmitting || isSuccess) return@Button
                        isSubmitting = true
                        if (taskIdToComplete != null) {
                            // Flow A: Completing assigned task
                            viewModel.completeAssignedTask(
                                taskId = taskIdToComplete,
                                workDone = workDone,
                                type = selectedType,
                                jobCardId = if (hasJobCard) jobCardId else null,
                                onSuccess = {
                                    isSuccess = true
                                    onTaskSaved()
                                },
                                onError = {
                                    isSubmitting = false
                                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                                }
                            )
                        } else{
                        viewModel.saveAdHocTask(
                            companyName = companyName,
                            companyAddress = companyAddress,
                            workDone = workDone,
                            type = selectedType,
                            jobCardId = if (hasJobCard) jobCardId else null,
                            employeeName = employeeName,
                            localDateString = localDateString,
                            onSuccess = {
                                isSuccess = true
                                onTaskSaved()
                            },
                            onError = {
                                isSubmitting = false
                                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                            }
                        )
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = isFormValid && !isLoading && !isSubmitting && !isSuccess,
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isLoading || isSubmitting || isSuccess) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text(if (taskIdToComplete != null) "Complete Assigned Job" else "Submit Log")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (isLoading) {
                            Toast.makeText(context, "Please wait, saving job details...", Toast.LENGTH_SHORT).show()
                        } else {
                            onBack()
                        }
                    }
                ) {
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Technician : $employeeName", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(localDateString, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                    }
                }
            }

            // --- CLIENT DETAILS ---
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Client Details", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Company / Client Name", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = companyAddress,
                        onValueChange = { companyAddress = it },
                        label = { Text("Address / Location", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }

            // --- ADMIN INSTRUCTIONS ---
            if (workToBeDone.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Admin Instructions:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(workToBeDone, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // --- JOB DETAILS ---
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Work Details", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = workDone,
                        onValueChange = { workDone = it },
                        label = { Text("Describe Work Done & Parts Replaced", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        shape = MaterialTheme.shapes.medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedType == TaskType.AMC,
                                onClick = { selectedType = TaskType.AMC },
                                modifier = Modifier.size(36.dp)
                            )
                            Text("AMC (Free)", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedType == TaskType.BILL,
                                onClick = { selectedType = TaskType.BILL },
                                modifier = Modifier.size(36.dp)
                            )
                            Text("Billable", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // --- JOB CARD SECTION ---
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Job Card", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = hasJobCard,
                            onCheckedChange = {
                                hasJobCard = it
                                if (!hasJobCard) jobCardId = ""
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (hasJobCard) "Job Card Issued" else "No Job Card Issued", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (hasJobCard) {
                        OutlinedTextField(
                            value = jobCardId,
                            onValueChange = { jobCardId = it },
                            label = { Text("Enter Job Card ID", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}