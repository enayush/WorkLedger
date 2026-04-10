package com.sharmarefrigeration.workledger.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Locale
import com.sharmarefrigeration.workledger.ui.components.RecentInvoiceCard
import com.sharmarefrigeration.workledger.ui.components.SubmittedTaskCard

// Simple state machine for the UI
enum class ReportMode { TASKS, INVOICES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(viewModel: AdminViewModel, onBack: () -> Unit) {
    val reportTasks by viewModel.reportTasks.collectAsStateWithLifecycle()
    val reportInvoices by viewModel.reportInvoices.collectAsStateWithLifecycle()
    val isLoading by viewModel.isReportLoading.collectAsStateWithLifecycle()

    var currentMode by remember { mutableStateOf(ReportMode.TASKS) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Material 3 built-in range picker
    val dateRangePickerState = rememberDateRangePickerState()
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    fun fetchReports() {
        val start = dateRangePickerState.selectedStartDateMillis
        val end = dateRangePickerState.selectedEndDateMillis
        if (start != null && end != null) {
            if (currentMode == ReportMode.TASKS) {
                viewModel.searchTasksByDateRange(start, end)
            } else {
                viewModel.searchInvoicesByDateRange(start, end)
            }
        }
    }

    LaunchedEffect(currentMode) {
        fetchReports()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks & Invoices") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Tab row to choose between Tasks and Invoices
            TabRow(selectedTabIndex = currentMode.ordinal) {
                Tab(
                    selected = currentMode == ReportMode.TASKS,
                    onClick = { currentMode = ReportMode.TASKS },
                    text = { Text("Tasks") },
                    icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null) }
                )
                Tab(
                    selected = currentMode == ReportMode.INVOICES,
                    onClick = { currentMode = ReportMode.INVOICES },
                    text = { Text("Invoices") },
                    icon = { Icon(Icons.Default.Receipt, contentDescription = null) }
                )
            }

            // Date Range Selector
            Surface(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Select Date Range",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Date Range",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            val start = dateRangePickerState.selectedStartDateMillis
                            val end = dateRangePickerState.selectedEndDateMillis
                            if (start != null && end != null) {
                                val startDateStr = dateFormatter.format(java.util.Date(start))
                                val endDateStr = dateFormatter.format(java.util.Date(end))
                                Text(
                                    text = "$startDateStr - $endDateStr",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            } else {
                                Text(
                                    text = "Select dates...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    TextButton(onClick = { showDatePicker = true }) {
                        Text("Change")
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (currentMode == ReportMode.TASKS) {
                        if (reportTasks.isEmpty()) {
                            item {
                                Text(
                                    "No tasks found in this date range.",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        items(reportTasks, key = { it.id }) { task ->
                            SubmittedTaskCard(task = task, onClick = {})
                        }
                    } else {
                        if (reportInvoices.isEmpty()) {
                            item {
                                Text(
                                    "No invoices found in this date range.",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        items(reportInvoices, key = { it.id }) { invoice ->
                            RecentInvoiceCard(invoice = invoice)
                        }
                    }
                }
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDatePicker = false
                            fetchReports()
                        },
                        enabled = dateRangePickerState.selectedStartDateMillis != null &&
                                  dateRangePickerState.selectedEndDateMillis != null
                    ) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DateRangePicker(
                    state = dateRangePickerState,
                    title = {
                        Text(
                            text = "Select Date Range",
                            modifier = Modifier.padding(16.dp)
                        )
                    },
                    headline = null,
                    showModeToggle = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}