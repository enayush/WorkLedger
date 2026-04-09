package com.sharmarefrigeration.workledger.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

data class Invoice(
    @DocumentId val id: String = "",
    val taskId: String = "", // Links back to the specific Task
    val amount: Double = 0.0,
    val requestedByUserId: String = "", // The Accountant who created it
    val approvedByUserId: String? = null, // The Admin who approved it

    @get:PropertyName("isApproved")
    @set:PropertyName("isApproved")
    var isApproved: Boolean = false,

    val createdAt: Date = Date(),
    val notes: String = "" // Optional notes from accountant to admin
)
