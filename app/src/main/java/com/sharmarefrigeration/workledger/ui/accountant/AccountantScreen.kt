package com.sharmarefrigeration.workledger.ui.accountant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sharmarefrigeration.workledger.model.Invoice
import com.sharmarefrigeration.workledger.model.Task
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountantScreen(
    viewModel: AccountantViewModel,
) {
    val pendingTasks by viewModel.pendingTasks.collectAsState()
    val recentInvoices by viewModel.recentInvoices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var taskToProcess by remember { mutableStateOf<Task?>(null) }

    Scaffold() { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading && pendingTasks.isEmpty() && recentInvoices.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // --- SECTION 1: PENDING BILLS ---
                        item {
                            Text("Action Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }

                        if (pendingTasks.isEmpty()) {
                            item {
                                Text("No pending bills. You are caught up!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(pendingTasks) { task ->
                                AccountantTaskCard(task = task, onClick = { taskToProcess = task })
                            }
                        }

                        item { Spacer(modifier = Modifier.height(8.dp)) }

                        // --- SECTION 2: RECENTLY PROCESSED ---
                        item {
                            Text("Recently Processed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }

                        if (recentInvoices.isEmpty()) {
                            item {
                                Text("No recent invoices.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(recentInvoices) { invoice ->
                                RecentInvoiceCard(invoice = invoice)
                            }
                        }
                    }
                }
            }
        }

    taskToProcess?.let { task ->
        ProcessBillDialog(
            task = task,
            isSubmitting = isLoading,
            onDismiss = { taskToProcess = null },
            onSubmit = { amount, notes ->
                viewModel.requestAdminApproval(
                    task = task,
                    amount = amount,
                    notes = notes,
                    onSuccess = { taskToProcess = null },
                    onError = { /* Show error toast */ }
                )
            }
        )
    }
}

@Composable
fun AccountantTaskCard(task: Task, onClick: () -> Unit) {
    val dateString = task.completedAt?.let {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(it)
    } ?: "Unknown time"

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "Completed: $dateString", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Tap to calculate final bill", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun RecentInvoiceCard(invoice: Invoice) {
    val dateString = invoice.createdAt?.let {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(it)
    } ?: "Unknown time"

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Amount: ₹${invoice.amount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "Sent: $dateString", style = MaterialTheme.typography.bodySmall)

                if (invoice.notes.isNotBlank()) {
                    Text(text = "Note: ${invoice.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Status Badge
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (invoice.isApproved) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Approved", tint = MaterialTheme.colorScheme.primary)
                    Text("Approved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(Icons.Default.AccessTime, contentDescription = "Pending Admin", tint = MaterialTheme.colorScheme.tertiary)
                    Text("Pending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}

@Composable
fun ProcessBillDialog(
    task: Task,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Double, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // YOUR RULE: Do not load image automatically
    var showImage by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Process Invoice") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Job: ${task.title}")
                Text("Details: ${task.description}", style = MaterialTheme.typography.bodySmall)

                // The On-Demand Image Loader
                if (task.jobCardImageUrl != null) {
                    if (showImage) {
                        // Coil automatically fetches and caches the image
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
                    Text("No image uploaded by technician.", color = MaterialTheme.colorScheme.error)
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Final Bill Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes for Admin (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    onSubmit(amount, notes)
                },
                enabled = amountText.isNotBlank() && !isSubmitting
            ) {
                Text("Send for Approval")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}