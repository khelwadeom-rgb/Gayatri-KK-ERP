package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ResponsiveText

import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog

import androidx.compose.material.icons.filled.CalendarToday
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SelectableOptionsGroup(
    label: String,
    options: List<String>,
    selected: String,
    onSelection: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveText.label()),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { opt ->
                val isSel = selected == opt
                FilterChip(
                    selected = isSel,
                    onClick = { onSelection(opt) },
                    label = { Text(opt, fontSize = ResponsiveText.label()) },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateInputField(
    label: String, 
    timestamp: Long, 
    modifier: Modifier = Modifier,
    onDateSelected: (Long) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = timestamp)
    val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))

    OutlinedTextField(
        value = formattedDate,
        onValueChange = {},
        readOnly = true,
        label = { Text(label, fontSize = ResponsiveText.body()) },
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDatePicker = true },
        shape = RoundedCornerShape(12.dp),
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun PriceIndicatorLabel(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = ResponsiveText.label()),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = ResponsiveText.body()),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DeleteWithReasonDialog(
    title: String,
    options: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedReason by remember { mutableStateOf(options.first()) }
    var otherReason by remember { mutableStateOf("") }
    val isOtherSelected = selectedReason == "Other"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("This action cannot be undone. Please select a reason for deletion.", style = MaterialTheme.typography.bodyMedium)
                
                options.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selectedReason = option },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedReason == option, onClick = { selectedReason = option })
                        Text(option, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                if (isOtherSelected) {
                    OutlinedTextField(
                        value = otherReason,
                        onValueChange = { otherReason = it },
                        label = { Text("Please specify reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = if (isOtherSelected) otherReason else selectedReason
                    if (finalReason.isNotEmpty()) {
                        onConfirm(finalReason)
                    }
                },
                enabled = !isOtherSelected || otherReason.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Permanently")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
