package com.sharmarefrigeration.workledger.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharmarefrigeration.workledger.model.User

@Composable
fun AdminDirectoryScreen(
    viewModel: AdminViewModel,
    onTechnicianClick: (String) -> Unit,
    onAccountantClick: (String) -> Unit
) {
    val technicians by viewModel.technicians.collectAsStateWithLifecycle()
    val accountants by viewModel.accountants.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Team Directory", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        // --- SECTION 1: ACCOUNTANTS ---
        item {
            Text("Accountants", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }

        if (accountants.isEmpty()) {
            item { Text("No accountants registered.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(accountants, key = { it.id }) { user ->
                UserDirectoryCard(
                    user = user,
                    isAccountant = true,
                    // Use the accountant callback!
                    onClick = { onAccountantClick(user.id) }
                )
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        // --- SECTION 2: FIELD TECHNICIANS ---
        item {
            Text("Field Technicians", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }

        if (technicians.isEmpty()) {
            item { Text("No technicians registered.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(technicians, key = { it.id }) { user ->
                UserDirectoryCard(
                    user = user,
                    isAccountant = false,
                    // Use the technician callback!
                    onClick = { onTechnicianClick(user.id) }
                )
            }
        }
    }
}

@Composable
fun UserDirectoryCard(user: User, isAccountant: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (isAccountant) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    val icon = if (isAccountant) Icons.Default.Calculate else Icons.Default.Person
                    val tint = if (isAccountant) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(12.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(user.phoneNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "View Ledger", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}