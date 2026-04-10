package com.sharmarefrigeration.workledger.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sharmarefrigeration.workledger.model.Invoice
import com.sharmarefrigeration.workledger.model.InvoiceStatus
import com.sharmarefrigeration.workledger.model.PaymentMethod

@Composable
fun AdminApprovalsScreen(viewModel: AdminViewModel) {
    val pendingInvoices by viewModel.pendingInvoices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    var selectedInvoice by remember { mutableStateOf<Invoice?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Revenue Pipeline", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (pendingInvoices.isEmpty() && !isLoading) {
            item { Text("No active bills in the pipeline.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(pendingInvoices, key = { it.id }) { invoice ->
                InvoiceApprovalCard(
                    invoice = invoice,
                    onClick = { selectedInvoice = invoice }
                )
            }
        }
    }

    // --- THE UNIFIED ADMIN INVOICE DIALOG ---
    selectedInvoice?.let { invoice ->
        var method by remember { mutableStateOf(PaymentMethod.CASH) }
        var refNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { selectedInvoice = null },
            title = { Text("Invoice Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Client", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(invoice.companyName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Amount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text("₹${invoice.amount}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Service Tracking", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text("Tech: ${invoice.employeeName}", style = MaterialTheme.typography.bodyMedium)

                            if (invoice.status == InvoiceStatus.PAYMENT_RECEIVED) {
                                Text("Collected via: ${invoice.paymentMethod.name}", style = MaterialTheme.typography.bodyMedium)
                                if (invoice.paymentNotes.isNotBlank()) {
                                    Text("Ref: ${invoice.paymentNotes}", style = MaterialTheme.typography.bodyMedium)
                                }
                            } else if (invoice.status == InvoiceStatus.DISTRIBUTED) {
                                Text("Sent to: ${invoice.distributedToPerson}", style = MaterialTheme.typography.bodyMedium)
                                Text("At: ${invoice.distributedAddress}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (invoice.status == InvoiceStatus.DISTRIBUTED) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Bypass Collection (Collect Directly)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Bypass the accountant and record this payment directly.", style = MaterialTheme.typography.bodySmall)

                        Text("Select Payment Method:")
                        PaymentMethod.values().filter { it != PaymentMethod.PENDING }.forEach { m ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                RadioButton(selected = method == m, onClick = { method = m })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(m.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        OutlinedTextField(
                            value = refNote,
                            onValueChange = { refNote = it },
                            label = { Text("Cheque # / UTR / Cash Note") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                if (invoice.status == InvoiceStatus.PAYMENT_RECEIVED) {
                    Button(
                        onClick = {
                            viewModel.approveInvoice(
                                invoice = invoice,
                                onSuccess = {
                                    selectedInvoice = null
                                    Toast.makeText(context, "Payment Verified & Job Closed", Toast.LENGTH_SHORT).show()
                                },
                                onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                            )
                        },
                        enabled = !isLoading
                    ) { Text("Verify & Close Job") }
                } else if (invoice.status == InvoiceStatus.DISTRIBUTED) {
                    Button(
                        onClick = {
                            viewModel.forceApproveDistributedInvoice(
                                invoice = invoice,
                                method = method,
                                notes = refNote,
                                onSuccess = {
                                    selectedInvoice = null
                                    Toast.makeText(context, "Bypass Successful. Job Closed.", Toast.LENGTH_SHORT).show()
                                },
                                onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                            )
                        },
                        enabled = !isLoading
                    ) { Text("Collect Now & Close") }
                }
            },
            dismissButton = { TextButton(onClick = { selectedInvoice = null }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceApprovalCard(
    invoice: Invoice,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(invoice.companyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Tech: ${invoice.employeeName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "₹${invoice.amount}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- STATE 1: PAYMENT RECEIVED (Ready to Verify) ---
            if (invoice.status == InvoiceStatus.PAYMENT_RECEIVED) {
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha=0.5f), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Collected (${invoice.paymentMethod.name})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            if (invoice.paymentNotes.isNotBlank()) {
                                Text("Ref: ${invoice.paymentNotes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // --- STATE 2: DISTRIBUTED (Ready to Bypass) ---
            else if (invoice.status == InvoiceStatus.DISTRIBUTED) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.5f), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Pending Collection", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("Sent to: ${invoice.distributedToPerson}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            val tapActionText = if (invoice.status == InvoiceStatus.PAYMENT_RECEIVED) {
                "Tap to verify and close"
            } else {
                "Tap to collect directly"
            }

            Text(
                text = tapActionText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}