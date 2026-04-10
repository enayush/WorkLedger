package com.sharmarefrigeration.workledger.ui.employee

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import android.widget.Toast
import com.sharmarefrigeration.workledger.ui.components.SubmittedTaskCard
import com.sharmarefrigeration.workledger.model.Task
import com.sharmarefrigeration.workledger.ui.components.SwipeRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkLedgerScreen(
    viewModel: EmployeeViewModel,
    targetUserId: String? = null,
    onBack: () -> Unit
) {
    val historyTasks by viewModel.historyTasks.collectAsState()
    val isLoading by viewModel.isHistoryLoading.collectAsState()
    val isLastHistoryPage by viewModel.isLastHistoryPage.collectAsState()
    val context = LocalContext.current

    var isSwipeRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (!isLoading) {
            isSwipeRefreshing = false
        }
    }

    // Fetch the first page when the screen opens
    LaunchedEffect(targetUserId) {
        viewModel.loadHistoryPage(isRefresh = true, targetUserId = targetUserId)
    }

    var taskToView by remember { mutableStateOf<Task?>(null) }

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
                            "Job History",
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
                modifier = Modifier.weight(1f)
            ) {
                if (historyTasks.isEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No past jobs found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (historyTasks.isEmpty() && !isSwipeRefreshing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
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
                            if (index == historyTasks.lastIndex && !isLoading && !isLastHistoryPage) {
                                LaunchedEffect(key1 = index) {
                                    viewModel.loadHistoryPage(targetUserId = targetUserId)
                                }
                            }
                        }

                        // Show a little spinner at the bottom while the next 10 are loading
                        if (isLoading && historyTasks.isNotEmpty() && !isSwipeRefreshing) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }

                        // Optional: Show text when they hit the very bottom
                        if (isLastHistoryPage && historyTasks.isNotEmpty()) {
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
    }

    taskToView?.let { task ->
        ViewSubmittedTaskDialog(task = task, onDismiss = { taskToView = null })
    }
}