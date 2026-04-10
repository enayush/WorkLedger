package com.sharmarefrigeration.workledger.ui.accountant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sharmarefrigeration.workledger.model.Invoice
import com.sharmarefrigeration.workledger.model.PaymentMethod
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectPaymentScreen(viewModel: AccountantViewModel) {
    val invoices by viewModel.invoicesToCollect.collectAsState()
    var invoiceToPay by remember { mutableStateOf<Invoice?>(null) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Awaiting Payment", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }

        if (invoices.isEmpty()) {
            item { Text("No payments pending collection.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(invoices, key = { it.id }) { invoice ->
                Card(
                    onClick = { invoiceToPay = invoice },
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
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Icon(
                                        Icons.Outlined.Receipt,
                                        contentDescription = null,
                                        modifier = Modifier.padding(6.dp).size(20.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
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
                                    if (invoice.distributedToPerson.isNotBlank()) {
                                        Text(
                                            text = "Delivered to: ${invoice.distributedToPerson}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    val dateToUse = invoice.distributedAt ?: invoice.createdAt
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                        Icon(Icons.Outlined.Event, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = dateFormatter.format(dateToUse),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "₹${invoice.amount}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                                        Text("Bill Notes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tap to record payment", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    invoiceToPay?.let { invoice ->
        var method by remember { mutableStateOf(PaymentMethod.CASH) }
        var refNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { invoiceToPay = null },
            title = { Text("Confirm Payment", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val idLabel = invoice.jobCardId?.let { "Job Card #$it" } ?: "Invoice #${invoice.id.takeLast(6).uppercase()}"
                            Text(idLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                            if (invoice.companyName.isNotBlank()) {
                                Text(invoice.companyName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text("Amount to Collect", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text("₹${invoice.amount}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Text("Select Payment Method", style = MaterialTheme.typography.labelMedium)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PaymentMethod.entries.filter { it != PaymentMethod.PENDING }.forEach { m ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = method == m,
                                    onClick = { method = m }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(m.name, style = MaterialTheme.typography.bodyMedium)
                            }
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
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmPayment(invoice.id, method, refNote) { invoiceToPay = null } },
                    shape = MaterialTheme.shapes.medium
                ) { Text("Confirm Payment") }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToPay = null }) { Text("Cancel") }
            }
        )
    }
}