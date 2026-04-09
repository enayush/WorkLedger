package com.sharmarefrigeration.workledger.ui.employee

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sharmarefrigeration.workledger.model.Task
import com.sharmarefrigeration.workledger.ui.components.SwipeRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkLedgerScreen(
    viewModel: EmployeeViewModel,
    onBack: () -> Unit
) {
    val historyTasks by viewModel.historyTasks.collectAsState()
    val isLoading by viewModel.isHistoryLoading.collectAsState()

    // Fetch the first page when the screen opens
    LaunchedEffect(Unit) {
        if (historyTasks.isEmpty()) {
            viewModel.loadHistoryPage(isRefresh = true)
        }
    }

    var taskToView by remember { mutableStateOf<Task?>(null) }

    Scaffold(
    ) { paddingValues ->
        SwipeRefreshBox(
            isRefreshing = isLoading && historyTasks.isEmpty(), // Only show top spinner on full refresh
            onRefresh = { viewModel.loadHistoryPage(isRefresh = true) },
            modifier = Modifier.padding(paddingValues)
        ) {
            if (historyTasks.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No past jobs found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(historyTasks) { index, task ->
                        // Reuse the card we built for the dashboard!
                        SubmittedTaskCard(task = task, onClick = { taskToView = task })

                        // --- THE INFINITE SCROLL TRIGGER ---
                        // If we scrolled to the last item, tell the ViewModel to get 10 more
                        if (index == historyTasks.lastIndex && !isLoading && !viewModel.isLastHistoryPage) {
                            LaunchedEffect(key1 = index) {
                                viewModel.loadHistoryPage()
                            }
                        }
                    }

                    // Show a little spinner at the bottom while the next 10 are loading
                    if (isLoading && historyTasks.isNotEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    // Optional: Show text when they hit the very bottom
                    if (viewModel.isLastHistoryPage && historyTasks.isNotEmpty()) {
                        item {
                            Text(
                                "End of history",
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }

    taskToView?.let { task ->
        ViewSubmittedTaskDialog(task = task, onDismiss = { taskToView = null })
    }
}