package com.example.ui.screens

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Farmer
import com.example.data.model.Product
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ErpViewModel
import com.example.util.BillPrintManager
import com.example.util.BillShareManager
import com.example.util.format
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(viewModel: ErpViewModel) {
    val products by viewModel.products.collectAsState()
    val basket by viewModel.basket.collectAsState()
    val farmers by viewModel.farmers.collectAsState()
    val selectedFarmer by viewModel.selectedFarmerForBilling.collectAsState()

    val subtotal by viewModel.basketSubtotal.collectAsState()
    val totalGst by viewModel.basketGstAmount.collectAsState()
    val totalAmount by viewModel.basketTotalAmount.collectAsState()
    val posPaymentMode by viewModel.posPaymentMode.collectAsState()
    val posDiscount by viewModel.posDiscount.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showFarmerSelectDialog by remember { mutableStateOf(false) }
    var showReceiptInvoiceId by remember { mutableStateOf<Long?>(null) }
    var paidAmountEntered by remember { mutableStateOf("") }

    val categories = listOf("All", "Seeds", "Fertilizers", "Pesticides", "Bio-Stimulants", "Tools")

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { prod ->
            val matchCategory = selectedCategory == "All" || prod.category.equals(selectedCategory, ignoreCase = true)
            val matchQuery = prod.name.contains(searchQuery, ignoreCase = true) || prod.barcode.contains(searchQuery, ignoreCase = true)
            matchCategory && matchQuery
        }
    }

    val isWideScreen = LocalConfiguration.current.screenWidthDp > 720

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(modifier = Modifier.fillMaxSize()) {
            // LEFT PANEL: Product selector
            Column(
                modifier = Modifier
                    .weight(if (isWideScreen) 1.2f else 1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val shopProfile by viewModel.shopProfile.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppLogo(size = 48, logoPath = shopProfile?.logoPath)
                        Text(
                            text = viewModel.t("billing"),
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = ResponsiveText.heading()),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Connected customer badge
                Surface(
                    color = if (selectedFarmer != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable { showFarmerSelectDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedFarmer != null) Icons.Default.Person else Icons.Default.PersonAdd,
                            contentDescription = "Customer",
                            tint = if (selectedFarmer != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = selectedFarmer?.name ?: "Walk-In Buyer",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = ResponsiveText.body()
                            ),
                            color = if (selectedFarmer != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search products...", fontSize = ResponsiveText.body()) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = ResponsiveText.body()),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                // Category Chips
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
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                selected = isSelected,
                                enabled = true,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Grid of Products
                if (filteredProducts.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Inventory2, contentDescription = "Empty", tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                            Text("No stock items found", style = MaterialTheme.typography.bodyLarge.copy(fontSize = ResponsiveText.body()), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(160.dp),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredProducts) { item ->
                            ProductBillingGridItem(item = item, quantityInBasket = basket[item] ?: 0.0) {
                                viewModel.addProductToBasket(item)
                            }
                        }
                    }
                }
            }

            // RIGHT PANEL: Cart Checkout Drawer
            if (isWideScreen) {
                Surface(
                    modifier = Modifier
                        .width(380.dp)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                    tonalElevation = 1.dp
                ) {
                    CartCheckoutColumn(
                        basket = basket,
                        selectedFarmer = selectedFarmer,
                        subtotal = subtotal,
                        totalGst = totalGst,
                        totalAmount = totalAmount,
                        posPaymentMode = posPaymentMode,
                        posDiscount = posDiscount,
                        paidAmountEntered = paidAmountEntered,
                        onPaidAmountChange = { paidAmountEntered = it },
                        viewModel = viewModel,
                        onFarmerClick = { showFarmerSelectDialog = true },
                        onCheckoutSuccess = { id ->
                            showReceiptInvoiceId = id
                            paidAmountEntered = ""
                        }
                    )
                }
            }
        }

        // Floating basket trigger for smaller mobile screens
        if (!isWideScreen && basket.isNotEmpty()) {
            var showMobileCartSheet by remember { mutableStateOf(false) }
            ExtendedFloatingActionButton(
                onClick = { showMobileCartSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Cart") },
                text = { Text("Cart (${basket.values.sum().toInt()}) • ₹${totalAmount.format()}") }
            )

            if (showMobileCartSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showMobileCartSheet = false },
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Box(modifier = Modifier.fillMaxHeight(0.9f)) {
                        CartCheckoutColumn(
                            basket = basket,
                            selectedFarmer = selectedFarmer,
                            subtotal = subtotal,
                            totalGst = totalGst,
                            totalAmount = totalAmount,
                            posPaymentMode = posPaymentMode,
                            posDiscount = posDiscount,
                            paidAmountEntered = paidAmountEntered,
                            onPaidAmountChange = { paidAmountEntered = it },
                            viewModel = viewModel,
                            onFarmerClick = { showFarmerSelectDialog = true },
                            onCheckoutSuccess = { id ->
                                showReceiptInvoiceId = id
                                showMobileCartSheet = false
                                paidAmountEntered = ""
                            }
                        )
                    }
                }
            }
        }

        // Registered Farmer selection Dialog
        if (showFarmerSelectDialog) {
            Dialog(onDismissRequest = { showFarmerSelectDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.8f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Link Farmer Ledger Account", style = MaterialTheme.typography.titleLarge.copy(fontSize = ResponsiveText.heading()))
                            IconButton(onClick = { showFarmerSelectDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        var customerSearch by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = customerSearch,
                            onValueChange = { customerSearch = it },
                            placeholder = { Text("Search farmer...", fontSize = ResponsiveText.body()) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = ResponsiveText.body()),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                        )

                        val matchingFarmers = remember(farmers, customerSearch) {
                            farmers.filter {
                                it.name.contains(customerSearch, ignoreCase = true) || it.mobile.contains(customerSearch)
                            }
                        }

                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setFarmerForBilling(null)
                                            showFarmerSelectDialog = false
                                        },
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PersonOutline, contentDescription = "Walk-In", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text("Standard Walk-In Farmer", style = MaterialTheme.typography.titleSmall.copy(fontSize = ResponsiveText.title()))
                                    }
                                }
                            }

                            items(matchingFarmers) { farmer ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setFarmerForBilling(farmer)
                                            showFarmerSelectDialog = false
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = farmer.name, style = MaterialTheme.typography.titleSmall.copy(fontSize = ResponsiveText.title()))
                                            Text(text = "Village: ${farmer.village} • Mobile: ${farmer.mobile}", style = MaterialTheme.typography.bodySmall.copy(fontSize = ResponsiveText.label()), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (farmer.outstandingCredit > 0) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.errorContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "Udhari: ₹${farmer.outstandingCredit.toInt()}",
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveText.label()),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

        // Receipt Simulation
        if (showReceiptInvoiceId != null) {
            Dialog(onDismissRequest = { showReceiptInvoiceId = null }) {
                ThermalReceiptSimulationModal(
                    invoiceId = showReceiptInvoiceId!!,
                    viewModel = viewModel,
                    onDismiss = { showReceiptInvoiceId = null }
                )
            }
        }
    }
}

@Composable
fun ProductBillingGridItem(item: Product, quantityInBasket: Double, onAdd: () -> Unit) {
    val isOutOfStock = item.stock <= quantityInBasket
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable(enabled = !isOutOfStock) { onAdd() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOutOfStock) 0.dp else 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = ResponsiveText.body()),
                    color = if (isOutOfStock) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.category} • ${item.weightValue.format()} ${item.weightUnit}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = ResponsiveText.label()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text(
                        text = "₹${item.sellingPrice.toInt()}",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = ResponsiveText.title()),
                        fontWeight = FontWeight.Black,
                        color = if (isOutOfStock) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (item.stock == 0.0) "Out of Stock" else "${item.stock.toInt()} ${item.unit}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveText.label()),
                        color = if (item.stock <= 10) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOutOfStock) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (quantityInBasket > 0) {
                            Text(text = "${quantityInBasket.toInt()}", style = MaterialTheme.typography.labelLarge.copy(fontSize = ResponsiveText.label()), fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartCheckoutColumn(
    basket: Map<Product, Double>,
    selectedFarmer: Farmer?,
    subtotal: Double,
    totalGst: Double,
    totalAmount: Double,
    posPaymentMode: String,
    posDiscount: Double,
    paidAmountEntered: String,
    onPaidAmountChange: (String) -> Unit,
    viewModel: ErpViewModel,
    onFarmerClick: () -> Unit,
    onCheckoutSuccess: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header and Farmer Info (Fixed at top)
        Text("Active POS Cart", style = MaterialTheme.typography.titleLarge.copy(fontSize = ResponsiveText.heading()))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onFarmerClick() },
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Farmer Linked", style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveText.label()), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Text(selectedFarmer?.name ?: "Walk-in Farmer (Standard)", style = MaterialTheme.typography.titleSmall.copy(fontSize = ResponsiveText.title()), fontWeight = FontWeight.Bold)
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Basket listing (Scrollable, Takes up available space)
        if (basket.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                    Text("Cart is empty.\nTap inputs to add to billing order.", style = MaterialTheme.typography.bodyMedium.copy(fontSize = ResponsiveText.body()), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(basket.entries.toList()) { (product, qty) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, style = MaterialTheme.typography.bodyMedium.copy(fontSize = ResponsiveText.body()), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${product.weightValue.format()} ${product.weightUnit} • ₹${product.sellingPrice.format()} x ${qty.toInt()}", style = MaterialTheme.typography.bodySmall.copy(fontSize = ResponsiveText.label()), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.subtractProductFromBasket(product) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Text(text = "${qty.toInt()}", style = MaterialTheme.typography.titleSmall.copy(fontSize = ResponsiveText.body()), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                IconButton(onClick = { viewModel.addProductToBasket(product) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
                TextButton(
                    onClick = { viewModel.clearPOSBillingBasket() },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Cart Basket", style = MaterialTheme.typography.labelLarge.copy(fontSize = ResponsiveText.body()))
                }
            }
        }

        // Summary accounting panel (Fixed at bottom)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryRow("Subtotal", "₹${subtotal.format()}")
                    SummaryRow("Included GST (SGST+CGST)", "₹${totalGst.format()}")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cash Discount", style = MaterialTheme.typography.bodySmall.copy(fontSize = ResponsiveText.label()), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ListOfDiscountButtons(currentDiscount = posDiscount) { viewModel.posDiscount.value = it }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Payable Gross Total", style = MaterialTheme.typography.titleMedium.copy(fontSize = ResponsiveText.title()), fontWeight = FontWeight.Bold)
                        Text("₹${totalAmount.format()}", style = MaterialTheme.typography.headlineSmall.copy(fontSize = ResponsiveText.heading()), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                    
                    val remaining = (selectedFarmer?.outstandingCredit ?: 0.0) + totalAmount - (paidAmountEntered.toDoubleOrNull() ?: 0.0)
                    val isPaid = remaining <= 0.1 && posPaymentMode != "Credit"
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Payment Status", style = MaterialTheme.typography.bodySmall.copy(fontSize = ResponsiveText.label()), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(if (isPaid) SuccessGreen600 else MaterialTheme.colorScheme.error, RoundedCornerShape(50)))
                            Text(
                                text = if (isPaid) "PAID" else "UNPAID",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isPaid) SuccessGreen600 else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Payment Method
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Payment Method", style = MaterialTheme.typography.labelLarge.copy(fontSize = ResponsiveText.label()), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Cash", "UPI", "Credit").forEach { mode ->
                        val isSelected = posPaymentMode == mode
                        val isCreditBlocked = mode == "Credit" && selectedFarmer == null
                        
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable(enabled = !isCreditBlocked) { viewModel.posPaymentMode.value = mode },
                            color = when {
                                isCreditBlocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (mode == "Credit") "Udhari" else mode,
                                    color = if (isCreditBlocked) MaterialTheme.colorScheme.outline else if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = ResponsiveText.body()),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Paid Amount or Credit Info
            if (posPaymentMode != "Credit") {
                OutlinedTextField(
                    value = paidAmountEntered,
                    onValueChange = { onPaidAmountChange(it) },
                    placeholder = { Text("Exact Paid Amount", fontSize = ResponsiveText.body()) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = ResponsiveText.body())
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Balance will be billed to farmer's account.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = ResponsiveText.label()),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Checkout Button
            AppButton(
                onClick = {
                    val finalPaidAmount = paidAmountEntered.toDoubleOrNull() ?: totalAmount
                    viewModel.checkoutBasket(finalPaidAmount) { generatedId ->
                        onCheckoutSuccess(generatedId)
                    }
                },
                enabled = basket.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                size = ButtonSize.LARGE
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("GENERATE INVOICE", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ListOfDiscountButtons(currentDiscount: Double, onSelected: (Double) -> Unit) {
    listOf(0.0, 50.0, 100.0, 200.0).forEach { discount ->
        val selected = currentDiscount == discount
        Surface(
            modifier = Modifier.clickable { onSelected(discount) },
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp),
            border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Text(
                text = if (discount == 0.0) "None" else "₹${discount.toInt()}",
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveText.label()),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(fontSize = ResponsiveText.label()), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontSize = ResponsiveText.body()), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ThermalReceiptSimulationModal(invoiceId: Long, viewModel: ErpViewModel, onDismiss: () -> Unit) {
    val invoices by viewModel.invoices.collectAsState()
    val allItems by viewModel.allInvoiceItems.collectAsState()
    val context = LocalContext.current

    val invoice = remember(invoices, invoiceId) { invoices.find { it.id == invoiceId } }
    val invoiceItems = remember(allItems, invoiceId) { allItems.filter { it.invoiceId == invoiceId } }
    val shopProfile by viewModel.shopProfile.collectAsState()

    var pdfFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(invoice, invoiceItems, shopProfile) {
        if (invoice != null && invoiceItems.isNotEmpty() && shopProfile != null) {
            pdfFile = BillShareManager.generateInvoicePdf(context, invoice, invoiceItems, shopProfile!!)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen600,
                modifier = Modifier.size(64.dp)
            )
            
            Text(
                text = "Bill Generated Successfully!",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Invoice #${invoiceId} for ${invoice?.farmerName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (pdfFile == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Preparing GST PDF...", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppButton(
                        onClick = { pdfFile?.let { BillShareManager.viewPdf(context, it) } },
                        modifier = Modifier.fillMaxWidth(),
                        size = ButtonSize.MEDIUM
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Invoice")
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AppButton(
                            onClick = { pdfFile?.let { BillShareManager.sharePdf(context, it) } },
                            modifier = Modifier.weight(1f),
                            size = ButtonSize.MEDIUM,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp")
                        }

                        AppOutlinedButton(
                            onClick = { 
                                pdfFile?.let { BillPrintManager.printPdf(context, it, "Invoice_${invoiceId}") }
                            },
                            modifier = Modifier.weight(1f),
                            size = ButtonSize.MEDIUM
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Print Bill")
                        }
                    }
                }
            }

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close & New Bill")
            }
        }
    }
}

@Composable
fun ReceiptBreakdownRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    bold: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = ResponsiveText.body()),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = ResponsiveText.body()),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace
        )
    }
}
