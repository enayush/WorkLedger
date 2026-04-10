package com.sharmarefrigeration.workledger.ui.accountant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.sharmarefrigeration.workledger.model.Invoice
import com.sharmarefrigeration.workledger.model.InvoiceStatus
import com.sharmarefrigeration.workledger.ui.components.RecentInvoiceCard
import com.sharmarefrigeration.workledger.ui.components.SwipeRefreshBox
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingArchiveScreen(
    viewModel: AccountantViewModel,
    targetUserId: String? = null,
    onBack: () -> Unit
) {
    val historyInvoices by viewModel.historyInvoices.collectAsState()
    val isLoading by viewModel.isHistoryLoading.collectAsState()
    val context = LocalContext.current

    var isSwipeRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (!isLoading) {
            isSwipeRefreshing = false
        }
    }

    LaunchedEffect(targetUserId) {
        if (historyInvoices.isEmpty()) {
            viewModel.loadHistoryPage(isRefresh = true, targetUserId = targetUserId)
        }
    }

    Scaffold(
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (isLoading) {
                        Toast.makeText(context, "Please wait...", Toast.LENGTH_SHORT).show()
                    } else {
                        onBack()
                    }
                }) {
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            SwipeRefreshBox(
                isRefreshing = isSwipeRefreshing,
                onRefresh = {
                    isSwipeRefreshing = true
                    viewModel.loadHistoryPage(isRefresh = true, targetUserId = targetUserId)
                },
                modifier = Modifier
                    .weight(1f)
                    .clipToBounds()
            ) {
                if (historyInvoices.isEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No past invoices found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (historyInvoices.isEmpty() && !isSwipeRefreshing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(historyInvoices) { index, invoice ->

                            // We reuse your awesome Invoice card!
                            RecentInvoiceCard(invoice = invoice)

                            // --- THE INFINITE SCROLL TRIGGER ---
                            if (index == historyInvoices.lastIndex && !isLoading && !viewModel.isLastHistoryPage) {
                                LaunchedEffect(key1 = index) {
                                    viewModel.loadHistoryPage(targetUserId = targetUserId)
                                }
                            }
                        }

                        if (isLoading && historyInvoices.isNotEmpty() && !isSwipeRefreshing) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }

                        if (viewModel.isLastHistoryPage && historyInvoices.isNotEmpty()) {
                            item {
                                Text("End of history", modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
