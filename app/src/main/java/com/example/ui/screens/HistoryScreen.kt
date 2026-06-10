package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Invoice
import com.example.ui.components.*
import com.example.ui.theme.ResponsiveText
import com.example.ui.theme.SuccessGreen600
import com.example.ui.viewmodel.ErpViewModel
import com.example.util.BillShareManager
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: ErpViewModel) {
    val invoices by viewModel.invoices.collectAsState()
    val allItems by viewModel.allInvoiceItems.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val context = LocalContext.current

    var selectedInvoiceId by remember { mutableStateOf<Long?>(null) }
    var showDeleteDialogFor by remember { mutableStateOf<Invoice?>(null) }

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
            val shopProfile by viewModel.shopProfile.collectAsState()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppLogo(size = 48, logoPath = shopProfile?.logoPath)
                Text(
                    text = "Invoice History",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = ResponsiveText.heading()),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (invoices.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No invoices found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(invoices) { invoice ->
                    InvoiceCard(
                        invoice = invoice,
                        onView = { selectedInvoiceId = invoice.id },
                        onDelete = { showDeleteDialogFor = invoice },
                        canDelete = currentUserRole != "Shop Staff"
                    )
                }
            }
        }
    }

    if (selectedInvoiceId != null) {
        Dialog(onDismissRequest = { selectedInvoiceId = null }) {
            ThermalReceiptSimulationModal(
                invoiceId = selectedInvoiceId!!,
                viewModel = viewModel,
                onDismiss = { selectedInvoiceId = null }
            )
        }
    }

    if (showDeleteDialogFor != null) {
        DeleteWithReasonDialog(
            title = "Delete Invoice #${showDeleteDialogFor!!.id}?",
            options = listOf("Wrong Entry", "Customer Cancelled", "Duplicate Bill", "Return Items", "Other"),
            onDismiss = { showDeleteDialogFor = null },
            onConfirm = { reason ->
                viewModel.deleteInvoiceWithReason(showDeleteDialogFor!!, reason)
                showDeleteDialogFor = null
            }
        )
    }
}

@Composable
fun InvoiceCard(
    invoice: Invoice,
    onView: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onView() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = invoice.farmerName,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = ResponsiveText.title()),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ID: #${invoice.id} • ${invoice.createdDate}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveText.label()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = invoice.paymentMode,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    if (invoice.paymentStatus == "PAID") {
                        Surface(
                            color = SuccessGreen600.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, SuccessGreen600.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "PAID",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                                color = SuccessGreen600,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = invoice.createdTime,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${invoice.totalAmount.toInt()}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = ResponsiveText.title()
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(onClick = onView, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Visibility, contentDescription = "View", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                
                if (canDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
