package com.sharmarefrigeration.workledger.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sharmarefrigeration.workledger.model.Invoice
import com.sharmarefrigeration.workledger.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: AdminViewModel,
) {
    val invoices by viewModel.pendingInvoices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold() { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading && invoices.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (invoices.isEmpty()) {
                    Text(
                        text = "No pending approvals. All caught up!",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text("Requires Approval", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(invoices) { invoice ->
                            InvoiceApprovalCard(
                                invoice = invoice,
                                isSubmitting = isLoading,
                                onApprove = {
                                    viewModel.approveInvoice(
                                        invoice = invoice,
                                        onSuccess = { /* List auto-updates */ },
                                        onError = { /* Show error toast */ }
                                    )
                                }
                            )
                        }
                    }
                }
        }
    }
}

@Composable
fun InvoiceApprovalCard(invoice: Invoice, isSubmitting: Boolean, onApprove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Amount: ₹${invoice.amount}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            if (invoice.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Accountant Notes: ${invoice.notes}", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onApprove,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Approve & Close Task")
            }
        }
    }
}

@Composable
fun AddUserDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, UserRole) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.EMPLOYEE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register New Staff") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (10 digits)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    prefix = { Text("+91 ") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Assign Role", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedRole == UserRole.EMPLOYEE, onClick = { selectedRole = UserRole.EMPLOYEE })
                        Text("Field Technician")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedRole == UserRole.ACCOUNTANT, onClick = { selectedRole = UserRole.ACCOUNTANT })
                        Text("Accountant")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(name, phone, selectedRole) },
                enabled = name.isNotBlank() && phone.length == 10 && !isSubmitting
            ) {
                Text("Register")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}