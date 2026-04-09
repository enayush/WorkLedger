package com.sharmarefrigeration.workledger.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

enum class TaskStatus {
    ASSIGNED,          // Admin created it, waiting for Employee
    IN_PROGRESS,       // Employee is actively working on it
    PENDING_BILLING,   // Employee finished, it's a BILL task, waiting for Accountant
    PENDING_APPROVAL,  // Accountant generated bill, waiting for Admin
    CLOSED             // Done. AMC is closed immediately, Bills close after Admin approval
}

enum class TaskType {
    BILL, AMC, UNKNOWN
}

data class Task(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val assignedToUserId: String = "", // Links to the User ID of the employee
    val createdByUserId: String = "",  // The ID of the person who created this database entry
    val status: TaskStatus = TaskStatus.ASSIGNED,
    val type: TaskType = TaskType.UNKNOWN,
    val jobCardImageUrl: String? = null,
    val employeeComments: String = "",
    val createdAt: Date = Date(),
    val completedAt: Date? = null
)
