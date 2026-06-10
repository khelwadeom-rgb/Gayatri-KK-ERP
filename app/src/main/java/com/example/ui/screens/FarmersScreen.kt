package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CreditRecord
import com.example.data.model.Farmer
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ErpViewModel
import com.example.util.BillShareManager
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FarmersScreen(viewModel: ErpViewModel) {
    val farmers by viewModel.farmers.collectAsState()
    val creditRecords by viewModel.creditRecords.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFarmerDetails by remember { mutableStateOf<Farmer?>(null) }
    var showAddFarmerDialog by remember { mutableStateOf(false) }
    var showEditFarmerDialog by remember { mutableStateOf<Farmer?>(null) }
    var showLedgerFor by remember { mutableStateOf<Farmer?>(null) }

    // Farmer Form Fields
    var fName by remember { mutableStateOf("") }
    var fMobile by remember { mutableStateOf("") }
    var fVillage by remember { mutableStateOf("") }
    var fCrop by remember { mutableStateOf("Cotton") }
    var fSoil by remember { mutableStateOf("Black Soil") }
    var fIrrigation by remember { mutableStateOf("Drip") }
    var fNotes by remember { mutableStateOf("") }

    val cropsList = listOf("Cotton", "Maize", "Banana","Soybean", "Wheat", "Vegetables")
    val soilsList = listOf("Black Soil", "Red Soil", "Sandy")
    val irrigationList = listOf("Drip", "Rain-fed")

    val filteredFarmers = remember(farmers, searchQuery) {
        farmers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.mobile.contains(searchQuery) ||
            it.village.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = viewModel.t("farmers"), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(text = "Total Registered: ${farmers.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AppButton(onClick = { showAddFarmerDialog = true }, size = ButtonSize.SMALL) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Farmer")
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter farmers directory...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredFarmers) { farmer ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { showLedgerFor = farmer },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = farmer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = "Village: ${farmer.village} • ${farmer.mobile}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "₹${farmer.outstandingCredit.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = if (farmer.outstandingCredit > 0) MaterialTheme.colorScheme.error else SuccessGreen600)
                                Row {
                                    IconButton(onClick = { showEditFarmerDialog = farmer }) { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                    IconButton(onClick = { 
                                        // Use common delete dialog
                                    }) { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddFarmerDialog) {
        Dialog(onDismissRequest = { showAddFarmerDialog = false }) {
            Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
                Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Add New Farmer Profile", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(value = fName, onValueChange = { fName = it }, label = { Text("Farmer Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = fMobile, onValueChange = { fMobile = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = fVillage, onValueChange = { fVillage = it }, label = { Text("Village") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        viewModel.addFarmer(fName, fMobile, fVillage, "", 0.0, fCrop, fSoil, fIrrigation, fNotes)
                        showAddFarmerDialog = false
                    }, modifier = Modifier.fillMaxWidth()) { Text("Save Farmer") }
                }
            }
        }
    }

    if (showEditFarmerDialog != null) {
        EditFarmerDialog(farmer = showEditFarmerDialog!!, viewModel = viewModel, onDismiss = { showEditFarmerDialog = null }, onUpdate = { viewModel.updateFarmer(it); showEditFarmerDialog = null })
    }

    if (showLedgerFor != null) {
        FarmerLedgerModal(farmer = showLedgerFor!!, viewModel = viewModel, onDismiss = { showLedgerFor = null })
    }
}

@Composable
fun FarmerLedgerModal(farmer: Farmer, viewModel: ErpViewModel, onDismiss: () -> Unit) {
    val invoices by viewModel.invoices.collectAsState()
    val creditRecords by viewModel.creditRecords.collectAsState()
    val allItems by viewModel.allInvoiceItems.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val context = LocalContext.current

    val ledgerEntries = remember(invoices, creditRecords, farmer) {
        val list = mutableListOf<LedgerEntry>()
        invoices.filter { it.farmerId == farmer.id }.forEach {
            list.add(LedgerEntry(it.createdAt, "Purchase", it.totalAmount, "Invoice #${it.id}", it.id))
        }
        creditRecords.filter { it.farmerId == farmer.id && it.amount < 0 }.forEach {
            list.add(LedgerEntry(it.timestamp, "Payment", Math.abs(it.amount), "via ${it.paymentMode}", it.id))
        }
        list.sortByDescending { it.timestamp }
        list
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Farmer Ledger", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
                }
                Text(text = farmer.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ledgerEntries) { entry ->
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(entry.timestamp)), style = MaterialTheme.typography.labelSmall)
                                    Text(entry.type, fontWeight = FontWeight.Bold, color = if (entry.type == "Purchase") Color.Red else SuccessGreen600)
                                    Text(entry.description, style = MaterialTheme.typography.bodySmall)
                                }
                                Text("₹${entry.amount.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                                if (entry.type == "Purchase") {
                                    IconButton(onClick = {
                                        val invoice = invoices.find { it.id == entry.id }
                                        val items = allItems.filter { it.invoiceId == entry.id }
                                        if (invoice != null && shopProfile != null) {
                                            val file = BillShareManager.generateInvoicePdf(context, invoice, items, shopProfile!!)
                                            file?.let { BillShareManager.viewPdf(context, it) }
                                        }
                                    }) { Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class LedgerEntry(val timestamp: Long, val type: String, val amount: Double, val description: String, val id: Long)

@Composable
fun EditFarmerDialog(farmer: Farmer, viewModel: ErpViewModel, onDismiss: () -> Unit, onUpdate: (Farmer) -> Unit) {
    var eName by remember { mutableStateOf(farmer.name) }
    var eMobile by remember { mutableStateOf(farmer.mobile) }
    var eVillage by remember { mutableStateOf(farmer.village) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Farmer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = eName, onValueChange = { eName = it }, label = { Text("Name") })
                OutlinedTextField(value = eMobile, onValueChange = { eMobile = it }, label = { Text("Mobile") })
                OutlinedTextField(value = eVillage, onValueChange = { eVillage = it }, label = { Text("Village") })
            }
        },
        confirmButton = { Button(onClick = { onUpdate(farmer.copy(name = eName, mobile = eMobile, village = eVillage)) }) { Text("Update") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
