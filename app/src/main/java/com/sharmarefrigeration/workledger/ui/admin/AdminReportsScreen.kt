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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharmarefrigeration.workledger.ui.components.ErrorDialog
import java.text.SimpleDateFormat
import java.util.Locale
import com.sharmarefrigeration.workledger.ui.components.RecentInvoiceCard
import com.sharmarefrigeration.workledger.ui.components.SubmittedTaskCard

// Simple state machine for the UI
enum class ReportMode { TASKS, INVOICES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(viewModel: AdminViewModel, onNavigateBack: () -> Unit) {
    val reportTasks by viewModel.reportTasks.collectAsStateWithLifecycle()
    val reportInvoices by viewModel.reportInvoices.collectAsStateWithLifecycle()
    val isLoading by viewModel.isReportLoading.collectAsStateWithLifecycle()
    val errorEvent by viewModel.errorEvent.collectAsStateWithLifecycle()

    var currentMode by remember { mutableStateOf(ReportMode.TASKS) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val startDateState = rememberDatePickerState()
    val endDateState = rememberDatePickerState()
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    fun fetchReports() {
        val start = startDateState.selectedDateMillis
        val end = endDateState.selectedDateMillis
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
                    IconButton(onClick = onNavigateBack) {
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

            // Date Range Selectors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // From Date
                DateSelectionCard(
                    title = "From",
                    dateMillis = startDateState.selectedDateMillis,
                    dateFormatter = dateFormatter,
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f)
                )

                // To Date
                DateSelectionCard(
                    title = "To",
                    dateMillis = endDateState.selectedDateMillis,
                    dateFormatter = dateFormatter,
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f)
                )
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

        if (showStartDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showStartDatePicker = false },
                modifier = Modifier.padding(16.dp), // Add margin to prevent edge-to-edge stretching on zoomed screens
                confirmButton = {
                    TextButton(
                        onClick = {
                            showStartDatePicker = false
                            fetchReports()
                        }
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(
                    state = startDateState,
                    showModeToggle = false // Hiding mode toggle saves some vertical space
                )
            }
        }

        if (showEndDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showEndDatePicker = false },
                modifier = Modifier.padding(16.dp), // Add margin to prevent edge-to-edge stretching on zoomed screens
                confirmButton = {
                    TextButton(
                        onClick = {
                            showEndDatePicker = false
                            fetchReports()
                        }
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(
                    state = endDateState,
                    showModeToggle = false // Hiding mode toggle saves some vertical space
                )
            }
        }
    }

    if (errorEvent != null) {
        ErrorDialog(
            errorMessage = errorEvent!!,
            onDismiss = { viewModel.clearError() }
        )
    }
}

@Composable
fun DateSelectionCard(
    title: String,
    dateMillis: Long?,
    dateFormatter: SimpleDateFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (dateMillis != null) dateFormatter.format(java.util.Date(dateMillis)) else "Select",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}