package com.sharmarefrigeration.workledger.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

enum class InvoiceStatus {
    CREATED,           // 1. Made by Accountant
    DISTRIBUTED,       // 2. Sent to Client
    PAYMENT_RECEIVED,  // 3. Accountant collected money
    APPROVED           // 4. Admin tallied and closed (or bypassed accountant)
}

enum class PaymentMethod { CASH, CHEQUE, ONLINE, PENDING }

data class Invoice(
    @DocumentId val id: String = "",
    val taskId: String = "",
    val requestedByUserId: String = "",

    val companyName: String = "",
    val workDone: String = "",
    val jobCardId: String? = null,
    val employeeName: String = "",

    // --- Billing Details ---
    val amount: Double = 0.0,
    val notes: String = "",

    // --- NEW: Distribution Details ---
    val distributedToPerson: String = "",
    val distributedAddress: String = "",

    // --- Pipeline State ---
    val status: InvoiceStatus = InvoiceStatus.CREATED,
    val paymentMethod: PaymentMethod = PaymentMethod.PENDING,
    val paymentNotes: String = "",

    // Explicit Confirmation Flags
    val accountantConfirmedPayment: Boolean = false,
    val adminConfirmedPayment: Boolean = false,

    val createdAt: Date = Date(),
    val distributedAt: Date? = null,
    val paidAt: Date? = null,
    val approvedAt: Date? = null
)