package com.sharmarefrigeration.workledger.ui.accountant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.sharmarefrigeration.workledger.data.InvoiceRepository
import com.sharmarefrigeration.workledger.data.TaskRepository
import com.sharmarefrigeration.workledger.model.Invoice
import com.sharmarefrigeration.workledger.model.Task
import com.sharmarefrigeration.workledger.model.TaskStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountantViewModel : ViewModel() {
    private val taskRepository = TaskRepository()
    private val invoiceRepository = InvoiceRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _todayTotalValue = MutableStateFlow(0.0)
    val todayTotalValue: StateFlow<Double> = _todayTotalValue.asStateFlow()

    private val _todayBillsProcessed = MutableStateFlow(0)
    val todayBillsProcessed: StateFlow<Int> = _todayBillsProcessed.asStateFlow()

    // 1. The Inbox (Pending Tasks)
    private val _pendingTasks = MutableStateFlow<List<Task>>(emptyList())
    val pendingTasks: StateFlow<List<Task>> = _pendingTasks.asStateFlow()

    // 2. The Outbox (Recent Invoices)
    private val _recentInvoices = MutableStateFlow<List<Invoice>>(emptyList())
    val recentInvoices: StateFlow<List<Invoice>> = _recentInvoices.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _historyInvoices = MutableStateFlow<List<Invoice>>(emptyList())
    val historyInvoices: StateFlow<List<Invoice>> = _historyInvoices.asStateFlow()

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading.asStateFlow()

    private var lastVisibleHistoryDoc: DocumentSnapshot? = null
    var isLastHistoryPage = false

    init {
        listenToLivePendingTasks()
        listenToLiveInvoices()
    }

    private fun listenToLivePendingTasks() {
        viewModelScope.launch {
            taskRepository.listenToPendingBillingTasks().collect { tasks ->
                _pendingTasks.value = tasks
            }
        }
    }
    fun fetchDashboardData() {
        viewModelScope.launch {
            _isLoading.value = true
            kotlinx.coroutines.delay(500)
            _isLoading.value = false
        }
    }

    // The Real-Time Listener (Runs silently in background)
    private fun listenToLiveInvoices() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            // This relies on the function we added to InvoiceRepository earlier!
            invoiceRepository.listenToRecentInvoices(uid).collect { invoices ->
                // This triggers instantly whenever Firestore changes!
                _recentInvoices.value = invoices
                calculateTodayStats(invoices)
            }
        }
    }

    // The Pagination Logic for the Billing Archive Profile Section
    fun loadHistoryPage(isRefresh: Boolean = false) {
        val uid = auth.currentUser?.uid ?: return

        if (isRefresh) {
            lastVisibleHistoryDoc = null
            isLastHistoryPage = false
            _historyInvoices.value = emptyList()
        }

        if (_isHistoryLoading.value || isLastHistoryPage) return

        viewModelScope.launch {
            _isHistoryLoading.value = true

            // This relies on the paginated function in InvoiceRepository
            val (newInvoices, lastDoc) = invoiceRepository.getAccountantHistoryPaginated(uid, lastVisibleHistoryDoc)

            if (newInvoices.isEmpty()) {
                isLastHistoryPage = true
            } else {
                _historyInvoices.value = _historyInvoices.value + newInvoices
                lastVisibleHistoryDoc = lastDoc
                if (newInvoices.size < 10) isLastHistoryPage = true
            }
            _isHistoryLoading.value = false
        }
    }

    private fun calculateTodayStats(invoices: List<Invoice>) {
        val today = java.util.Calendar.getInstance()
        today.set(java.util.Calendar.HOUR_OF_DAY, 0)
        today.set(java.util.Calendar.MINUTE, 0)
        today.set(java.util.Calendar.SECOND, 0)

        val todaysInvoices = invoices.filter {
            it.createdAt != null && it.createdAt!!.after(today.time)
        }

        _todayBillsProcessed.value = todaysInvoices.size
        _todayTotalValue.value = todaysInvoices.sumOf { it.amount }
    }

    fun requestAdminApproval(task: Task, amount: Double, notes: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Authentication error")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            val newInvoice = Invoice(
                taskId = task.id,
                amount = amount,
                requestedByUserId = uid,
                notes = notes,
                isApproved = false
            )

            val invoiceSaved = invoiceRepository.createInvoice(newInvoice)

            if (invoiceSaved) {
                val taskUpdated = taskRepository.updateTaskStatus(task.id, TaskStatus.PENDING_APPROVAL)

                if (taskUpdated) {
                    onSuccess()
                } else {
                    onError("Invoice created, but failed to update task status.")
                }
            } else {
                onError("Failed to create invoice.")
            }
            _isLoading.value = false
        }
    }




}