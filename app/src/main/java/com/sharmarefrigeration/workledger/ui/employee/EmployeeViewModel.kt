package com.sharmarefrigeration.workledger.ui.employee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sharmarefrigeration.workledger.data.TaskRepository
import com.sharmarefrigeration.workledger.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.firebase.firestore.DocumentSnapshot

class EmployeeViewModel : ViewModel() {

    private val storageRepository = com.sharmarefrigeration.workledger.data.StorageRepository()
    private val taskRepository = TaskRepository()
    private val auth = FirebaseAuth.getInstance()

    // State holding the list of tasks
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    private val _assignedTasks = MutableStateFlow<List<Task>>(emptyList())
    val assignedTasks: StateFlow<List<Task>> = _assignedTasks.asStateFlow()

    private val _submittedTasks = MutableStateFlow<List<Task>>(emptyList())
    val submittedTasks: StateFlow<List<Task>> = _submittedTasks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0)
    val uploadProgress: StateFlow<Int> = _uploadProgress.asStateFlow()

    private val _historyTasks = MutableStateFlow<List<Task>>(emptyList())
    val historyTasks: StateFlow<List<Task>> = _historyTasks.asStateFlow()

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading.asStateFlow()

    private var lastVisibleHistoryDoc: DocumentSnapshot? = null
    var isLastHistoryPage = false


    fun fetchDashboardData() {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _isLoading.value = true

            // Fetch both lists concurrently
            _assignedTasks.value = taskRepository.getAssignedTasksForEmployee(uid)
            _submittedTasks.value = taskRepository.getRecentSubmittedTasks(uid)

            _isLoading.value = false
        }
    }

    init {
        fetchDashboardData()
    }

    fun saveAdHocTask(
        context: android.content.Context, // NEW
        title: String,
        description: String,
        type: com.sharmarefrigeration.workledger.model.TaskType,
        imageUri: android.net.Uri?,       // NEW
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
            _uploadProgress.value = 0

            var uploadedImageUrl: String? = null
            if (imageUri != null) {
                //Pass the progress callback to the repository
                uploadedImageUrl = storageRepository.uploadJobCardImage(context, imageUri) { progress ->
                    _uploadProgress.value = progress
                }

                if (uploadedImageUrl == null) {
                    _isLoading.value = false
                    onError("Failed to upload image. Check network connection.")
                    return@launch
                }
            }

            // 2. Save the Task to Firestore with the image URL attached
            val newTask = com.sharmarefrigeration.workledger.model.Task(
                title = title,
                description = description,
                assignedToUserId = uid,
                createdByUserId = uid,
                type = type,
                jobCardImageUrl = uploadedImageUrl, // ✅ URL IS SAVED HERE
                status = if (type == com.sharmarefrigeration.workledger.model.TaskType.BILL) {
                    com.sharmarefrigeration.workledger.model.TaskStatus.PENDING_BILLING
                } else {
                    com.sharmarefrigeration.workledger.model.TaskStatus.CLOSED
                },
                completedAt = java.util.Date()
            )

            val success = taskRepository.saveTask(newTask)
            _isLoading.value = false

            if (success) {
                fetchDashboardData()
                onSuccess()
            } else {
                onError("Failed to save task details.")
            }

            _uploadProgress.value = 0
        }
    }

    fun loadHistoryPage(isRefresh: Boolean = false) {
        val uid = auth.currentUser?.uid ?: return

        if (isRefresh) {
            lastVisibleHistoryDoc = null
            isLastHistoryPage = false
            _historyTasks.value = emptyList()
        }

        // Prevent double-fetching or fetching if we hit the end
        if (_isHistoryLoading.value || isLastHistoryPage) return

        viewModelScope.launch {
            _isHistoryLoading.value = true

            val (newTasks, lastDoc) = taskRepository.getEmployeeHistoryPaginated(
                uid = uid,
                lastVisible = lastVisibleHistoryDoc
            )

            if (newTasks.isEmpty()) {
                isLastHistoryPage = true // No more data to fetch!
            } else {
                // Append the new 10 tasks to the existing list
                _historyTasks.value = _historyTasks.value + newTasks
                lastVisibleHistoryDoc = lastDoc

                // If we got fewer than 10, we know we hit the end of the database
                if (newTasks.size < 10) isLastHistoryPage = true
            }

            _isHistoryLoading.value = false
        }
    }
}

