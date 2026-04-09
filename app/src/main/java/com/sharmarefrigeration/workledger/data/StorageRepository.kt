package com.sharmarefrigeration.workledger.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID

class StorageRepository {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    suspend fun uploadJobCardImage(
        context: Context,
        imageUri: Uri,
        onProgress: (Int) -> Unit
    ): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) return null

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos)
            val data = baos.toByteArray()

            val uniqueFilename = java.util.UUID.randomUUID().toString() + ".jpg"
            val imageRef = storageRef.child("job_cards/$uniqueFilename")

            // 1. Create the task but don't await it immediately
            val uploadTask = imageRef.putBytes(data)

            // 2. Attach a listener to calculate the percentage!
            uploadTask.addOnProgressListener { snapshot ->
                val progress = (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount).toInt()
                onProgress(progress) // Send the number back to the ViewModel
            }

            // 3. Now wait for it to actually finish
            uploadTask.await()

            // 4. Return the URL
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}