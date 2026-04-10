package com.sharmarefrigeration.workledger.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

enum class TaskStatus {
    UNASSIGNED,
    ASSIGNED,
    IN_PROGRESS,
    PENDING_BILLING,
    PENDING_APPROVAL,
    CLOSED
}

enum class TaskType {
    BILL, AMC, UNKNOWN
}

data class Task(
    @DocumentId val id: String = "",
    val companyName: String = "",
    val companyAddress: String = "",

    val workToBeDone: String = "",
    val workDone: String = "",

    val jobCardId: String? = null,
    val localDateString: String = "",

    val assignedToUserId: String = "",
    val createdByUserId: String = "",
    val employeeName: String = "",

    val status: TaskStatus = TaskStatus.ASSIGNED,
    val type: TaskType = TaskType.UNKNOWN,
    val employeeComments: String = "",
    val createdAt: Date = Date(),
    val completedAt: Date? = null
)