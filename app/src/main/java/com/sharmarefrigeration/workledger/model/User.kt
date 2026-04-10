package com.sharmarefrigeration.workledger.model

import com.google.firebase.firestore.DocumentId

enum class UserRole {
    ADMIN, ACCOUNTANT, EMPLOYEE, UNKNOWN
}

data class User(
    @DocumentId val id: String = "", // This will be the Firebase Auth UID
    val phoneNumber: String = "",
    val name: String = "",
    val username: String = "",
    val role: UserRole = UserRole.UNKNOWN,
    val isActive: Boolean = true // Soft delete: Admin can disable users instead of deleting them
)