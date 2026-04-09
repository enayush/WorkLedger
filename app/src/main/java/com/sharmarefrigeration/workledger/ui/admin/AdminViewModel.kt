package com.sharmarefrigeration.workledger.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sharmarefrigeration.workledger.data.InvoiceRepository
import com.sharmarefrigeration.workledger.data.TaskRepository
import com.sharmarefrigeration.workledger.model.Invoice
import com.sharmarefrigeration.workledger.model.TaskStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {
    private val invoiceRepository = InvoiceRepository()
    private val taskRepository = TaskRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _pendingInvoices = MutableStateFlow<List<Invoice>>(emptyList())
    val pendingInvoices: StateFlow<List<Invoice>> = _pendingInvoices.asStateFlow()

    private val userRepository = com.sharmarefrigeration.workledger.data.UserRepository()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        listenToLivePendingInvoices()
    }

    private fun listenToLivePendingInvoices() {
        viewModelScope.launch {
            _isLoading.value = true
            invoiceRepository.listenToPendingInvoices().collect { invoices ->
                _pendingInvoices.value = invoices
                _isLoading.value = false
            }
        }
    }

    fun approveInvoice(invoice: Invoice, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val adminId = auth.currentUser?.uid
        if (adminId == null) {
            onError("Admin not logged in")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val invoiceApproved = invoiceRepository.markInvoiceApproved(invoice.id, adminId)
            if (invoiceApproved) {
                val taskClosed = taskRepository.updateTaskStatus(invoice.taskId, TaskStatus.CLOSED)
                if (taskClosed) {
                    onSuccess()
                } else {
                    onError("Invoice approved, but failed to close the task.")
                }
            } else {
                onError("Failed to approve invoice.")
            }
            _isLoading.value = false
        }
    }

    fun registerNewUser(
        name: String,
        phoneNumber: String,
        role: com.sharmarefrigeration.workledger.model.UserRole,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Simple validation to ensure standard +91 format
        val formattedPhone = if (phoneNumber.startsWith("+")) phoneNumber else "+91$phoneNumber"

        viewModelScope.launch {
            _isLoading.value = true

            // First, check if this number already exists so we don't make duplicates
            val existingUser = userRepository.getUserProfileByPhone(formattedPhone)
            if (existingUser != null) {
                _isLoading.value = false
                onError("This phone number is already registered.")
                return@launch
            }

            // Create the new user object
            val newUser = com.sharmarefrigeration.workledger.model.User(
                name = name,
                phoneNumber = formattedPhone,
                role = role,
                isActive = true
            )

            val success = userRepository.createUser(newUser)
            _isLoading.value = false

            if (success) {
                onSuccess()
            } else {
                onError("Failed to register user. Check network.")
            }
        }
    }
}