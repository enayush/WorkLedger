package com.sharmarefrigeration.workledger.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import com.sharmarefrigeration.workledger.model.Task
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.channels.awaitClose

class TaskRepository {
    private val db = FirebaseFirestore.getInstance()
    private val tasksCollection = db.collection("Tasks")

    // Flow 1: Admin or Employee creates a new task record
    suspend fun saveTask(task: Task): Boolean {
        return try {
            // If the task has no ID (new), Firebase generates one.
            // If it has an ID, it updates the existing document.
            val docRef = if (task.id.isEmpty()) tasksCollection.document() else tasksCollection.document(task.id)

            // Create a copy of the task with the generated ID
            val taskToSave = task.copy(id = docRef.id)

            docRef.set(taskToSave).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Flow 2: Employee looking at their assigned tasks
    suspend fun getAssignedTasksForEmployee(uid: String): List<Task> {
        return try {
            val snapshot = tasksCollection
                .whereEqualTo("assignedToUserId", uid)
                .whereEqualTo("status", com.sharmarefrigeration.workledger.model.TaskStatus.ASSIGNED)
                .get()
                .await()

            snapshot.toObjects(Task::class.java).sortedBy { it.createdAt }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getRecentSubmittedTasks(uid: String): List<Task> {
        return try {
            val snapshot = tasksCollection
                .whereEqualTo("assignedToUserId", uid)
                // We fetch tasks that are waiting for billing or already closed
                .whereIn("status", listOf(
                    com.sharmarefrigeration.workledger.model.TaskStatus.PENDING_BILLING,
                    com.sharmarefrigeration.workledger.model.TaskStatus.PENDING_APPROVAL,
                    com.sharmarefrigeration.workledger.model.TaskStatus.CLOSED
                ))
                .get()
                .await()

            // Sort them so the newest ones are at the top
            val tasks = snapshot.toObjects(Task::class.java).sortedByDescending { it.completedAt }

            // To keep it clean, we'll just return the 10 most recent ones
            tasks.take(10)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 1. Fetch only the tasks waiting for the accountant
    suspend fun getPendingBillingTasks(): List<Task> {
        return try {
            val snapshot = tasksCollection
                // We only care about tasks stuck in this specific state
                .whereEqualTo("status", com.sharmarefrigeration.workledger.model.TaskStatus.PENDING_BILLING.name)
                .get()
                .await()

            // Sort them locally so the oldest tasks (most urgent) are at the top
            val tasks = snapshot.toObjects(Task::class.java)
            tasks.sortedBy { it.createdAt }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 2. Update a task's status (used when Accountant pushes it to Admin)
    suspend fun updateTaskStatus(taskId: String, newStatus: com.sharmarefrigeration.workledger.model.TaskStatus): Boolean {
        return try {
            tasksCollection.document(taskId)
                .update("status", newStatus)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getEmployeeHistoryPaginated(
        uid: String,
        lastVisible: DocumentSnapshot?,
        limit: Long = 10
    ): Pair<List<Task>, DocumentSnapshot?> {
        return try {
            var query = tasksCollection
                .whereEqualTo("assignedToUserId", uid)
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .limit(limit)

            // If we have a bookmark from the last query, start AFTER it
            if (lastVisible != null) {
                query = query.startAfter(lastVisible)
            }

            val snapshot = query.get().await()

            val tasks = snapshot.toObjects(Task::class.java)

            // Get the last document to act as the bookmark for the next page
            val newLastVisible = if (snapshot.documents.isNotEmpty()) {
                snapshot.documents[snapshot.documents.size - 1]
            } else null

            Pair(tasks, newLastVisible)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(emptyList(), null)
        }
    }

    fun listenToPendingBillingTasks(): kotlinx.coroutines.flow.Flow<List<com.sharmarefrigeration.workledger.model.Task>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = tasksCollection
            // Make sure this status matches whatever you use for bills waiting to be processed!
            .whereEqualTo("status", com.sharmarefrigeration.workledger.model.TaskStatus.PENDING_BILLING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val tasks = snapshot.toObjects(com.sharmarefrigeration.workledger.model.Task::class.java)
                    trySend(tasks) // Instantly push new or deleted tasks to the UI
                }
            }

        awaitClose { listener.remove() }
    }

    // --- LIVE LISTENERS FOR EMPLOYEE DASHBOARD ---

    fun listenToAssignedTasksForEmployee(uid: String): kotlinx.coroutines.flow.Flow<List<Task>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = tasksCollection
            .whereEqualTo("assignedToUserId", uid)
            .whereEqualTo("status", com.sharmarefrigeration.workledger.model.TaskStatus.ASSIGNED)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val tasks = snapshot.toObjects(Task::class.java).sortedBy { it.createdAt }
                    trySend(tasks)
                }
            }
        awaitClose { listener.remove() }
    }

    fun listenToRecentSubmittedTasks(uid: String): kotlinx.coroutines.flow.Flow<List<Task>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = tasksCollection
            .whereEqualTo("assignedToUserId", uid)
            .whereIn("status", listOf(
                com.sharmarefrigeration.workledger.model.TaskStatus.PENDING_BILLING,
                com.sharmarefrigeration.workledger.model.TaskStatus.PENDING_APPROVAL,
                com.sharmarefrigeration.workledger.model.TaskStatus.CLOSED
            ))
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val tasks = snapshot.toObjects(Task::class.java)
                        .sortedByDescending { it.completedAt }
                        .take(10) // Only keep top 10
                    trySend(tasks)
                }
            }
        awaitClose { listener.remove() }
    }

    fun listenToActiveTasksForAdmin(): kotlinx.coroutines.flow.Flow<List<Task>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = tasksCollection
            .whereIn("status", listOf(
                com.sharmarefrigeration.workledger.model.TaskStatus.UNASSIGNED,
                com.sharmarefrigeration.workledger.model.TaskStatus.ASSIGNED,
                com.sharmarefrigeration.workledger.model.TaskStatus.PENDING_BILLING
            ))
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                snapshot?.let { trySend(it.toObjects(Task::class.java)) }
            }
        awaitClose { listener.remove() }
    }

    // Flow: Employee completes an assigned task
    suspend fun completeAssignedTask(
        taskId: String,
        workDone: String,
        type: com.sharmarefrigeration.workledger.model.TaskType,
        jobCardId: String?
    ): Boolean {
        return try {
            val newStatus = if (type == com.sharmarefrigeration.workledger.model.TaskType.BILL) {
                com.sharmarefrigeration.workledger.model.TaskStatus.PENDING_BILLING
            } else {
                com.sharmarefrigeration.workledger.model.TaskStatus.CLOSED
            }

            tasksCollection.document(taskId).update(
                mapOf(
                    "workDone" to workDone,
                    "type" to type,
                    "jobCardId" to jobCardId,
                    "status" to newStatus,
                    "completedAt" to java.util.Date()
                )
            ).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Fetch active/unassigned tasks ONCE (no flow/listener)
    suspend fun getManageableTasks(): List<Task> {
        return try {
            val snapshot = tasksCollection
                .whereIn("status", listOf(
                    com.sharmarefrigeration.workledger.model.TaskStatus.UNASSIGNED.name,
                    com.sharmarefrigeration.workledger.model.TaskStatus.ASSIGNED.name
                ))
                .get()
                .await()

            // Fetch and sort locally so we don't trigger a missing Firebase Index error
            snapshot.toObjects(Task::class.java).sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Permanently delete a task
    suspend fun deleteTask(taskId: String): Boolean {
        return try {
            tasksCollection.document(taskId).delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getTasksByDateRange(startDate: java.util.Date, endDate: java.util.Date): List<Task> {
        return try {
            val snapshot = tasksCollection
                .whereGreaterThanOrEqualTo("createdAt", startDate)
                .whereLessThanOrEqualTo("createdAt", endDate)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.toObjects(Task::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}