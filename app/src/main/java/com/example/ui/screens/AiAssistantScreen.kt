package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeminiClient
import com.example.ui.viewmodel.ErpViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(viewModel: ErpViewModel) {
    val scope = rememberCoroutineScope()
    var aiResponse by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    
    val invoices by viewModel.invoices.collectAsState()
    val products by viewModel.products.collectAsState()
    val farmers by viewModel.farmers.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GKK AI Assistant") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.currentRoute.value = "dashboard" }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Ask GKK AI to analyze your business data and provide smart insights.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (aiResponse.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("AI Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (isThinking) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else {
                            Text(aiResponse, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
                        }
                    }
                }
            }

            Text("Quick Analysis Options", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            AiOptionCard(
                title = "Sales Insights",
                subtitle = "Analyze current month sales trends and performance.",
                icon = Icons.Default.TrendingUp,
                color = Color(0xFF4CAF50),
                onClick = {
                    scope.launch {
                        isThinking = true
                        aiResponse = "Analyzing sales data..."
                        val salesSummary = invoices.take(50).joinToString("\n") { "${it.createdDate}: ₹${it.totalAmount} (${it.farmerName})" }
                        aiResponse = GeminiClient.getAgricultureAdvice("General", "Multiple", "Various", "Analyze these recent sales and provide 3 business insights:\n$salesSummary")
                        isThinking = false
                    }
                }
            )

            AiOptionCard(
                title = "Stock Analysis",
                subtitle = "Identify low stock risks and replenishment needs.",
                icon = Icons.Default.Inventory,
                color = Color(0xFF2196F3),
                onClick = {
                    scope.launch {
                        isThinking = true
                        aiResponse = "Analyzing inventory..."
                        val stockSummary = products.joinToString("\n") { "${it.name}: ${it.stock} ${it.unit} (${it.category})" }
                        aiResponse = GeminiClient.getAgricultureAdvice("Inventory", "N/A", "N/A", "Analyze this stock list and suggest what I should order more of and why:\n$stockSummary")
                        isThinking = false
                    }
                }
            )

            AiOptionCard(
                title = "Recovery Suggestions",
                subtitle = "Strategies to recover outstanding Udhari efficiently.",
                icon = Icons.Default.AccountBalanceWallet,
                color = Color(0xFFFF9800),
                onClick = {
                    scope.launch {
                        isThinking = true
                        aiResponse = "Preparing recovery strategies..."
                        val creditSummary = farmers.filter { it.outstandingCredit > 0 }.joinToString("\n") { "${it.name}: ₹${it.outstandingCredit}" }
                        aiResponse = GeminiClient.getAgricultureAdvice("Finance", "N/A", "N/A", "Based on these outstanding credits, provide 5 professional strategies to recover money from farmers politely:\n$creditSummary")
                        isThinking = false
                    }
                }
            )

            AiOptionCard(
                title = "Product Recommendations",
                subtitle = "Suggest products for current season and crop trends.",
                icon = Icons.Default.Recommend,
                color = Color(0xFF9C27B0),
                onClick = {
                    scope.launch {
                        isThinking = true
                        aiResponse = "Generating recommendations..."
                        aiResponse = GeminiClient.getAgricultureAdvice("All Crops", "Black Soil", "Drip", "It's the current season in Maharashtra. Which 5 high-demand products (Pesticides/Fertilizers) should I promote to my farmers right now?")
                        isThinking = false
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AiOptionCard(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}
