package com.sharmarefrigeration.workledger.ui.accountant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sharmarefrigeration.workledger.ui.components.SwipeRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingArchiveScreen(
    viewModel: AccountantViewModel,
    onBack: () -> Unit
) {
    val historyInvoices by viewModel.historyInvoices.collectAsState()
    val isLoading by viewModel.isHistoryLoading.collectAsState()

    LaunchedEffect(Unit) {
        if (historyInvoices.isEmpty()) {
            viewModel.loadHistoryPage(isRefresh = true)
        }
    }

    Scaffold(
    ) { paddingValues ->
        SwipeRefreshBox(
            isRefreshing = isLoading && historyInvoices.isEmpty(),
            onRefresh = { viewModel.loadHistoryPage(isRefresh = true) },
            modifier = Modifier.padding(paddingValues)
        ) {
            if (historyInvoices.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No past invoices found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                viewModel.loadHistoryPage()
                            }
                        }
                    }

                    if (isLoading && historyInvoices.isNotEmpty()) {
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