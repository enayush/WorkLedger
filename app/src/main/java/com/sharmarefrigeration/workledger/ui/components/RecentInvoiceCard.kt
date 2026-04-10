package com.sharmarefrigeration.workledger.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sharmarefrigeration.workledger.model.Invoice
import com.sharmarefrigeration.workledger.model.InvoiceStatus
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun RecentInvoiceCard(invoice: Invoice) {
    val dateString = invoice.createdAt.let {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it)
    }

    // --- DYNAMIC STATUS BADGE ---
    val statusText = when(invoice.status) {
        InvoiceStatus.CREATED -> "Created"
        InvoiceStatus.DISTRIBUTED -> "Sent"
        InvoiceStatus.PAYMENT_RECEIVED -> "Paid"
        InvoiceStatus.APPROVED -> "Closed"
    }

    val statusColor = when(invoice.status) {
        InvoiceStatus.APPROVED -> MaterialTheme.colorScheme.primary
        InvoiceStatus.PAYMENT_RECEIVED -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }

    val statusIcon = when(invoice.status) {
        InvoiceStatus.APPROVED -> Icons.Default.CheckCircle
        InvoiceStatus.PAYMENT_RECEIVED -> Icons.Default.Payments
        InvoiceStatus.DISTRIBUTED -> Icons.AutoMirrored.Filled.Send
        InvoiceStatus.CREATED -> Icons.Default.Receipt
    }

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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = statusColor.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            statusIcon,
                            contentDescription = statusText,
                            modifier = Modifier.padding(6.dp).size(20.dp),
                            tint = statusColor
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.padding(end = 8.dp)) {
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
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Icon(Icons.Outlined.Event, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = dateString,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

