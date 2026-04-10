package com.sharmarefrigeration.workledger.data

import com.google.firebase.firestore.FirebaseFirestore
import com.sharmarefrigeration.workledger.model.Invoice
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.sharmarefrigeration.workledger.model.InvoiceStatus
import com.sharmarefrigeration.workledger.model.PaymentMethod
import com.sharmarefrigeration.workledger.model.TaskStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.collections.remove

class InvoiceRepository {
    private val db = FirebaseFirestore.getInstance()
    private val invoiceCollection = db.collection("Invoices")

    suspend fun createInvoiceAndUpdateTask(invoice: Invoice, newStatus: TaskStatus): Boolean {
        return try {
            val batch = db.batch()

            // 1. Create the Invoice
            val invoiceRef = invoiceCollection.document()
            val invoiceToSave = invoice.copy(id = invoiceRef.id)
            batch.set(invoiceRef, invoiceToSave)

            // 2. Update the Task
            val taskRef = db.collection("Tasks").document(invoice.taskId)
            // Storing the enum name as string if that's how it's stored, or the object. If tasks.status is stored as string:
            batch.update(taskRef, "status", newStatus.name)

            // Commit both at exactly the same time
            batch.commit().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun listenToRecentInvoices(accountantId: String): Flow<List<Invoice>> = callbackFlow {
        val listener = invoiceCollection
            .whereEqualTo("requestedByUserId", accountantId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error) // Close the flow if there's an error
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val invoices = snapshot.toObjects(Invoice::class.java)
                    trySend(invoices) // Instantly push new data to the UI!
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getAccountantHistoryPaginated(
        uid: String,
        lastVisible: DocumentSnapshot?,
        limit: Long = 10
    ): Pair<List<Invoice>, DocumentSnapshot?> {
        return try {
            var query = invoiceCollection
                .whereEqualTo("requestedByUserId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)

            if (lastVisible != null) {
                query = query.startAfter(lastVisible)
            }

            val snapshot = query.get().await()
            val invoices = snapshot.toObjects(Invoice::class.java)

            val newLastVisible = if (snapshot.documents.isNotEmpty()) {
                snapshot.documents[snapshot.documents.size - 1]
            } else null

            Pair(invoices, newLastVisible)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(emptyList(), null)
        }
    }

    fun listenToPendingInvoices(): Flow<List<Invoice>> = callbackFlow {
        val listener = invoiceCollection
            .whereIn("status", listOf(
                InvoiceStatus.DISTRIBUTED.name,
                InvoiceStatus.PAYMENT_RECEIVED.name
            ))
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                snapshot?.let { trySend(it.toObjects(Invoice::class.java)) }
            }
        awaitClose { listener.remove() }
    }

    suspend fun updatePaymentDetails(invoiceId: String, method: PaymentMethod, refNote: String): Boolean {
        return try {
            invoiceCollection.document(invoiceId).update(
                mapOf(
                    "status" to InvoiceStatus.PAYMENT_RECEIVED.name,
                    "paymentMethod" to method.name,
                    "paymentNotes" to refNote,
                    "paidAt" to java.util.Date()
                )
            ).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun markInvoiceApproved(invoiceId: String): Boolean {
        return try {
            invoiceCollection.document(invoiceId).update(
                "status", InvoiceStatus.APPROVED.name,
                "approvedAt", java.util.Date()
            ).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun markInvoiceApprovedAndUpdateTask(invoiceId: String, taskId: String): Boolean {
        return try {
            val batch = db.batch()

            val invoiceRef = invoiceCollection.document(invoiceId)
            batch.update(invoiceRef, mapOf(
                "status" to InvoiceStatus.APPROVED.name,
                "approvedAt" to java.util.Date()
            ))

            val taskRef = db.collection("Tasks").document(taskId)
            batch.update(taskRef, "status", TaskStatus.CLOSED.name) // or use enum value depending on how it's saved. Usually string or enum works.

            batch.commit().await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun updateInvoiceDistribution(invoiceId: String, newStatus: InvoiceStatus, personName: String, address: String): Boolean {
        return try {
            invoiceCollection.document(invoiceId).update(
                mapOf(
                    "status" to newStatus.name,
                    "distributedToPerson" to personName,
                    "distributedAddress" to address,
                    "distributedAt" to java.util.Date()
                )
            ).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun adminForceApprovePaymentAndUpdateTask(
        invoiceId: String,
        taskId: String,
        method: PaymentMethod,
        refNote: String
    ): Boolean {
        return try {
            val batch = db.batch()

            val invoiceRef = invoiceCollection.document(invoiceId)
            batch.update(invoiceRef, mapOf(
                "status" to InvoiceStatus.APPROVED.name, // Immediately moves to final state!
                "paymentMethod" to method.name,
                "paymentNotes" to refNote,
                "adminConfirmedPayment" to true,
                "accountantConfirmedPayment" to true, // Force this true so it's fully closed
                "paidAt" to java.util.Date(),
                "approvedAt" to java.util.Date()
            ))

            val taskRef = db.collection("Tasks").document(taskId)
            batch.update(taskRef, "status", TaskStatus.CLOSED.name)

            batch.commit().await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun getInvoicesByDateRange(startDate: java.util.Date, endDate: java.util.Date): List<Invoice> {
        return try {
            val snapshot = invoiceCollection
                .whereGreaterThanOrEqualTo("createdAt", startDate)
                .whereLessThanOrEqualTo("createdAt", endDate)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.toObjects(Invoice::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

}