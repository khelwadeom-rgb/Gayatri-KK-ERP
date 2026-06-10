package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CreditRecord
import com.example.data.model.Farmer
import com.example.ui.components.AppButton
import com.example.ui.components.ButtonSize
import com.example.ui.theme.*
import com.example.ui.viewmodel.ErpViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UdhariScreen(viewModel: ErpViewModel) {
    val farmers by viewModel.farmers.collectAsState()
    val allLedgers by viewModel.creditRecords.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFarmerForPayment by remember { mutableStateOf<Farmer?>(null) }
    var showHistory by remember { mutableStateOf(false) }

    val farmersWithCredit = remember(farmers, searchQuery) {
        farmers.filter {
            it.outstandingCredit > 0.0 &&
            (it.name.contains(searchQuery, ignoreCase = true) || it.village.contains(searchQuery, ignoreCase = true))
        }.sortedByDescending { it.outstandingCredit }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = viewModel.t("udhari"),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = ResponsiveText.heading()),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val aggregateOwed = farmers.sumOf { it.outstandingCredit }
                Text(
                    text = "Total Outstanding: ₹${aggregateOwed.toInt()}",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = ResponsiveText.label()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AppButton(
                onClick = { showHistory = true },
                size = ButtonSize.SMALL,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("History", style = MaterialTheme.typography.labelLarge.copy(fontSize = ResponsiveText.body()))
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by farmer name or village...") },
            leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        // Debtors List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Outstanding Udhari List", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

            if (farmersWithCredit.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen600, modifier = Modifier.size(48.dp))
                        Text("All farmer accounts are clean", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(farmersWithCredit) { farmer ->
                        val riskColor = when {
                            farmer.outstandingCredit <= 2000.0 -> SuccessGreen600
                            farmer.outstandingCredit <= 5000.0 -> SoftOrange500
                            else -> MaterialTheme.colorScheme.error
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFarmerForPayment = farmer },
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = farmer.name, 
                                        style = MaterialTheme.typography.titleSmall.copy(fontSize = ResponsiveText.title()),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Village: ${farmer.village}", 
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = ResponsiveText.label()), 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Surface(
                                        modifier = Modifier.padding(top = 6.dp),
                                        color = riskColor.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (farmer.outstandingCredit <= 2000.0) "Healthy" else if (farmer.outstandingCredit <= 5000.0) "Reminder Due" else "High Risk",
                                            color = riskColor,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveText.label()),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "₹${farmer.outstandingCredit.toInt()}", 
                                        style = MaterialTheme.typography.titleLarge.copy(fontSize = ResponsiveText.heading()), 
                                        fontWeight = FontWeight.Black, 
                                        color = riskColor
                                    )
                                    Surface(
                                        modifier = Modifier.clip(RoundedCornerShape(10.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(
                                            "Collect", 
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveText.label()), 
                                            fontWeight = FontWeight.Bold, 
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Recovery History Modal
    if (showHistory) {
        RecoveryHistoryModal(viewModel = viewModel, onDismiss = { showHistory = false })
    }

    // Payment Dialog
    if (selectedFarmerForPayment != null) {
        val farmer = selectedFarmerForPayment!!
        var collectAmount by remember { mutableStateOf("") }
        var collectMode by remember { mutableStateOf("Cash") }
        var collectNotes by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { selectedFarmerForPayment = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("Record Dues Payment", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(farmer.name, style = MaterialTheme.typography.titleSmall)
                        Text("Village: ${farmer.village}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "TOTAL DUE: ₹${farmer.outstandingCredit.toInt()}",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = collectAmount,
                        onValueChange = { collectAmount = it },
                        label = { Text("Collection Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Cash", "UPI", "Bank").forEach { md ->
                            val isSel = collectMode == md
                            Surface(
                                modifier = Modifier.weight(1f).clickable { collectMode = md },
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(10.dp),
                                border = if (isSel) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            ) {
                                Text(
                                    text = md,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = collectNotes,
                        onValueChange = { collectNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { selectedFarmerForPayment = null }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val repayAmount = collectAmount.toDoubleOrNull() ?: 0.0
                                if (repayAmount > 0.0) {
                                    viewModel.payFarmerCredit(farmer.id, farmer.name, repayAmount, collectMode, if (collectNotes.isEmpty()) "Cash recovery" else collectNotes)
                                    selectedFarmerForPayment = null
                                }
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen600)
                        ) {
                            Text("Record Payment", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecoveryHistoryModal(viewModel: ErpViewModel, onDismiss: () -> Unit) {
    val farmers by viewModel.farmers.collectAsState()
    val allLedgers by viewModel.creditRecords.collectAsState()
    var historySearch by remember { mutableStateOf("") }

    val recoveryHistory = remember(allLedgers, historySearch) {
        allLedgers.filter { it.amount < 0.0 && it.farmerName.contains(historySearch, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Recovery Payment History", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = historySearch,
                    onValueChange = { historySearch = it },
                    placeholder = { Text("Search history by farmer name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (recoveryHistory.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No recovery history found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recoveryHistory) { record ->
                            val farmer = farmers.find { it.id == record.farmerId }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = record.farmerName, 
                                                style = MaterialTheme.typography.titleSmall.copy(fontSize = ResponsiveText.title()), 
                                                fontWeight = FontWeight.Bold
                                            )
                                            val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(record.timestamp))
                                            Text(
                                                text = date, 
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveText.label()), 
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "₹${Math.abs(record.amount).toInt()}", 
                                                style = MaterialTheme.typography.titleMedium.copy(fontSize = ResponsiveText.title()), 
                                                fontWeight = FontWeight.Black, 
                                                color = SuccessGreen600
                                            )
                                            Text(
                                                text = "via ${record.paymentMode}", 
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveText.label()), 
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = record.description, 
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = ResponsiveText.label()), 
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "Due: ₹${farmer?.outstandingCredit?.toInt() ?: 0}", 
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveText.label()), 
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
