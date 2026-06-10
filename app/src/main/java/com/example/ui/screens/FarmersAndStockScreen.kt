package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.viewmodel.ErpViewModel

@Composable
fun FarmersAndStockScreen(viewModel: ErpViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Farmers", "Stock")
    val icons = listOf(Icons.Default.People, Icons.Default.Warehouse)

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
            0 -> FarmersScreen(viewModel = viewModel)
            1 -> StockScreen(viewModel = viewModel)
        }
    }
}
