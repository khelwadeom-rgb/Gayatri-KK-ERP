package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.viewmodel.ErpViewModel

@Composable
fun PurchaseAndSuppliersScreen(viewModel: ErpViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Purchases", "Suppliers")
    val icons = listOf(Icons.Default.AddBusiness, Icons.Default.LocalShipping)

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                    icon = { Icon(icons[index], contentDescription = null) }
                )
            }
        }
        
        when (selectedTab) {
            0 -> PurchaseScreen(viewModel = viewModel)
            1 -> SupplierScreen(viewModel = viewModel)
        }
    }
}
