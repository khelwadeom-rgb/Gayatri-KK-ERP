package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ErpViewModel
import com.example.util.format
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(viewModel: ErpViewModel) {
    val purchases by viewModel.purchaseInvoices.collectAsState()
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var selectedPurchaseId by remember { mutableStateOf<Long?>(null) }
    var purchaseToEdit by remember { mutableStateOf<Pair<PurchaseInvoice, List<PurchaseItem>>?>(null) }

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
                    text = viewModel.t("purchases"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Record incoming stock inventory",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AppButton(onClick = { 
                purchaseToEdit = null
                showPurchaseDialog = true 
            }, size = ButtonSize.SMALL) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Purchase")
            }
        }

        if (purchases.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No purchase records found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(purchases) { purchase ->
                    PurchaseInvoiceCard(
                        invoice = purchase,
                        onClick = { selectedPurchaseId = purchase.id }
                    )
                }
            }
        }
    }

    if (showPurchaseDialog) {
        PurchaseDialog(
            viewModel = viewModel,
            initialInvoice = purchaseToEdit?.first,
            initialItems = purchaseToEdit?.second ?: emptyList(),
            onDismiss = { 
                showPurchaseDialog = false
                purchaseToEdit = null
            }
        )
    }

    if (selectedPurchaseId != null) {
        PurchaseDetailsDialog(
            purchaseId = selectedPurchaseId!!,
            viewModel = viewModel,
            onEdit = { invoice, items ->
                purchaseToEdit = Pair(invoice, items)
                selectedPurchaseId = null
                showPurchaseDialog = true
            },
            onDismiss = { selectedPurchaseId = null }
        )
    }
}

@Composable
fun PurchaseInvoiceCard(invoice: PurchaseInvoice, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
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
                Text(text = "Inv #${invoice.invoiceNumber}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = invoice.supplierName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Text(text = "Date: ${invoice.invoiceDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "₹${invoice.totalAmount.format()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    color = when(invoice.paymentStatus) {
                        "Paid" -> SuccessGreen600.copy(alpha = 0.1f)
                        "Partially Paid" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = invoice.paymentStatus,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when(invoice.paymentStatus) {
                            "Paid" -> SuccessGreen600
                            "Partially Paid" -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDialog(
    viewModel: ErpViewModel, 
    initialInvoice: PurchaseInvoice? = null,
    initialItems: List<PurchaseItem> = emptyList(),
    onDismiss: () -> Unit
) {
    val suppliers by viewModel.suppliers.collectAsState()
    
    var selectedSupplier by remember { mutableStateOf(suppliers.find { it.id == initialInvoice?.supplierId }) }
    var invoiceNumber by remember { mutableStateOf(initialInvoice?.invoiceNumber ?: "") }
    var invoiceDate by remember { 
        val date = initialInvoice?.invoiceDate
        val timestamp = if (date != null) {
            try { SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(date)?.time ?: System.currentTimeMillis() }
            catch (e: Exception) { System.currentTimeMillis() }
        } else System.currentTimeMillis()
        mutableStateOf(timestamp)
    }
    var remarks by remember { mutableStateOf(initialInvoice?.remarks ?: "") }
    var paidAmount by remember { mutableStateOf(initialInvoice?.paidAmount?.toString() ?: "") }
    var paymentStatus by remember { mutableStateOf(initialInvoice?.paymentStatus ?: "Unpaid") }

    val items = remember { mutableStateListOf<PurchaseItem>().apply { addAll(initialItems) } }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.95f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (initialInvoice == null) "Record Purchase Bill" else "Edit Purchase Bill", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
                }

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppDropdown(
                                label = "Select Supplier *",
                                options = suppliers.map { it.name },
                                selectedOption = selectedSupplier?.name ?: "Select Supplier",
                                onOptionSelected = { name -> selectedSupplier = suppliers.find { it.name == name } }
                            )
                            OutlinedTextField(value = invoiceNumber, onValueChange = { invoiceNumber = it }, label = { Text("Invoice Number *") }, modifier = Modifier.fillMaxWidth())
                            DateInputField(label = "Invoice Date", timestamp = invoiceDate, onDateSelected = { invoiceDate = it })
                        }
                    }

                    item {
                        Text("Products in Bill", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    itemsIndexed(items) { index, item ->
                        PurchaseItemEntry(
                            item = item, 
                            onUpdate = { updated -> items[index] = updated },
                            onDelete = { items.removeAt(index) }
                        )
                    }

                    item {
                        AppOutlinedButton(onClick = { 
                            items.add(PurchaseItem(purchaseInvoiceId = initialInvoice?.id ?: 0, productId = 0, productName = "", brandName = "", category = "Seeds", batchNumber = "", quantity = 1.0, unit = "Packets", purchaseRate = 0.0, total = 0.0))
                        }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Product to Bill")
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val total = items.sumOf { it.total }
                            Text("Total Bill Amount: ₹${total.format()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            
                            OutlinedTextField(value = paidAmount, onValueChange = { paidAmount = it }, label = { Text("Amount Paid Now") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            
                            SelectableOptionsGroup(
                                label = "Payment Status",
                                options = listOf("Paid", "Partially Paid", "Unpaid"),
                                selected = paymentStatus,
                                onSelection = { paymentStatus = it }
                            )
                            
                            OutlinedTextField(value = remarks, onValueChange = { remarks = it }, label = { Text("Remarks") }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                Button(
                    onClick = {
                        val total = items.sumOf { it.total }
                        val paid = paidAmount.toDoubleOrNull() ?: 0.0
                        val finalStatus = if (paid >= total && total > 0) "Paid" else if (paid > 0) "Partially Paid" else paymentStatus
                        
                        val invoice = PurchaseInvoice(
                            id = initialInvoice?.id ?: 0,
                            supplierId = selectedSupplier?.id ?: 0,
                            supplierName = selectedSupplier?.name ?: "Unknown",
                            invoiceNumber = invoiceNumber,
                            invoiceDate = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(invoiceDate)),
                            totalAmount = total,
                            paidAmount = paid,
                            paymentStatus = finalStatus,
                            remarks = remarks,
                            createdAt = initialInvoice?.createdAt ?: System.currentTimeMillis(),
                            createdBy = initialInvoice?.createdBy ?: "Super Admin",
                            updatedAt = System.currentTimeMillis(),
                            updatedBy = "Super Admin",
                            lastModified = System.currentTimeMillis()
                        )

                        if (initialInvoice == null) {
                            viewModel.addPurchaseInvoice(invoice, items)
                        } else {
                            viewModel.updatePurchaseInvoice(invoice, items)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedSupplier != null && 
                              invoiceNumber.isNotEmpty() && 
                              items.isNotEmpty() && 
                              items.all { it.productName.isNotEmpty() && it.quantity > 0 && it.purchaseRate > 0 }
                ) {
                    Text(if (initialInvoice == null) "Save Purchase & Update Stock" else "Update Purchase & Adjust Stock")
                }
            }
        }
    }
}

@Composable
fun PurchaseItemEntry(item: PurchaseItem, onUpdate: (PurchaseItem) -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = item.productName, 
                    onValueChange = { onUpdate(item.copy(productName = it)) }, 
                    label = { Text("Product Name") }, 
                    modifier = Modifier.weight(1.5f)
                )
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = item.brandName, onValueChange = { onUpdate(item.copy(brandName = it)) }, label = { Text("Brand") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = item.batchNumber, onValueChange = { onUpdate(item.copy(batchNumber = it)) }, label = { Text("Batch") }, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateInputField(label = "MFG", timestamp = item.manufacturingDate, onDateSelected = { onUpdate(item.copy(manufacturingDate = it)) }, modifier = Modifier.weight(1f))
                DateInputField(label = "EXP", timestamp = item.expiryDate, onDateSelected = { onUpdate(item.copy(expiryDate = it)) }, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = item.weightValue.toString(), onValueChange = { onUpdate(item.copy(weightValue = it.toDoubleOrNull() ?: 0.0)) }, label = { Text("Weight") }, modifier = Modifier.weight(0.7f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                AppDropdown(
                    label = "W.Unit", 
                    options = listOf("gm", "kg", "ml", "ltr"), 
                    selectedOption = item.weightUnit, 
                    onOptionSelected = { onUpdate(item.copy(weightUnit = it)) },
                    modifier = Modifier.weight(0.7f)
                )
                AppDropdown(
                    label = "GST %", 
                    options = listOf("0%", "5%", "12%", "18%", "28%"), 
                    selectedOption = "${item.gstPercent.toInt()}%", 
                    onOptionSelected = { onUpdate(item.copy(gstPercent = it.replace("%", "").toDoubleOrNull() ?: 18.0)) },
                    modifier = Modifier.weight(0.7f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = item.quantity.toString(), onValueChange = { 
                    val q = it.toDoubleOrNull() ?: 0.0
                    onUpdate(item.copy(quantity = q, total = q * item.purchaseRate)) 
                }, label = { Text("Qty") }, modifier = Modifier.weight(0.8f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                
                AppDropdown(
                    label = "Unit", 
                    options = listOf("Packets", "Bags", "Liters", "Bottles", "Pieces"), 
                    selectedOption = item.unit, 
                    onOptionSelected = { onUpdate(item.copy(unit = it)) },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(value = item.purchaseRate.toString(), onValueChange = { 
                    val r = it.toDoubleOrNull() ?: 0.0
                    onUpdate(item.copy(purchaseRate = r, total = item.quantity * r))
                }, label = { Text("Rate") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Text(text = "Item Total: ₹${item.total.format()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun PurchaseDetailsDialog(
    purchaseId: Long, 
    viewModel: ErpViewModel, 
    onEdit: (PurchaseInvoice, List<PurchaseItem>) -> Unit,
    onDismiss: () -> Unit
) {
    val purchases by viewModel.purchaseInvoices.collectAsState()
    val purchase = remember(purchases, purchaseId) { purchases.find { it.id == purchaseId } }
    
    val itemsFlow = remember(purchaseId) { viewModel.getPurchaseItems(purchaseId) }
    val items by itemsFlow.collectAsState(initial = emptyList())

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteReason by remember { mutableStateOf("") }

    if (purchase != null) {
        Dialog(onDismissRequest = onDismiss) {
            Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Purchase Details", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                            Text("Inv #${purchase.invoiceNumber} • ${purchase.invoiceDate}", style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
                    }

                    Text(text = "Supplier: ${purchase.supplierName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    HorizontalDivider()

                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(items) { item ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.productName, fontWeight = FontWeight.Bold)
                                    Text(text = "${item.brandName} • Batch: ${item.batchNumber}", style = MaterialTheme.typography.bodySmall)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "₹${item.total.format()}", fontWeight = FontWeight.Bold)
                                    Text(text = "${item.quantity} ${item.unit} @ ₹${item.purchaseRate}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp).alpha(0.1f))
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Grand Total", fontWeight = FontWeight.Bold)
                            Text("₹${purchase.totalAmount.format()}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete")
                        }
                        Button(
                            onClick = { onEdit(purchase, items) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Purchase Record") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Are you sure you want to delete this purchase? This will reduce the current stock inventory.")
                    OutlinedTextField(
                        value = deleteReason,
                        onValueChange = { deleteReason = it },
                        label = { Text("Reason for deletion *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePurchaseInvoiceWithReason(purchaseId, deleteReason)
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    enabled = deleteReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
