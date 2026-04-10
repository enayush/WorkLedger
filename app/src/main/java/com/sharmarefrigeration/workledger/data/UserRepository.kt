package com.sharmarefrigeration.workledger.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.sharmarefrigeration.workledger.model.User

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("Users")

    suspend fun getUserProfileByPhone(phoneNumber: String): User? {
        return try {
            val snapshot = usersCollection
                .whereEqualTo("phoneNumber", phoneNumber)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                snapshot.documents[0].toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getUserProfileByUsername(username: String): User? {
        return try {
            val snapshot = usersCollection
                .whereEqualTo("username", username) // Search by username now
                .get()
                .await()

            if (!snapshot.isEmpty) {
                snapshot.documents[0].toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun createUser(user: User): Boolean {
        return try {
            // Generate a random document ID since we don't know their UID yet
            val docRef = usersCollection.document()
            val userToSave = user.copy(id = docRef.id)

            docRef.set(userToSave).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getStaffByRole(role: com.sharmarefrigeration.workledger.model.UserRole): List<com.sharmarefrigeration.workledger.model.User> {
        return try {
            val snapshot = usersCollection
                .whereEqualTo("role", role.name)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            snapshot.toObjects(com.sharmarefrigeration.workledger.model.User::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }


}