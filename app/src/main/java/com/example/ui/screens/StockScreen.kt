package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Product
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ErpViewModel
import com.example.util.format
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(viewModel: ErpViewModel) {
    val products by viewModel.products.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showEditProductDialog by remember { mutableStateOf<Product?>(null) }
    var selectedProductForStockAdjust by remember { mutableStateOf<Product?>(null) }

    // Form variables
    var pName by remember { mutableStateOf("") }
    var pBrand by remember { mutableStateOf("") }
    var pCategory by remember { mutableStateOf("Seeds") }
    var pStock by remember { mutableStateOf("") }
    var pUnit by remember { mutableStateOf("Packets") }
    var pPurchase by remember { mutableStateOf("") }
    var pSelling by remember { mutableStateOf("") }
    var pGst by remember { mutableStateOf("0.0") }
    var pBatch by remember { mutableStateOf("") }
    var pMfgDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var pExpDate by remember { mutableStateOf(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000) }
    var pWeight by remember { mutableStateOf("") }
    var pWeightUnit by remember { mutableStateOf("gm") }
    var pBarcode by remember { mutableStateOf("") }

    val categories = listOf("All", "Seeds", "Fertilizers", "Pesticides", "Bio-Stimulants", "Tools")
    val unitsList = listOf("Packets", "Bags", "Liters", "Bottles", "Pieces")
    val weightUnitsList = listOf("gm", "kg", "ml", "ltr")
    val gstSlabs = listOf("0.0", "5.0", "18.0")

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { prod ->
            val matchCategory = selectedCategory == "All" || prod.category.equals(selectedCategory, ignoreCase = true)
            val matchQuery = prod.name.contains(searchQuery, ignoreCase = true) || prod.barcode.contains(searchQuery, ignoreCase = true)
            matchCategory && matchQuery
        }
    }

    val today = System.currentTimeMillis()

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
                    text = viewModel.t("stock"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Total Active items: ${products.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AppButton(
                onClick = { showAddProductDialog = true },
                size = ButtonSize.SMALL
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Product", style = MaterialTheme.typography.labelLarge)
            }
        }

        // Filters search & category chips
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by keyword or barcode SKU...", fontSize = ResponsiveText.body()) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = ResponsiveText.label()) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // Listing
        if (filteredProducts.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Inventory, contentDescription = null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                    Text("No inventory items found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredProducts) { item ->
                    val isExpired = today >= item.expiryDate
                    val isNearExpiry = !isExpired && (item.expiryDate - today <= 90L * 24 * 60 * 60 * 1000)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProductForStockAdjust = item },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name, 
                                        style = MaterialTheme.typography.titleMedium, 
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.category} • Batch: ${item.batchNumber} • ${item.weightValue.format()} ${item.weightUnit}", 
                                        style = MaterialTheme.typography.bodySmall, 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    color = when {
                                        item.stock == 0.0 -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                        item.stock <= 10.0 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "${item.stock.toInt()} ${item.unit}",
                                        color = when {
                                            item.stock == 0.0 -> MaterialTheme.colorScheme.error
                                            item.stock <= 10.0 -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.primary
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                IconButton(onClick = { showEditProductDialog = item }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                
                                if (currentUserRole != "Shop Staff") {
                                    var showDeleteProductDialog by remember { mutableStateOf(false) }
                                    IconButton(onClick = { showDeleteProductDialog = true }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    }
                                    if (showDeleteProductDialog) {
                                        DeleteWithReasonDialog(
                                            title = "Delete Product?",
                                            options = listOf("Expired Product", "Duplicate Product", "Wrong Entry", "Damaged Stock", "Supplier Return", "Other"),
                                            onDismiss = { showDeleteProductDialog = false },
                                            onConfirm = { reason ->
                                                viewModel.deleteProductWithReason(item, reason)
                                                showDeleteProductDialog = false
                                            }
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    PriceIndicatorLabel("Purchase", "₹${item.purchasePrice.format()}")
                                    PriceIndicatorLabel("Retail", "₹${item.sellingPrice.format()}")
                                    PriceIndicatorLabel("Profit", "₹${item.profitPerUnit.format()}")
                                    PriceIndicatorLabel("GST", "${item.gstPercent.toInt()}%")
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val formattedExpiry = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(item.expiryDate))
                                    val statusColor = when {
                                        isExpired -> MaterialTheme.colorScheme.error
                                        isNearExpiry -> MaterialTheme.colorScheme.tertiary
                                        else -> SuccessGreen600
                                    }
                                    Icon(
                                        imageVector = if (isExpired) Icons.Default.EventBusy else if (isNearExpiry) Icons.Default.RunningWithErrors else Icons.Default.Event,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isExpired) "Expired" else if (isNearExpiry) "Near Expiry" else formattedExpiry,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Product Dialog
    if (showAddProductDialog) {
        Dialog(onDismissRequest = { showAddProductDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Add New Product", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = { showAddProductDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = pName, 
                            onValueChange = { pName = it }, 
                            label = { Text("Product Name *", fontSize = ResponsiveText.body()) }, 
                            modifier = Modifier.fillMaxWidth(), 
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        OutlinedTextField(
                            value = pBrand, 
                            onValueChange = { pBrand = it }, 
                            label = { Text("Brand Name / Company", fontSize = ResponsiveText.body()) },
                            modifier = Modifier.fillMaxWidth(), 
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        SelectableOptionsGroup(label = "Category *", options = categories.filter { it != "All" }, selected = pCategory) { pCategory = it }

                        SelectableOptionsGroup(label = "Unit *", options = unitsList, selected = pUnit) { pUnit = it }

                        OutlinedTextField(
                            value = pStock, 
                            onValueChange = { pStock = it }, 
                            label = { Text("Available Stock *", fontSize = ResponsiveText.body()) }, 
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), 
                            modifier = Modifier.fillMaxWidth(), 
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        DateInputField(label = "Manufacturing Date", timestamp = pMfgDate) { pMfgDate = it }
                        DateInputField(label = "Expiry Date", timestamp = pExpDate) { pExpDate = it }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = pWeight,
                                onValueChange = { pWeight = it },
                                label = { Text("Weight Value *", fontSize = ResponsiveText.body()) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                            AppDropdown(
                                label = "Unit *",
                                options = weightUnitsList,
                                selectedOption = pWeightUnit,
                                onOptionSelected = { pWeightUnit = it },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = pPurchase, 
                            onValueChange = { pPurchase = it }, 
                            label = { Text("Purchase Price (₹) *", fontSize = ResponsiveText.body()) }, 
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                            modifier = Modifier.fillMaxWidth(), 
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = pSelling, 
                            onValueChange = { pSelling = it }, 
                            label = { Text("Retail Price (₹) *", fontSize = ResponsiveText.body()) }, 
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                            modifier = Modifier.fillMaxWidth(), 
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        AppDropdown(
                            label = "GST (%) *",
                            options = gstSlabs,
                            selectedOption = pGst,
                            onOptionSelected = { pGst = it }
                        )

                        OutlinedTextField(
                            value = pBatch, 
                            onValueChange = { pBatch = it }, 
                            label = { Text("Batch Code", fontSize = ResponsiveText.body()) }, 
                            modifier = Modifier.fillMaxWidth(), 
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = pBarcode, 
                            onValueChange = { pBarcode = it }, 
                            label = { Text("Barcode SKU", fontSize = ResponsiveText.body()) }, 
                            modifier = Modifier.fillMaxWidth(), 
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                    }

                    AppButton(
                        onClick = {
                            val weightVal = pWeight.toDoubleOrNull() ?: 0.0
                            if (pName.isNotEmpty() && pStock.isNotEmpty() && weightVal > 0) {
                                viewModel.addProduct(
                                    pName, pBrand, pCategory, pStock.toDoubleOrNull() ?: 0.0, pUnit,
                                    pPurchase.toDoubleOrNull() ?: 0.0, pSelling.toDoubleOrNull() ?: 0.0, 
                                    pGst.toDoubleOrNull() ?: 0.0, if (pBatch.isEmpty()) "B-NEW" else pBatch, 
                                    pMfgDate, pExpDate, weightVal, pWeightUnit, pBarcode
                                )
                                showAddProductDialog = false
                                // reset
                                pName = ""; pBrand = ""; pStock = ""; pPurchase = ""; pSelling = ""; pBatch = ""; pWeight = ""; pBarcode = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        size = ButtonSize.MEDIUM
                    ) {
                        Text("Save to Stock", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Adjust Stock Dialog
    if (selectedProductForStockAdjust != null) {
        val item = selectedProductForStockAdjust!!
        var adjustAmount by remember { mutableStateOf("") }
        var isDamageWriteOff by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { selectedProductForStockAdjust = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Stock Adjustment", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Current Stock", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${item.stock.toInt()} ${item.unit}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    OutlinedTextField(
                        value = adjustAmount,
                        onValueChange = { adjustAmount = it },
                        label = { Text("Quantity", fontSize = ResponsiveText.body()) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(checked = isDamageWriteOff, onCheckedChange = { isDamageWriteOff = it })
                        Text("Deduct from stock (Damage / Spill)", style = MaterialTheme.typography.bodySmall)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppOutlinedButton(
                            onClick = { selectedProductForStockAdjust = null }, 
                            modifier = Modifier.weight(1f),
                            size = ButtonSize.SMALL
                        ) {
                            Text("Cancel")
                        }
                        AppButton(
                            onClick = {
                                val amountVal = adjustAmount.toDoubleOrNull() ?: 0.0
                                if (amountVal > 0.0) {
                                    viewModel.adjustProductStock(item.id, amountVal, increase = !isDamageWriteOff)
                                    selectedProductForStockAdjust = null
                                }
                            },
                            modifier = Modifier.weight(1.5f),
                            size = ButtonSize.SMALL,
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDamageWriteOff) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (isDamageWriteOff) "Deduct Stock" else "Add Stock")
                        }
                    }
                }
            }
        }
    }

    // Edit Product Dialog
    if (showEditProductDialog != null) {
        EditProductDialog(
            product = showEditProductDialog!!,
            viewModel = viewModel,
            onDismiss = { showEditProductDialog = null },
            onUpdate = { updatedProduct ->
                viewModel.updateProduct(updatedProduct)
                showEditProductDialog = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductDialog(
    product: Product,
    viewModel: ErpViewModel,
    onDismiss: () -> Unit,
    onUpdate: (Product) -> Unit
) {
    var eName by remember { mutableStateOf(product.name) }
    var eBrand by remember { mutableStateOf(product.brandName) }
    var eCategory by remember { mutableStateOf(product.category) }
    var eStock by remember { mutableStateOf(product.stock.toString()) }
    var eUnit by remember { mutableStateOf(product.unit) }
    var ePurchase by remember { mutableStateOf(product.purchasePrice.toString()) }
    var eSelling by remember { mutableStateOf(product.sellingPrice.toString()) }
    var eGst by remember { mutableStateOf(product.gstPercent.toString()) }
    var eBatch by remember { mutableStateOf(product.batchNumber) }
    var eMfgDate by remember { mutableStateOf(product.manufacturingDate) }
    var eExpDate by remember { mutableStateOf(product.expiryDate) }
    var eWeight by remember { mutableStateOf(product.weightValue.toString()) }
    var eWeightUnit by remember { mutableStateOf(product.weightUnit) }
    var eBarcode by remember { mutableStateOf(product.barcode) }

    val categories = listOf("Seeds", "Fertilizers", "Pesticides", "Bio-Stimulants", "Tools")
    val unitsList = listOf("Bags", "Bottles", "Pieces")
    val weightUnitsList = listOf("gm", "kg", "ml", "ltr")
    val gstSlabs = listOf("0.0", "5.0", "18.0")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Edit Product", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = eName, 
                        onValueChange = { eName = it }, 
                        label = { Text("Product Name *", fontSize = ResponsiveText.body()) }, 
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = eBrand, 
                        onValueChange = { eBrand = it }, 
                        label = { Text("Brand Name / Company", fontSize = ResponsiveText.body()) },
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    SelectableOptionsGroup(label = "Category *", options = categories, selected = eCategory) { eCategory = it }

                    SelectableOptionsGroup(label = "Unit *", options = unitsList, selected = eUnit) { eUnit = it }

                    OutlinedTextField(
                        value = eStock, 
                        onValueChange = { eStock = it }, 
                        label = { Text("Available Stock", fontSize = ResponsiveText.body()) }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), 
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    DateInputField(label = "Manufacturing Date", timestamp = eMfgDate) { eMfgDate = it }
                    DateInputField(label = "Expiry Date", timestamp = eExpDate) { eExpDate = it }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = eWeight,
                            onValueChange = { eWeight = it },
                            label = { Text("Weight Value", fontSize = ResponsiveText.body()) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        AppDropdown(
                            label = "Unit",
                            options = weightUnitsList,
                            selectedOption = eWeightUnit,
                            onOptionSelected = { eWeightUnit = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = ePurchase, 
                        onValueChange = { ePurchase = it }, 
                        label = { Text("Purchase Price (₹) *", fontSize = ResponsiveText.body()) }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = eSelling, 
                        onValueChange = { eSelling = it }, 
                        label = { Text("Retail Price (₹) *", fontSize = ResponsiveText.body()) }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    AppDropdown(
                        label = "GST (%)",
                        options = gstSlabs,
                        selectedOption = eGst,
                        onOptionSelected = { eGst = it }
                    )

                    OutlinedTextField(
                        value = eBatch, 
                        onValueChange = { eBatch = it }, 
                        label = { Text("Batch Code", fontSize = ResponsiveText.body()) }, 
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = eBarcode, 
                        onValueChange = { eBarcode = it }, 
                        label = { Text("Barcode SKU", fontSize = ResponsiveText.body()) }, 
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }

                AppButton(
                    onClick = {
                        val weightVal = eWeight.toDoubleOrNull() ?: 0.0
                        if (eName.isNotEmpty() && eStock.isNotEmpty() && weightVal > 0) {
                            onUpdate(product.copy(
                                name = eName,
                                brandName = eBrand,
                                category = eCategory,
                                stock = eStock.toDoubleOrNull() ?: product.stock,
                                unit = eUnit,
                                purchasePrice = ePurchase.toDoubleOrNull() ?: product.purchasePrice,
                                sellingPrice = eSelling.toDoubleOrNull() ?: product.sellingPrice,
                                gstPercent = eGst.toDoubleOrNull() ?: product.gstPercent,
                                batchNumber = eBatch,
                                manufacturingDate = eMfgDate,
                                expiryDate = eExpDate,
                                weightValue = weightVal,
                                weightUnit = eWeightUnit,
                                barcode = eBarcode
                            ))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    size = ButtonSize.MEDIUM
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Update Product", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
