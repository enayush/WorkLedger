package com.sharmarefrigeration.workledger.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sharmarefrigeration.workledger.data.InvoiceRepository
import com.sharmarefrigeration.workledger.data.TaskRepository
import com.sharmarefrigeration.workledger.data.UserRepository
import com.sharmarefrigeration.workledger.model.*
import android.content.Context
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

class AdminViewModel : ViewModel() {
    private val invoiceRepository = InvoiceRepository()
    private val taskRepository = TaskRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- PIPELINE 1: OPERATIONS (Tasks) ---
    private val _activeTasks = MutableStateFlow<List<Task>>(emptyList())

    val unassignedTasks: StateFlow<List<Task>> = _activeTasks.map { list ->
        list.filter { it.status == TaskStatus.UNASSIGNED }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val assignedTasks: StateFlow<List<Task>> = _activeTasks.map { list ->
        list.filter { it.status == TaskStatus.ASSIGNED  }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- PIPELINE 2: APPROVALS (Invoices) ---
    private val _pendingInvoices = MutableStateFlow<List<Invoice>>(emptyList())
    val pendingInvoices: StateFlow<List<Invoice>> = _pendingInvoices.asStateFlow()

    private val _technicians = MutableStateFlow<List<User>>(emptyList())
    val technicians: StateFlow<List<User>> = _technicians.asStateFlow()

    private val _accountants = MutableStateFlow<List<User>>(emptyList())
    val accountants: StateFlow<List<User>> = _accountants.asStateFlow()

    init {
        fetchTeam()
        listenToPipes()
    }

    private fun fetchTeam() {
        viewModelScope.launch {
            launch { _technicians.value = userRepository.getStaffByRole(com.sharmarefrigeration.workledger.model.UserRole.EMPLOYEE) }
            launch { _accountants.value = userRepository.getStaffByRole(com.sharmarefrigeration.workledger.model.UserRole.ACCOUNTANT) }
        }
    }

    private fun listenToPipes() {
        viewModelScope.launch {
            // Pipe 1: Operations
            launch { taskRepository.listenToActiveTasksForAdmin().collect { _activeTasks.value = it } }
            // Pipe 2: Approvals (Assuming your repo listens to PAYMENT_RECEIVED)
            launch { invoiceRepository.listenToPendingInvoices().collect { _pendingInvoices.value = it } }
        }
    }

    fun createNewTask(
        companyName: String,
        address: String,
        workNeeded: String,
        dateString: String,
        assignedEmployee: User?, // Null if unassigned!
        onSuccess: () -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val newTask = Task(
                companyName = companyName,
                companyAddress = address,
                workToBeDone = workNeeded, // <--- MAP IT HERE
                workDone = "",
                localDateString = dateString,
                createdByUserId = uid,
                // The magic assignment logic:
                assignedToUserId = assignedEmployee?.id ?: "",
                employeeName = assignedEmployee?.name ?: "Unassigned",
                status = if (assignedEmployee != null) TaskStatus.ASSIGNED else TaskStatus.UNASSIGNED,
                createdAt = Date()
            )

            if (taskRepository.saveTask(newTask)) {
                onSuccess()
            }
            _isLoading.value = false
        }
    }

    fun quickAssignTask(task: Task, employee: User) {
        viewModelScope.launch {
            val updatedTask = task.copy(
                assignedToUserId = employee.id,
                employeeName = employee.name,
                status = TaskStatus.ASSIGNED
            )
            taskRepository.saveTask(updatedTask) // Real-time listener will auto-move it to the assigned list!
        }
    }

    fun approveInvoice(invoice: Invoice, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            if (invoiceRepository.markInvoiceApproved(invoice.id)) {
                if (taskRepository.updateTaskStatus(invoice.taskId, TaskStatus.CLOSED)) {
                    onSuccess()
                } else onError("Invoice approved, but task didn't close.")
            } else onError("Failed to approve invoice.")
            _isLoading.value = false
        }
    }

    fun forceApproveDistributedInvoice(
        invoice: Invoice,
        method: com.sharmarefrigeration.workledger.model.PaymentMethod,
        notes: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            // 1. Force the Invoice Closed
            if (invoiceRepository.adminForceApprovePayment(invoice.id, method, notes)) {
                // 2. Close the original Task
                if (taskRepository.updateTaskStatus(invoice.taskId, com.sharmarefrigeration.workledger.model.TaskStatus.CLOSED)) {
                    onSuccess()
                } else onError("Invoice bypassed, but task didn't close.")
            } else onError("Failed to force approve invoice.")

            _isLoading.value = false
        }
    }

    fun registerNewUser(
        context: Context,
        name: String,
        phoneNumber: String,
        username: String,
        role: com.sharmarefrigeration.workledger.model.UserRole,
        onSuccess: (String) -> Unit, // Returns the generated password
        onError: (String) -> Unit
    ) {
        val formattedPhone = if (phoneNumber.startsWith("+")) phoneNumber else "+91$phoneNumber"
        val cleanUsername = username.trim().lowercase()
        val fakeEmail = "$cleanUsername@sharma.local"

        // Generate a random 6-character alphanumeric password
        val generatedPassword = List(6) { (('a'..'z') + ('A'..'Z') + ('2'..'9')).random() }.joinToString("")

        viewModelScope.launch {
            _isLoading.value = true

            // 1. Check if phone or username already exists
            val existingPhone = userRepository.getUserProfileByPhone(formattedPhone)
            // (Assumes you added getUserProfileByUsername to UserRepository as discussed previously)
            val existingUsername = userRepository.getUserProfileByUsername(cleanUsername)

            if (existingPhone != null) {
                _isLoading.value = false
                onError("This phone number is already registered.")
                return@launch
            }
            if (existingUsername != null) {
                _isLoading.value = false
                onError("Username '$cleanUsername' is already taken.")
                return@launch
            }

            try {
                // 2. Setup the Secondary Firebase App to prevent Admin logout
                val primaryApp = FirebaseApp.getInstance()
                val secondaryApp = try {
                    FirebaseApp.getInstance("SecondaryAdminApp")
                } catch (e: IllegalStateException) {
                    FirebaseApp.initializeApp(context.applicationContext, primaryApp.options, "SecondaryAdminApp")
                }

                // 3. Create the user using the Secondary Auth instance
                val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
                secondaryAuth.createUserWithEmailAndPassword(fakeEmail, generatedPassword).await()

                // 4. Save to Firestore
                val newUser = com.sharmarefrigeration.workledger.model.User(
                    name = name,
                    phoneNumber = formattedPhone,
                    username = cleanUsername, // Make sure 'username' is in your User.kt data class!
                    role = role,
                    isActive = true
                )
                userRepository.createUser(newUser)

                // 5. Cleanup
                secondaryAuth.signOut()
                fetchTeam() // Refresh Admin directory lists

                onSuccess(generatedPassword)

            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.localizedMessage ?: "Failed to create account.")
            }
            _isLoading.value = false
        }
    }

    // --- TASK MANAGEMENT STATE (For Profile Section) ---
    private val _manageUnassignedTasks = MutableStateFlow<List<Task>>(emptyList())
    val manageUnassignedTasks: StateFlow<List<Task>> = _manageUnassignedTasks.asStateFlow()

    private val _manageAssignedTasks = MutableStateFlow<List<Task>>(emptyList())
    val manageAssignedTasks: StateFlow<List<Task>> = _manageAssignedTasks.asStateFlow()

    private val _isManageTasksLoading = MutableStateFlow(false)
    val isManageTasksLoading: StateFlow<Boolean> = _isManageTasksLoading.asStateFlow()

    fun fetchManageableTasks() {
        viewModelScope.launch {
            _isManageTasksLoading.value = true
            val allTasks = taskRepository.getManageableTasks()

            // Split them into two lists for the UI
            _manageUnassignedTasks.value = allTasks.filter { it.status == com.sharmarefrigeration.workledger.model.TaskStatus.UNASSIGNED }
            _manageAssignedTasks.value = allTasks.filter { it.status == com.sharmarefrigeration.workledger.model.TaskStatus.ASSIGNED }

            _isManageTasksLoading.value = false
        }
    }

    fun deleteManageableTask(taskId: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            _isManageTasksLoading.value = true
            val success = taskRepository.deleteTask(taskId)
            if (success) {
                fetchManageableTasks() // Refresh the list automatically
                onSuccess()
            } else {
                _isManageTasksLoading.value = false
                onError()
            }
        }
    }


    // --- REPORTS / PASSBOOK STATE ---
    private val _reportTasks = MutableStateFlow<List<Task>>(emptyList())
    val reportTasks: StateFlow<List<Task>> = _reportTasks.asStateFlow()

    private val _reportInvoices = MutableStateFlow<List<Invoice>>(emptyList())
    val reportInvoices: StateFlow<List<Invoice>> = _reportInvoices.asStateFlow()

    private val _isReportLoading = MutableStateFlow(false)
    val isReportLoading: StateFlow<Boolean> = _isReportLoading.asStateFlow()

    fun searchTasksByDateRange(startMillis: Long, endMillis: Long) {
        viewModelScope.launch {
            _isReportLoading.value = true
            // Convert milliseconds from the date picker to Java Dates
            val startDate = java.util.Date(startMillis)
            // Add 24 hours to the end date to include the entire final day
            val endDate = java.util.Date(endMillis + 86400000)

            _reportTasks.value = taskRepository.getTasksByDateRange(startDate, endDate)
            _isReportLoading.value = false
        }
    }

    fun searchInvoicesByDateRange(startMillis: Long, endMillis: Long) {
        viewModelScope.launch {
            _isReportLoading.value = true
            val startDate = java.util.Date(startMillis)
            val endDate = java.util.Date(endMillis + 86400000)

            _reportInvoices.value = invoiceRepository.getInvoicesByDateRange(startDate, endDate)
            _isReportLoading.value = false
        }
    }

    fun clearReports() {
        _reportTasks.value = emptyList()
        _reportInvoices.value = emptyList()
    }
}