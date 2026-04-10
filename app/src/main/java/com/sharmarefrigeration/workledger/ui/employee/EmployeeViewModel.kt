package com.sharmarefrigeration.workledger.ui.employee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.sharmarefrigeration.workledger.data.TaskRepository
import com.sharmarefrigeration.workledger.model.Task
import com.sharmarefrigeration.workledger.model.TaskStatus
import com.sharmarefrigeration.workledger.model.TaskType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

class EmployeeViewModel : ViewModel() {

    private val taskRepository = TaskRepository()
    private val auth = FirebaseAuth.getInstance()

    // Dashboard State
    private val uid = auth.currentUser?.uid
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Replacing manual collect with stateIn
    val assignedTasks: StateFlow<List<Task>> = if (uid != null) {
        taskRepository.listenToAssignedTasksForEmployee(uid)
            .onStart { _isLoading.value = true }
            .onEach { _isLoading.value = false }
            .catch { e ->
                _isLoading.value = false
                e.printStackTrace()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    } else MutableStateFlow(emptyList())

    val submittedTasks: StateFlow<List<Task>> = if (uid != null) {
        taskRepository.listenToRecentSubmittedTasks(uid)
            .catch { it.printStackTrace() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    } else MutableStateFlow(emptyList())

    // History Pagination State
    private val _historyTasks = MutableStateFlow<List<Task>>(emptyList())
    val historyTasks: StateFlow<List<Task>> = _historyTasks.asStateFlow()

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading.asStateFlow()

    private var lastVisibleHistoryDoc: DocumentSnapshot? = null
    private val _isLastHistoryPage = MutableStateFlow(false)
    val isLastHistoryPage: StateFlow<Boolean> = _isLastHistoryPage.asStateFlow()

    // ✅ Cleaned up, streamlined save function with the new Data Model
    fun saveAdHocTask(
        companyName: String,
        companyAddress: String,
        workDone: String,
        type: TaskType,
        jobCardId: String?,
        employeeName: String,
        localDateString: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("User not logged in")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            val newTask = Task(
                companyName = companyName,
                companyAddress = companyAddress,
                workDone = workDone,
                jobCardId = jobCardId,
                localDateString = localDateString,
                employeeName = employeeName,
                assignedToUserId = uid,
                createdByUserId = uid,
                type = type,
                status = if (type == TaskType.BILL) TaskStatus.PENDING_BILLING else TaskStatus.CLOSED,
                completedAt = Date()
            )

            val success = taskRepository.saveTask(newTask)
            _isLoading.value = false

            if (success) {
                onSuccess()
            } else {
                onError("Failed to save task details.")
            }
        }
    }

    fun loadHistoryPage(isRefresh: Boolean = false, targetUserId: String? = null) {
        // Use targetUserId if provided, otherwise fallback to the logged-in user
        val uid = targetUserId ?: auth.currentUser?.uid ?: return

        if (isRefresh) {
            lastVisibleHistoryDoc = null
            _isLastHistoryPage.value = false
            _historyTasks.value = emptyList()
        }

        if (_isHistoryLoading.value || _isLastHistoryPage.value) return

        viewModelScope.launch {
            _isHistoryLoading.value = true

            val (newTasks, lastDoc) = taskRepository.getEmployeeHistoryPaginated(
                uid = uid,
                lastVisible = lastVisibleHistoryDoc
            )

            if (newTasks.isEmpty()) {
                _isLastHistoryPage.value = true
            } else {
                _historyTasks.value = _historyTasks.value + newTasks
                lastVisibleHistoryDoc = lastDoc

                if (newTasks.size < 10) _isLastHistoryPage.value = true
            }

            _isHistoryLoading.value = false
        }
    }

    // Helper to get a specific task from the assigned list
    fun getAssignedTaskById(taskId: String): Task? {
        return assignedTasks.value.find { it.id == taskId }
    }

    // New function to update an existing task instead of creating a new one
    fun completeAssignedTask(
        taskId: String,
        workDone: String,
        type: TaskType,
        jobCardId: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = taskRepository.completeAssignedTask(taskId, workDone, type, jobCardId)
            _isLoading.value = false

            if (success) {
                onSuccess()
            } else {
                onError("Failed to submit work.")
            }
        }
    }
}