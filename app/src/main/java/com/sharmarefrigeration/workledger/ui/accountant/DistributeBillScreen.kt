package com.sharmarefrigeration.workledger.ui.accountant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sharmarefrigeration.workledger.model.Invoice
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DistributeBillScreen(viewModel: AccountantViewModel) {
    val invoices by viewModel.invoicesToDistribute.collectAsState()
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    var showDialog by remember { mutableStateOf<Invoice?>(null) }
    var distributedToPerson by remember { mutableStateOf("") }
    var distributedAddress by remember { mutableStateOf("") }

    if (showDialog != null) {
        val invoice = showDialog!!
        AlertDialog(
            onDismissRequest = { showDialog = null },
            title = { Text("Distribute Bill") },
            text = {
                Column {
                    Text("Record who received the bill and where.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = distributedToPerson,
                        onValueChange = { distributedToPerson = it },
                        label = { Text("Delivered To (Person)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = distributedAddress,
                        onValueChange = { distributedAddress = it },
                        label = { Text("Delivery Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markAsDistributed(
                            invoiceId = invoice.id,
                            personName = distributedToPerson,
                            address = distributedAddress,
                            onSuccess = { showDialog = null }
                        )
                    },
                    enabled = distributedToPerson.isNotBlank() && distributedAddress.isNotBlank()
                ) {
                    Text("Approve")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Ready to Send", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (invoices.isEmpty()) {
            item { Text("No bills waiting to be sent.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(invoices, key = { it.id }) { invoice ->
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Icon(
                                        Icons.Outlined.Receipt,
                                        contentDescription = null,
                                        modifier = Modifier.padding(6.dp).size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = invoice.jobCardId?.let { "Job Card #$it" } ?: "Invoice #${invoice.id.takeLast(6).uppercase()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (invoice.companyName.isNotBlank()) {
                                        Text(
                                            text = invoice.companyName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (invoice.employeeName.isNotBlank()) {
                                        Text(
                                            text = "Worker: ${invoice.employeeName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (invoice.workDone.isNotBlank()) {
                                        Text(
                                            text = invoice.workDone,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                        Icon(Icons.Outlined.Event, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = dateFormatter.format(invoice.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "₹${invoice.amount}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (invoice.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Internal Notes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = invoice.notes,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                distributedToPerson = ""
                                distributedAddress = ""
                                showDialog = invoice
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mark as Sent to Client")
                        }
                    }
                }
            }
        }
    }
}