package com.sharmarefrigeration.workledger.data

import com.google.firebase.firestore.FirebaseFirestore
import com.sharmarefrigeration.workledger.model.Invoice
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class InvoiceRepository {
    private val db = FirebaseFirestore.getInstance()
    private val invoiceCollection = db.collection("Invoices")

    suspend fun createInvoice(invoice: Invoice): Boolean {
        return try {
            // Generate a new ID for the invoice
            val docRef = invoiceCollection.document()
            val invoiceToSave = invoice.copy(id = docRef.id)

            docRef.set(invoiceToSave).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 1. Fetch invoices waiting for Admin approval
    suspend fun getPendingInvoices(): List<Invoice> {
        return try {
            val snapshot = invoiceCollection
                .whereEqualTo("approved", false)
                .get()
                .await()

            snapshot.toObjects(Invoice::class.java).sortedBy { it.createdAt }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 2. Mark invoice as approved by the Admin
    suspend fun markInvoiceApproved(invoiceId: String, adminId: String): Boolean {
        return try {
            invoiceCollection.document(invoiceId)
                .update(
                    "isApproved", true,
                    "approvedByUserId", adminId
                )
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getRecentInvoices(accountantId: String): List<com.sharmarefrigeration.workledger.model.Invoice> {
        return try {
            val snapshot = invoiceCollection
                .whereEqualTo("requestedByUserId", accountantId)
                .get()
                .await()

            // Sort them so the newest ones are at the top, and take the latest 10
            // Note: We use the 'createdAt' field we saw in your Firestore screenshot!
            snapshot.toObjects(com.sharmarefrigeration.workledger.model.Invoice::class.java)
                .sortedByDescending { it.createdAt }
                .take(10)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun listenToRecentInvoices(accountantId: String): Flow<List<com.sharmarefrigeration.workledger.model.Invoice>> = callbackFlow {
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
                    val invoices = snapshot.toObjects(com.sharmarefrigeration.workledger.model.Invoice::class.java)
                    trySend(invoices) // Instantly push new data to the UI!
                }
            }

        // When the ViewModel dies, this cleanly closes the Firebase pipe
        awaitClose { listener.remove() }
    }

    suspend fun getAccountantHistoryPaginated(
        uid: String,
        lastVisible: DocumentSnapshot?,
        limit: Long = 10
    ): Pair<List<com.sharmarefrigeration.workledger.model.Invoice>, DocumentSnapshot?> {
        return try {
            var query = invoiceCollection
                .whereEqualTo("requestedByUserId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)

            if (lastVisible != null) {
                query = query.startAfter(lastVisible)
            }

            val snapshot = query.get().await()
            val invoices = snapshot.toObjects(com.sharmarefrigeration.workledger.model.Invoice::class.java)

            val newLastVisible = if (snapshot.documents.isNotEmpty()) {
                snapshot.documents[snapshot.documents.size - 1]
            } else null

            Pair(invoices, newLastVisible)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(emptyList(), null)
        }
    }

    fun listenToPendingInvoices(): kotlinx.coroutines.flow.Flow<List<com.sharmarefrigeration.workledger.model.Invoice>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = invoiceCollection
            .whereEqualTo("isApproved", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val invoices = snapshot.toObjects(com.sharmarefrigeration.workledger.model.Invoice::class.java)
                    trySend(invoices)
                }
            }

        awaitClose { listener.remove() }
    }
}