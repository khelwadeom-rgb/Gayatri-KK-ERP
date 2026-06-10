package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppLogo
import com.example.ui.theme.ResponsiveText
import com.example.ui.viewmodel.ErpViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.BillingScreen
import com.example.ui.screens.FarmersAndStockScreen
import com.example.ui.screens.PurchaseAndSuppliersScreen
import com.example.ui.screens.UdhariScreen
import com.example.ui.screens.SettingsScreen

data class NavTab(
    val route: String,
    val labelKey: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainCabinet(viewModel: ErpViewModel) {
    val currentRoute by viewModel.currentRoute.collectAsState()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 768
    val shopProfile by viewModel.shopProfile.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val products by viewModel.products.collectAsState()
    val purchases by viewModel.purchaseInvoices.collectAsState()
    val farmers by viewModel.farmers.collectAsState()

    val lowStockCount = remember(products) { products.count { it.stock <= 10 } }
    val pendingPurchaseCount = remember(purchases) { purchases.count { it.paymentStatus != "Paid" } }
    val outstandingFarmerCount = remember(farmers) { farmers.count { it.outstandingCredit > 0 } }

    val allTabs = listOf(
        NavTab("dashboard", "dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        NavTab("billing", "billing", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
        NavTab("farmers_stock", "farmers_stock", Icons.Filled.Warehouse, Icons.Outlined.Warehouse),
        NavTab("purchase_suppliers", "purchase_suppliers", Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping),
        NavTab("udhari", "udhari", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)
    )
    
    val navTabs = remember(currentUserRole) {
        if (currentUserRole == "Shop Staff") {
            // Staff restricted: No Udhari, Purchases, Suppliers in main nav? 
            // PRD: "Allowed: Billing, Farmer lookup. Restricted: Settings, Delete actions, Developer panel."
            allTabs.filter { it.route in listOf("dashboard", "billing", "farmers_stock") }
        } else {
            allTabs
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!isTablet) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.currentRoute.value = "settings" }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            AppLogo(size = 48, logoPath = shopProfile?.logoPath)
                            Text(
                                text = shopProfile?.shopName ?: "Gayatri Krushi Kendra",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = ResponsiveText.title()),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (!isTablet) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    windowInsets = NavigationBarDefaults.windowInsets
                ) {
                    navTabs.forEach { tab ->
                        val isSel = currentRoute == tab.route
                        val badgeCount = when(tab.route) {
                            "farmers_stock" -> lowStockCount
                            "purchase_suppliers" -> pendingPurchaseCount
                            "udhari" -> outstandingFarmerCount
                            else -> 0
                        }

                        NavigationBarItem(
                            selected = isSel,
                            onClick = { viewModel.currentRoute.value = tab.route },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (badgeCount > 0) {
                                            Badge {
                                                Text(badgeCount.toString())
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSel) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = viewModel.t(tab.labelKey)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = viewModel.t(tab.labelKey),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // LEFT SIDEBAR (Only visible on tablets)
            if (isTablet) {
                Column(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)), RoundedCornerShape(0.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        // Shop Brand Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.currentRoute.value = "settings" }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AppLogo(size = 60, logoPath = shopProfile?.logoPath)
                            Column {
                                Text(
                                    text = shopProfile?.shopName ?: "Gayatri Krushi Kendra",
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = ResponsiveText.title()),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "ERP v1.1 Premium",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveText.label()),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Sidebar buttons
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            navTabs.forEach { tab ->
                                val isSel = currentRoute == tab.route
                                val badgeCount = when(tab.route) {
                                    "farmers_stock" -> lowStockCount
                                    "purchase_suppliers" -> pendingPurchaseCount
                                    "udhari" -> outstandingFarmerCount
                                    else -> 0
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clickable { viewModel.currentRoute.value = tab.route },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isSel) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = viewModel.t(tab.labelKey),
                                                tint = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Text(
                                                text = viewModel.t(tab.labelKey),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        
                                        if (badgeCount > 0) {
                                            Badge {
                                                Text(badgeCount.toString())
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom info label
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)))
                                Text(
                                    text = "Connected DB Local",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Licensed Shopkeeper Account",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // MAIN CONTENT PORTAL
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentRoute) {
                    "dashboard" -> DashboardScreen(viewModel = viewModel)
                    "billing" -> BillingScreen(viewModel = viewModel)
                    "farmers_stock" -> FarmersAndStockScreen(viewModel = viewModel)
                    "purchase_suppliers" -> PurchaseAndSuppliersScreen(viewModel = viewModel)
                    "udhari" -> UdhariScreen(viewModel = viewModel)
                    "settings" -> SettingsScreen(viewModel = viewModel)
                    "about" -> AboutScreen(viewModel = viewModel)
                    "ai_assistant" -> AiAssistantScreen(viewModel = viewModel)
                    "ai_doctor" -> AiCropDoctorScreen(viewModel = viewModel)
                    else -> DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}
