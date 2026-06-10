package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Supplier
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ErpViewModel
import com.example.util.format
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*
import com.example.util.BillShareManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierScreen(viewModel: ErpViewModel) {
    val suppliers by viewModel.suppliers.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<Supplier?>(null) }
    var showLedgerFor by remember { mutableStateOf<Supplier?>(null) }

    val filteredSuppliers = remember(suppliers, searchQuery) {
        suppliers.filter { it.name.contains(searchQuery, ignoreCase = true) || it.mobile.contains(searchQuery) }
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
                    text = viewModel.t("suppliers"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Manage product sources",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AppButton(onClick = { showAddDialog = true }, size = ButtonSize.SMALL) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Supplier")
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name or mobile...", fontSize = ResponsiveText.body()) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        if (filteredSuppliers.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No suppliers found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredSuppliers) { supplier ->
                    SupplierCard(
                        supplier = supplier,
                        onEdit = { editingSupplier = supplier },
                        onDelete = { viewModel.deleteSupplier(supplier) },
                        onViewLedger = { showLedgerFor = supplier }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        SupplierFormDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, mob, addr, gst, contact, email ->
                viewModel.addSupplier(name, mob, addr, gst, contact, email)
                showAddDialog = false
            }
        )
    }

    if (editingSupplier != null) {
        SupplierFormDialog(
            supplier = editingSupplier,
            onDismiss = { editingSupplier = null },
            onSave = { name, mob, addr, gst, contact, email ->
                viewModel.updateSupplier(editingSupplier!!.copy(
                    name = name, mobile = mob, address = addr,
                    gstNumber = gst, contactPerson = contact, email = email
                ))
                editingSupplier = null
            }
        )
    }

    if (showLedgerFor != null) {
        SupplierLedgerModal(
            supplier = showLedgerFor!!,
            viewModel = viewModel,
            onDismiss = { showLedgerFor = null }
        )
    }
}

@Composable
fun SupplierCard(supplier: Supplier, onEdit: () -> Unit, onDelete: () -> Unit, onViewLedger: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onViewLedger),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = supplier.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "Mob: ${supplier.mobile} • ${supplier.address}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (supplier.gstNumber.isNotEmpty()) {
                    Text(text = "GST: ${supplier.gstNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "₹${supplier.outstandingBalance.format()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (supplier.outstandingBalance > 0) MaterialTheme.colorScheme.error else SuccessGreen600)
                    Text(text = "Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun SupplierFormDialog(
    supplier: Supplier? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var mobile by remember { mutableStateOf(supplier?.mobile ?: "") }
    var address by remember { mutableStateOf(supplier?.address ?: "") }
    var gst by remember { mutableStateOf(supplier?.gstNumber ?: "") }
    var contact by remember { mutableStateOf(supplier?.contactPerson ?: "") }
    var email by remember { mutableStateOf(supplier?.email ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = if (supplier == null) "Add Supplier" else "Edit Supplier", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Supplier Name *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number *") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = gst, onValueChange = { gst = it }, label = { Text("GST Number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contact Person") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))

                Spacer(modifier = Modifier.weight(1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { if (name.isNotEmpty() && mobile.isNotEmpty()) onSave(name, mobile, address, gst, contact, email) },
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotEmpty() && mobile.isNotEmpty()
                    ) {
                        Text("Save Supplier")
                    }
                }
            }
        }
    }
}

@Composable
fun SupplierLedgerModal(supplier: Supplier, viewModel: ErpViewModel, onDismiss: () -> Unit) {
    val purchases by viewModel.purchaseInvoices.collectAsState()
    val allItems by viewModel.allPurchaseItems.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val context = LocalContext.current

    val ledgerEntries = remember(purchases, supplier) {
        purchases.filter { it.supplierId == supplier.id }
            .map { LedgerEntry(it.createdAt, "Purchase", it.totalAmount, "Invoice #${it.invoiceNumber}", it.id) }
            .sortedByDescending { it.timestamp }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Supplier Ledger", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
                }
                Text(text = supplier.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ledgerEntries) { entry ->
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(entry.timestamp)), style = MaterialTheme.typography.labelSmall)
                                    Text(entry.type, fontWeight = FontWeight.Bold, color = Color.Red)
                                    Text(entry.description, style = MaterialTheme.typography.bodySmall)
                                }
                                Text("₹${entry.amount.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                                IconButton(onClick = {
                                    val invoice = purchases.find { it.id == entry.id }
                                    val items = allItems.filter { it.purchaseInvoiceId == entry.id }
                                    if (invoice != null && shopProfile != null) {
                                        val file = BillShareManager.generatePurchaseInvoicePdf(context, invoice, items, shopProfile!!)
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
