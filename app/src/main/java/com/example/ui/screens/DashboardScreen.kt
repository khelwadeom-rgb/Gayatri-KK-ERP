package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Invoice
import com.example.data.model.Product
import com.example.ui.components.AppLogo
import com.example.ui.theme.*
import com.example.ui.viewmodel.ErpViewModel

import com.example.util.format
import java.text.SimpleDateFormat
import java.util.*

data class ActivityItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val type: String, // Bill, Purchase, Udhari, Recovery
    val timestamp: Long,
    val date: String,
    val time: String,
    val color: Color
)

@Composable
fun DashboardScreen(viewModel: ErpViewModel) {
    val farmers by viewModel.farmers.collectAsState()
    val products by viewModel.products.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val purchases by viewModel.purchaseInvoices.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()

    // Calculate Stats
    val tz = TimeZone.getTimeZone("Asia/Kolkata")
    val now = Calendar.getInstance(tz).time
    val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { timeZone = tz }.format(now)
    val currentMonthStr = SimpleDateFormat("MM/yyyy", Locale.getDefault()).apply { timeZone = tz }.format(now)

    val todaySales = remember(invoices) {
        invoices.filter { it.createdDate == todayStr }.sumOf { it.totalAmount }
    }
    val monthlyRevenue = remember(invoices) {
        invoices.filter { it.createdDate.endsWith(currentMonthStr) }.sumOf { it.totalAmount }
    }
    
    val todayPurchases = remember(purchases) {
        purchases.filter { it.invoiceDate == todayStr }.sumOf { it.totalAmount }
    }
    val monthlyPurchases = remember(purchases) {
        purchases.filter { it.invoiceDate.endsWith(currentMonthStr) }.sumOf { it.totalAmount }
    }
    
    val outstandingCredits = remember(farmers) {
        farmers.sumOf { it.outstandingCredit }
    }
    val pendingSupplierPayments = remember(suppliers) {
        suppliers.sumOf { it.outstandingBalance }
    }
    
    val lowStockCount = remember(products) {
        products.count { it.stock <= 10 }
    }
    val totalFarmers = farmers.size
    val totalSuppliers = suppliers.size
    val creditRecords by viewModel.creditRecords.collectAsState()

    val activities = remember(invoices, purchases, creditRecords) {
        val list = mutableListOf<ActivityItem>()
        
        invoices.forEach { 
            list.add(ActivityItem(
                id = "B_${it.id}",
                title = it.farmerName,
                subtitle = "Bill #${it.id}",
                amount = it.totalAmount,
                type = "Bill",
                timestamp = it.createdAt,
                date = it.createdDate,
                time = it.createdTime,
                color = GKKPrimary
            ))
        }

        purchases.forEach {
            list.add(ActivityItem(
                id = "P_${it.id}",
                title = it.supplierName,
                subtitle = "Purchase #${it.invoiceNumber}",
                amount = it.totalAmount,
                type = "Purchase",
                timestamp = it.createdAt,
                date = it.invoiceDate,
                time = "",
                color = InsightBlue500
            ))
        }

        creditRecords.forEach {
            val isRecovery = it.amount < 0
            list.add(ActivityItem(
                id = "C_${it.id}",
                title = it.farmerName,
                subtitle = if (isRecovery) "Udhari Recovery" else "Credit Entry",
                amount = kotlin.math.abs(it.amount),
                type = if (isRecovery) "Recovery" else "Udhari",
                timestamp = it.timestamp,
                date = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(it.timestamp)),
                time = SimpleDateFormat("hh:mm a", Locale.US).format(Date(it.timestamp)),
                color = if (isRecovery) SuccessGreen600 else Color.Red
            ))
        }

        list.sortByDescending { it.timestamp }
        list.take(10)
    }

    val config = LocalConfiguration.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Premium System Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AppLogo(
                        size = 80,
                        logoPath = shopProfile?.logoPath,
                        onClick = { viewModel.currentRoute.value = "settings" }
                    )
                    Column {
                        Text(
                            text = shopProfile?.shopName ?: "Gayatri Krushi Kendra",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = ResponsiveText.heading()),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (shopProfile?.shopAddress != null) shopProfile!!.shopAddress else "मनुर बु, बोदवड, जळगाव",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveText.label()),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Notification Bell Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)), RoundedCornerShape(12.dp))
                        .clickable { /* Tap activity */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    if (lowStockCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.error)
                                .align(Alignment.TopEnd)
                                .offset(x = (-4).dp, y = 4.dp)
                        )
                    }
                }
            }
        }

        // 2. Summary Stats Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Main Highlight Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(GKKPrimary, Emerald600)
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Text(
                                text = "TODAY'S SALES: ₹${todaySales.toInt()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "PURCHASES: ₹${todayPurchases.toInt()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Monthly Revenue: ₹${monthlyRevenue.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "Monthly Purchases: ₹${monthlyPurchases.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                // Responsive Row of smaller stats
                val isTablet = config.screenWidthDp >= 600
                if (isTablet) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardStatCard(
                            modifier = Modifier.weight(1f),
                            title = "PENDING CREDIT",
                            value = "₹${outstandingCredits.toInt()}",
                            subtitle = "From Farmers",
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            onClick = { viewModel.currentRoute.value = "udhari" }
                        )
                        DashboardStatCard(
                            modifier = Modifier.weight(1f),
                            title = "PENDING PAYABLE",
                            value = "₹${pendingSupplierPayments.toInt()}",
                            subtitle = "To Suppliers",
                            accentColor = MaterialTheme.colorScheme.error,
                            onClick = { viewModel.currentRoute.value = "suppliers" }
                        )
                        DashboardStatCard(
                            modifier = Modifier.weight(1f),
                            title = "SOURCES",
                            value = "$totalSuppliers",
                            subtitle = "Active Suppliers",
                            accentColor = InsightBlue500,
                            onClick = { viewModel.currentRoute.value = "suppliers" }
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DashboardStatCard(
                                modifier = Modifier.weight(1f),
                                title = "UDHARI",
                                value = "₹${outstandingCredits.toInt()}",
                                subtitle = "From Farmers",
                                accentColor = MaterialTheme.colorScheme.tertiary,
                                onClick = { viewModel.currentRoute.value = "udhari" }
                            )
                            DashboardStatCard(
                                modifier = Modifier.weight(1f),
                                title = "PAYABLE",
                                value = "₹${pendingSupplierPayments.toInt()}",
                                subtitle = "To Suppliers",
                                accentColor = MaterialTheme.colorScheme.error,
                                onClick = { viewModel.currentRoute.value = "suppliers" }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                             DashboardStatCard(
                                modifier = Modifier.weight(1f),
                                title = "FARMERS",
                                value = "$totalFarmers",
                                subtitle = "Registered",
                                accentColor = InsightBlue500,
                                onClick = { viewModel.currentRoute.value = "farmers" }
                            )
                            DashboardStatCard(
                                modifier = Modifier.weight(1f),
                                title = "SUPPLIERS",
                                value = "$totalSuppliers",
                                subtitle = "Active",
                                accentColor = Color(0xFF9C27B0),
                                onClick = { viewModel.currentRoute.value = "suppliers" }
                            )
                        }
                    }
                }
            }
        }

        // 3. Quick Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // AI Section
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier.weight(1f).height(80.dp).clickable { viewModel.currentRoute.value = "ai_assistant" },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("GKK AI", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Business Insights", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f).height(80.dp).clickable { viewModel.currentRoute.value = "ai_doctor" },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Column {
                                Text("Crop Doctor", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("AI Diagnosis", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.currentRoute.value = "billing" },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Bill")
                    }
                    Button(
                        onClick = { viewModel.currentRoute.value = "purchases" },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        Icon(Icons.Default.AddBusiness, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Purchase")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.currentRoute.value = "farmers" },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Farmer")
                    }
                    Button(
                        onClick = { viewModel.currentRoute.value = "suppliers" },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gray100, contentColor = Charcoal900)
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Supplier")
                    }
                }
            }
        }

        // 4. Recent Activity
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = ResponsiveText.title()),
                        fontWeight = FontWeight.Bold
                    )
                }

                if (activities.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No recent activity", style = MaterialTheme.typography.bodyMedium.copy(fontSize = ResponsiveText.body()), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    activities.forEach { activity ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Surface(
                                        modifier = Modifier.size(40.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        color = activity.color.copy(alpha = 0.1f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = when(activity.type) {
                                                    "Bill" -> Icons.Default.ReceiptLong
                                                    "Purchase" -> Icons.Default.AddBusiness
                                                    "Udhari" -> Icons.Default.RemoveCircleOutline
                                                    "Recovery" -> Icons.Default.CheckCircleOutline
                                                    else -> Icons.Default.History
                                                },
                                                contentDescription = null,
                                                tint = activity.color,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = activity.title, 
                                            style = MaterialTheme.typography.titleSmall.copy(fontSize = ResponsiveText.title()), 
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${activity.subtitle} • ${activity.date}", 
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveText.label()), 
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if(activity.type == "Purchase" || activity.type == "Udhari") "-" else "+"}₹${activity.amount.toInt()}", 
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = ResponsiveText.title()), 
                                        fontWeight = FontWeight.Bold, 
                                        color = if(activity.type == "Purchase" || activity.type == "Udhari") Color.Red else SuccessGreen600
                                    )
                                    Surface(
                                        color = activity.color.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = activity.type.uppercase(), 
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), 
                                            color = activity.color,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Warehouse Alerts Section
        item {
            WarningsSection(products = products)
        }
    }
}

@Composable
fun DashboardStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accentColor)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveText.label()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = ResponsiveText.heading()),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = ResponsiveText.label()),
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun WarningsSection(products: List<Product>) {
    val now = System.currentTimeMillis()
    val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
    
    val lowStockList = remember(products) {
        products.filter { it.stock <= 10 }.take(4)
    }
    
    val expiringSoonList = remember(products) {
        products.filter { it.expiryDate != 0L && it.expiryDate - now < thirtyDaysInMillis }.take(4)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationImportant,
                    contentDescription = "Warnings",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Operational Warehouse Alerts",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = ResponsiveText.title()),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (lowStockList.isEmpty() && expiringSoonList.isEmpty()) {
                Text(
                    text = "✓ All inventory stock items are healthy. No replenishment or expiry issues detected.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = ResponsiveText.body()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                if (lowStockList.isNotEmpty()) {
                    Text("Low Stock Alerts", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    lowStockList.forEachIndexed { index, product ->
                        AlertRow(
                            title = product.name,
                            subtitle = product.category,
                            value = if (product.stock == 0.0) "Out of Stock" else "Only ${product.stock.toInt()} ${product.unit} left",
                            isError = product.stock == 0.0
                        )
                    }
                }

                if (expiringSoonList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Expiry Alerts (Next 30 Days)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Red)
                    expiringSoonList.forEach { product ->
                        val daysLeft = ((product.expiryDate - now) / (1000 * 60 * 60 * 24)).toInt()
                        AlertRow(
                            title = product.name,
                            subtitle = "Batch: ${product.batchNumber}",
                            value = if (daysLeft < 0) "Expired" else "Expires in $daysLeft days",
                            isError = daysLeft < 0
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlertRow(title: String, subtitle: String, value: String, isError: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.5f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = ResponsiveText.title()),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = ResponsiveText.label()),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = value,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveText.label()),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
}
