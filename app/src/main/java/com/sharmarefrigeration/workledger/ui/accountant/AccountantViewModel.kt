package com.sharmarefrigeration.workledger.ui.accountant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sharmarefrigeration.workledger.data.InvoiceRepository
import com.sharmarefrigeration.workledger.data.TaskRepository
import com.sharmarefrigeration.workledger.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AccountantViewModel : ViewModel() {
    private val taskRepository = TaskRepository()
    private val invoiceRepository = InvoiceRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _historyInvoices = MutableStateFlow<List<Invoice>>(emptyList())
    val historyInvoices: StateFlow<List<Invoice>> = _historyInvoices.asStateFlow()

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading.asStateFlow()

    private var lastVisibleHistoryDoc: com.google.firebase.firestore.DocumentSnapshot? = null

    private val _isLastHistoryPage = MutableStateFlow(false)
    val isLastHistoryPage: StateFlow<Boolean> = _isLastHistoryPage.asStateFlow()

    // PIPELINE BUCKETS
    val tasksNeedingBills: StateFlow<List<Task>> = taskRepository.listenToPendingBillingTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val activeInvoices: StateFlow<List<Invoice>> = (auth.currentUser?.uid?.let { uid ->
        invoiceRepository.listenToRecentInvoices(uid)
    } ?: emptyFlow()).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invoicesToDistribute: StateFlow<List<Invoice>> = activeInvoices.map { list ->
        list.filter { it.status == InvoiceStatus.CREATED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invoicesToCollect: StateFlow<List<Invoice>> = activeInvoices.map { list ->
        list.filter { it.status == InvoiceStatus.DISTRIBUTED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun generateBill(task: Task, amount: Double, notes: String, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true

            val newInvoice = Invoice(
                taskId = task.id,
                requestedByUserId = uid,
                amount = amount,
                notes = notes,
                status = InvoiceStatus.CREATED,
                companyName = task.companyName,
                workDone = task.workDone,
                jobCardId = task.jobCardId,
                employeeName = task.employeeName
            )

            if (invoiceRepository.createInvoiceAndUpdateTask(newInvoice, TaskStatus.PENDING_APPROVAL)) {
                onSuccess()
            }
            _isLoading.value = false
        }
    }

    fun markAsDistributed(invoiceId: String, personName: String, address: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true

            // Note: You will need to update your InvoiceRepository to accept these extra strings!
            invoiceRepository.updateInvoiceDistribution(invoiceId, InvoiceStatus.DISTRIBUTED, personName, address)

            onSuccess()
            _isLoading.value = false
        }
    }

    fun confirmPayment(invoiceId: String, method: PaymentMethod, referenceNote: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            invoiceRepository.updatePaymentDetails(invoiceId, method, referenceNote)
            onSuccess()
            _isLoading.value = false
        }
    }

    fun loadHistoryPage(isRefresh: Boolean = false, targetUserId: String? = null) {
        // Use targetUserId if provided, otherwise fallback to the logged-in user
        val uid = targetUserId ?: auth.currentUser?.uid ?: return

        if (isRefresh) {
            lastVisibleHistoryDoc = null
            _isLastHistoryPage.value = false
            _historyInvoices.value = emptyList()
        }

        if (_isHistoryLoading.value || _isLastHistoryPage.value) return

        viewModelScope.launch {
            _isHistoryLoading.value = true

            val (newInvoices, lastDoc) = invoiceRepository.getAccountantHistoryPaginated(uid, lastVisibleHistoryDoc)

            if (newInvoices.isEmpty()) {
                _isLastHistoryPage.value = true
            } else {
                _historyInvoices.value = _historyInvoices.value + newInvoices
                lastVisibleHistoryDoc = lastDoc
                if (newInvoices.size < 10) _isLastHistoryPage.value = true
            }
            _isHistoryLoading.value = false
        }
    }
}