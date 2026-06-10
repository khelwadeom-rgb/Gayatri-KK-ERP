package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.data.model.ShopProfile
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ErpViewModel
import com.example.util.BillShareManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: ErpViewModel) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val activeRole by viewModel.currentUserRole.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()

    val languages = listOf("English", "Hindi", "Marathi")
    val roles = listOf("Super Admin", "Shop Staff", "Accountant")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppLogo(size = 140, logoPath = shopProfile?.logoPath)
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = shopProfile?.shopName ?: "Gayatri Krushi Kendra",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (shopProfile?.shopAddress != null) shopProfile!!.shopAddress else "मनुर बु, बोदवड, जळगाव",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Language settings
        SettingsCard(title = "Localization", icon = Icons.Default.Translate) {
            Text(
                text = "Switch ERP language. All invoice tables, billing controls and farmers directories translate instantaneously.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                languages.forEach { lang ->
                    val isSel = selectedLanguage == lang
                    Surface(
                        modifier = Modifier.weight(1f).clickable { viewModel.selectedLanguage.value = lang },
                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSel) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = lang,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 14.dp)
                        )
                    }
                }
            }
        }

        // Security Settings
        SettingsCard(title = "Security Dashboard", icon = Icons.Default.Security) {
            var showChangePinDialog by remember { mutableStateOf(false) }
            val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
            
            Text(
                text = "Last Login: Today, 10:45 AM", // Placeholder
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Biometric Authentication", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("Use Fingerprint or Face to unlock", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isBiometricEnabled,
                    onCheckedChange = { viewModel.toggleBiometric(it) }
                )
            }

            AppButton(
                onClick = { showChangePinDialog = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                size = ButtonSize.SMALL
            ) {
                Icon(Icons.Default.Password, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change App Password / PIN")
            }

            if (showChangePinDialog) {
                ChangePinDialog(
                    onDismiss = { showChangePinDialog = false },
                    viewModel = viewModel
                )
            }
        }

        // Farmer Directory PDF
        SettingsCard(title = "Reports & Directories", icon = Icons.Default.Description) {
            val context = LocalContext.current
            val farmers by viewModel.farmers.collectAsState()
            val products by viewModel.products.collectAsState()
            val invoices by viewModel.invoices.collectAsState()
            val shopProfile by viewModel.shopProfile.collectAsState()

            Text(
                text = "Export professional PDF reports for farmers, inventory stock, and GST sales tax summaries.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                AppButton(
                    onClick = {
                        if (shopProfile != null) {
                            val file = BillShareManager.generateFarmerDirectoryPdf(context, farmers, shopProfile!!)
                            file?.let { BillShareManager.viewPdf(context, it) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    size = ButtonSize.SMALL
                ) {
                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Farmer Directory")
                }

                AppButton(
                    onClick = {
                        if (shopProfile != null) {
                            val file = BillShareManager.generateStockDirectoryPdf(context, products, shopProfile!!)
                            file?.let { BillShareManager.viewPdf(context, it) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    size = ButtonSize.SMALL
                ) {
                    Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Stock Directory")
                }

                AppButton(
                    onClick = {
                        if (shopProfile != null) {
                            val file = BillShareManager.generateGstReportPdf(context, invoices, shopProfile!!)
                            file?.let { BillShareManager.viewPdf(context, it) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    size = ButtonSize.SMALL
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export GST Sales Report")
                }

                AppButton(
                    onClick = {
                        if (shopProfile != null) {
                            val file = BillShareManager.generateSalesReportPdf(context, invoices, shopProfile!!)
                            file?.let { BillShareManager.viewPdf(context, it) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    size = ButtonSize.SMALL
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Sales Summary Report")
                }

                AppButton(
                    onClick = {
                        if (shopProfile != null) {
                            val file = BillShareManager.generatePurchaseSummaryPdf(context, viewModel.purchaseInvoices.value, shopProfile!!)
                            file?.let { BillShareManager.viewPdf(context, it) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    size = ButtonSize.SMALL
                ) {
                    Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Purchase Report")
                }
            }
        }

        // Active Role
        SettingsCard(title = "User Authentication Role", icon = Icons.Default.VerifiedUser) {
            Text(
                text = "Configure the currently active operational profile. Permissions adapt instantly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                roles.forEach { role ->
                    val isSel = activeRole == role
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.currentUserRole.value = role },
                        color = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = role,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when (role) {
                                        "Super Admin" -> "Full Administrative access, configuration control."
                                        "Shop Staff" -> "Restricted clerk access. POS and Farmer registers."
                                        else -> "Financial bookkeeper access. Active logs summaries."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RadioButton(
                                selected = isSel,
                                onClick = { viewModel.currentUserRole.value = role },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }

        // Shop metadata
        SettingsCard(title = "Shop Ledger Configuration", icon = Icons.Default.Store) {
            Text(
                text = "Edit receipt headings, contact lines, and active tax registration configurations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = "Gayatri Krushi Kendra ERP",
                    onValueChange = {},
                    label = { Text("Shop Name") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = "27AWGPK6243H1ZF",
                    onValueChange = {},
                    label = { Text("GSTIN") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = "Manur bk, Bodwad, Jalgaon",
                    onValueChange = {},
                    label = { Text("Address") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // API settings
        SettingsCard(title = "Cloud Synchronization Mode", icon = Icons.Default.CloudSync) {
            val isSyncEnabled by viewModel.isCloudSyncEnabled.collectAsState()
            val firebaseStatus by viewModel.firebaseStatus.collectAsState()
            val connectionError by viewModel.connectionError.collectAsState()
            var showLoginDialog by remember { mutableStateOf(false) }

            Text(
                text = "Enable multi-device real-time sync via Firebase Cloud Firestore. Access your data from any terminal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = firebaseStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (firebaseStatus) {
                            "Connected" -> SuccessGreen600
                            "Local Mode Active" -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                    if (connectionError != null) {
                        Text(
                            text = connectionError!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = if (isSyncEnabled) "Connected to Gayatri Cloud" else "Disconnected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isSyncEnabled,
                    onCheckedChange = { 
                        if (it) showLoginDialog = true else viewModel.toggleCloudSync(false)
                    }
                )
            }
            
            if (isSyncEnabled && firebaseStatus == "Connected") {
                AppButton(
                    onClick = { viewModel.syncDataToCloud() },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    size = ButtonSize.SMALL
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Local Data to Cloud")
                }

                TextButton(
                    onClick = { viewModel.logoutFromCloud() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Logout from Cloud Account", color = MaterialTheme.colorScheme.error)
                }
            } else if (isSyncEnabled && firebaseStatus == "Auth Required") {
                AppButton(
                    onClick = { showLoginDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    size = ButtonSize.SMALL
                ) {
                    Text("Login to Firebase")
                }
            }

            if (showLoginDialog) {
                CloudLoginDialog(
                    onDismiss = { showLoginDialog = false },
                    viewModel = viewModel
                )
            }
        }

        // Gemini AI settings
        SettingsCard(title = "System Services", icon = Icons.Default.SettingsInputAntenna) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Gemini AI Client", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("Smart Agronomist Services", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Surface(
                    color = SuccessGreen600.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Connected",
                        color = SuccessGreen600,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // App Distribution
        SettingsCard(title = "App Distribution & Sharing", icon = Icons.Default.Share) {
            val context = LocalContext.current
            var showShareOptions by remember { mutableStateOf(false) }

            Text(
                text = "Share the Gayatri ERP application directly with other staff or devices via APK or download link.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AppButton(
                onClick = { showShareOptions = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                size = ButtonSize.SMALL
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share App Options")
            }

            if (showShareOptions) {
                ShareAppDialog(onDismiss = { showShareOptions = false }, context = context)
            }
        }

        // About Application
        SettingsCard(title = "About Application", icon = Icons.Default.Info) {
            Text(
                text = "View technical specifications, technology stack and developer information.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AppButton(
                onClick = { viewModel.currentRoute.value = "about" },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                size = ButtonSize.SMALL
            ) {
                Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Application Info")
            }
        }

        // Developer Options (Hidden/Admin)
        var showDevLogin by remember { mutableStateOf(false) }
        var showDevPanel by remember { mutableStateOf(false) }
        val isDevAuthed by viewModel.isDeveloperAuthenticated.collectAsState()

        if (isDevAuthed) {
            TextButton(
                onClick = { showDevPanel = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Developer Configuration Options", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }

        if (showDevLogin) {
            DeveloperLoginDialog(
                onDismiss = { showDevLogin = false },
                onSuccess = { 
                    showDevLogin = false
                    showDevPanel = true 
                },
                viewModel = viewModel
            )
        }

        if (showDevPanel && isDevAuthed) {
            DeveloperPanelDialog(
                onDismiss = { showDevPanel = false },
                viewModel = viewModel
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        
        var tapCount by remember { mutableStateOf(0) }
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .clickable { 
                    tapCount++
                    if (tapCount >= 5) {
                        tapCount = 0
                        if (isDevAuthed) showDevPanel = true else showDevLogin = true
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Gayatri Krushi Kendra ERP",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Developed by",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "OM EKNATH KHELWADE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Version 1.1",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            if (tapCount in 1..4) {
                Text(
                    text = "Tap ${5 - tapCount} more times for Dev Mode",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ShareAppDialog(onDismiss: () -> Unit, context: android.content.Context) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share App") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose how you want to share the application.")
                
                Button(
                    onClick = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, "Download Gayatri Krushi Kendra ERP: https://gkk-erp.web.app/download")
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Download Link"))
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Option 1: Share Download Link")
                }

                Button(
                    onClick = {
                        BillShareManager.shareApp(context)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Option 2: Share APK File")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SettingsCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
fun ChangePinDialog(onDismiss: () -> Unit, viewModel: ErpViewModel) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change App PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { if (it.length <= 6) currentPin = it },
                    label = { Text("Current PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6) newPin = it },
                    label = { Text("New 6-Digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6) confirmPin = it },
                    label = { Text("Confirm New PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error.isNotEmpty()) {
                    Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (currentPin.length != 6 || newPin.length != 6 || confirmPin.length != 6) {
                    error = "All PINs must be 6 digits."
                } else if (newPin != confirmPin) {
                    error = "New PIN and confirmation do not match."
                } else {
                    val success = viewModel.updateAppPin(currentPin, newPin)
                    if (success) {
                        onDismiss()
                    } else {
                        error = "Incorrect current PIN."
                    }
                }
            }) {
                Text("Update PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CloudLoginDialog(onDismiss: () -> Unit, viewModel: ErpViewModel) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cloud Sync Authentication") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter shop cloud credentials to enable real-time sync.")
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Admin Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error.isNotEmpty()) {
                    Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.loginToCloud(email, pass, 
                    onSuccess = { onDismiss() },
                    onError = { error = it }
                )
            }) {
                Text("Enable Sync")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeveloperLoginDialog(onDismiss: () -> Unit, onSuccess: () -> Unit, viewModel: ErpViewModel) {
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Developer Access") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter Developer Password")
                OutlinedTextField(
                    value = pass,
                    onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) pass = it },
                    label = { Text("6-Digit Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (error.isNotEmpty()) {
                    Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (viewModel.loginAsDeveloper(pass)) {
                    onSuccess()
                } else {
                    error = "Incorrect Password"
                }
            }) {
                Text("Verify")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeveloperPanelDialog(onDismiss: () -> Unit, viewModel: ErpViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shopProfile by viewModel.shopProfile.collectAsState()
    
    // Local state for editing
    var sName by remember { mutableStateOf(shopProfile?.shopName ?: "") }
    var sAddress by remember { mutableStateOf(shopProfile?.shopAddress ?: "") }
    var sOwner by remember { mutableStateOf(shopProfile?.ownerName ?: "") }
    var sMobile by remember { mutableStateOf(shopProfile?.mobileNumber ?: "") }
    var sAltMobile by remember { mutableStateOf(shopProfile?.alternateNumber ?: "") }
    var sGst by remember { mutableStateOf(shopProfile?.gstNumber ?: "") }
    var sPesticide by remember { mutableStateOf(shopProfile?.pesticideLicense ?: "") }
    var sSeed by remember { mutableStateOf(shopProfile?.seedLicense ?: "") }
    var sCotton by remember { mutableStateOf(shopProfile?.cottonLicense ?: "") }
    var sFertilizer by remember { mutableStateOf(shopProfile?.fertilizerLicense ?: "") }
    var sUpi by remember { mutableStateOf(shopProfile?.upiId ?: "") }
    var sQrPath by remember { mutableStateOf(shopProfile?.qrImagePath ?: "") }
    var sLogoPath by remember { mutableStateOf(shopProfile?.logoPath ?: "") }
    var sInvoiceLogoPath by remember { mutableStateOf(shopProfile?.invoiceLogoPath ?: "") }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val path = viewModel.saveImageToInternalStorage(context, it, "shop_logo.png")
            if (path != null) sLogoPath = path
        }
    }

    val invoiceLogoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val path = viewModel.saveImageToInternalStorage(context, it, "invoice_logo.png")
            if (path != null) sInvoiceLogoPath = path
        }
    }

    val qrPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val path = viewModel.saveImageToInternalStorage(context, it, "upi_qr.png")
            if (path != null) sQrPath = path
        }
    }

    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                val success = com.example.util.BackupManager.restoreBackup(context, it, viewModel)
                if (success) {
                    android.widget.Toast.makeText(context, "Restore Successful", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "Restore Failed", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.95f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Developer Config", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
                }

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // branding section
                    Text("Branding & Assets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    AssetUploadSection(
                        label = "Shop Logo",
                        path = sLogoPath,
                        onUploadClick = { logoPicker.launch("image/*") }
                    )

                    AssetUploadSection(
                        label = "Invoice Logo",
                        path = sInvoiceLogoPath,
                        onUploadClick = { invoiceLogoPicker.launch("image/*") }
                    )

                    AssetUploadSection(
                        label = "UPI QR Code",
                        path = sQrPath,
                        onUploadClick = { qrPicker.launch("image/*") }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Text("Shop Identity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = sName, onValueChange = { sName = it }, label = { Text("Shop Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sAddress, onValueChange = { sAddress = it }, label = { Text("Shop Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sOwner, onValueChange = { sOwner = it }, label = { Text("Owner Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sMobile, onValueChange = { sMobile = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sAltMobile, onValueChange = { sAltMobile = it }, label = { Text("Alternate Mobile") }, modifier = Modifier.fillMaxWidth())

                    Text("Tax & Licensing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = sGst, onValueChange = { sGst = it }, label = { Text("GSTIN Number") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sPesticide, onValueChange = { sPesticide = it }, label = { Text("Pesticide License") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sSeed, onValueChange = { sSeed = it }, label = { Text("Seeds License") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sCotton, onValueChange = { sCotton = it }, label = { Text("Cotton License") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sFertilizer, onValueChange = { sFertilizer = it }, label = { Text("Fertilizer License") }, modifier = Modifier.fillMaxWidth())

                    Text("Banking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = sUpi, onValueChange = { sUpi = it }, label = { Text("UPI ID") }, modifier = Modifier.fillMaxWidth())
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Maintenance & Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    var showClearConfirm by remember { mutableStateOf(false) }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { 
                                scope.launch {
                                    val file = com.example.util.BackupManager.createBackup(context, viewModel)
                                    if (file != null) {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(android.content.Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.gkk.fileprovider", file))
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "Save Backup To"))
                                    }
                                }
                            }, 
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { 
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Backup") 
                        }

                        Button(
                            onClick = { restorePicker.launch("application/json") }, 
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { 
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore") 
                        }
                    }

                    AppOutlinedButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Wipe All Local Database")
                    }

                    if (showClearConfirm) {
                        AlertDialog(
                            onDismissRequest = { showClearConfirm = false },
                            title = { Text("Danger: Wipe Database") },
                            text = { Text("This will permanently delete all farmers, stock, bills and records from this device. Are you absolutely sure?") },
                            confirmButton = {
                                TextButton(onClick = { 
                                    viewModel.clearAllData()
                                    showClearConfirm = false
                                }) { Text("YES, WIPE", color = Color.Red) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
                            }
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.updateShopProfile(ShopProfile(
                            shopName = sName,
                            shopAddress = sAddress,
                            ownerName = sOwner,
                            mobileNumber = sMobile,
                            alternateNumber = sAltMobile,
                            gstNumber = sGst,
                            pesticideLicense = sPesticide,
                            seedLicense = sSeed,
                            cottonLicense = sCotton,
                            fertilizerLicense = sFertilizer,
                            upiId = sUpi,
                            qrImagePath = if (sQrPath.isEmpty()) null else sQrPath,
                            logoPath = if (sLogoPath.isEmpty()) null else sLogoPath,
                            invoiceLogoPath = if (sInvoiceLogoPath.isEmpty()) null else sInvoiceLogoPath
                        ))
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Global Configuration", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AssetUploadSection(label: String, path: String?, onUploadClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (path != null && path.isNotEmpty()) {
                    AsyncImage(
                        model = path,
                        contentDescription = label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }
            }
            
            AppOutlinedButton(
                onClick = onUploadClick,
                modifier = Modifier.weight(1f),
                size = ButtonSize.SMALL
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload $label")
            }
        }
        if (path == null || path.isEmpty()) {
            Text("No image selected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
