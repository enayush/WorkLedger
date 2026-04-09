package com.sharmarefrigeration.workledger.ui.employee

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.sharmarefrigeration.workledger.model.TaskType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Helper function to create a temporary file for the camera to write to
fun Context.createImageFile(): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = File(cacheDir, "images").apply { mkdirs() }
    return File.createTempFile("JOBCARD_${timeStamp}_", ".jpg", storageDir)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogWorkScreen(
    viewModel: EmployeeViewModel,
    onBack: () -> Unit,
    onTaskSaved: () -> Unit
) {
    val context = LocalContext.current

    val focusManager = LocalFocusManager.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TaskType.AMC) }

    // State to hold the image we just took
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    val isLoading by viewModel.isLoading.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()

    BackHandler(enabled = isLoading) {
        Toast.makeText(context, "Please wait, saving job details...", Toast.LENGTH_SHORT).show()
    }

    // The Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // If the user hit the checkmark in the camera, save the URI!
            capturedImageUri = tempImageUri
        }
    }

    Scaffold() { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Short Title (e.g., AC Repair)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Details / Replaced Parts") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Text("Job Type", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedType == TaskType.AMC,
                        onClick = { selectedType = TaskType.AMC }
                    )
                    Text("AMC (Free)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedType == TaskType.BILL,
                        onClick = { selectedType = TaskType.BILL }
                    )
                    Text("Billable")
                }
            }

            // The magical Camera Button
            Button(
                onClick = {
                    // 1. Create the empty file
                    val file = context.createImageFile()
                    // 2. Get the secure FileProvider URI
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    tempImageUri = uri
                    // 3. Launch the native camera
                    cameraLauncher.launch(uri)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (capturedImageUri != null)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                if (capturedImageUri != null) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Job Card Captured!")
                } else {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Snap Job Card Picture")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.saveAdHocTask(
                        context = context,
                        title = title,
                        description = description,
                        type = selectedType,
                        imageUri = capturedImageUri,
                        onSuccess = onTaskSaved,
                        onError = { /* Show error toast */ }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        if (uploadProgress in 1..99) {
                            Text("Uploading $uploadProgress%")
                        } else {
                            Text("Saving...")
                        }
                    }
                } else {
                    Text("Save Job")
                }
            }
        }
    }
}