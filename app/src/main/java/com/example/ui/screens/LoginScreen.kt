package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppButton
import com.example.ui.components.AppLogo
import com.example.ui.components.ButtonSize
import com.example.ui.theme.ResponsiveText
import com.example.ui.viewmodel.ErpViewModel

@Composable
fun LoginScreen(viewModel: ErpViewModel) {
    var pin by remember { mutableStateOf("") }
    var isPinVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val loginErrorMessage by viewModel.loginErrorMessage.collectAsState()
    val loginCooldown by viewModel.loginCooldownSeconds.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            AppLogo(size = 120, logoPath = shopProfile?.logoPath)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = shopProfile?.shopName ?: "Gayatri Krushi Kendra",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Secure ERP Access",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "6 Digit PIN Login",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        pin = it
                        if (it.length == 6) {
                            viewModel.unlockApp(it)
                            if (loginErrorMessage.isEmpty()) {
                                // Pin will be cleared if successful anyway as screen changes
                            } else {
                                pin = ""
                            }
                        }
                    }
                },
                modifier = Modifier
                    .width(280.dp)
                    .focusRequester(focusRequester),
                label = { Text("Enter 6-Digit PIN") },
                visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                trailingIcon = {
                    val image = if (isPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { isPinVisible = !isPinVisible }) {
                        Icon(imageVector = image, contentDescription = null)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    letterSpacing = 8.sp,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            if (loginErrorMessage.isNotEmpty()) {
                Text(
                    text = loginErrorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (loginCooldown > 0) {
                Text(
                    text = "Try again in $loginCooldown seconds",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            AppButton(
                onClick = { viewModel.unlockApp(pin) },
                modifier = Modifier.width(200.dp),
                size = ButtonSize.MEDIUM,
                enabled = pin.length == 6 && loginCooldown == 0
            ) {
                Text(
                    text = "Unlock ERP",
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Admin Reset Option
            var showResetDialog by remember { mutableStateOf(false) }
            TextButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Forgot PIN? Admin Reset", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (showResetDialog) {
                AdminResetDialog(
                    onDismiss = { showResetDialog = false },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun AdminResetDialog(onDismiss: () -> Unit, viewModel: ErpViewModel) {
    var masterPin by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Admin Reset") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Enter Master PIN or Owner Name to unlock.")
                
                OutlinedTextField(
                    value = masterPin,
                    onValueChange = { masterPin = it },
                    label = { Text("Master PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("OR", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Owner Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (masterPin.isNotEmpty()) {
                    viewModel.resetPinViaMaster(masterPin)
                } else if (ownerName.isNotEmpty()) {
                    viewModel.resetPinViaOwnerName(ownerName)
                }
                onDismiss()
            }) {
                Text("Verify & Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
