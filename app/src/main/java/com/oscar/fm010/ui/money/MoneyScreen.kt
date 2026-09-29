package com.oscar.fm010.ui.money

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    currentTotalMoney: Double,
    onUpdateMoney: (Double) -> Unit
) {
    var moneyInput by remember(currentTotalMoney) {
        mutableStateOf(if (currentTotalMoney > 0.0) currentTotalMoney.toString() else "")
    }
    var showSuccessMessage by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Dinero Disponible", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Total Actual en Cuentas / Efectivo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$${String.format("%.2f", currentTotalMoney)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "Actualiza o ingresa el total de dinero disponible que tienes actualmente.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = moneyInput,
                onValueChange = { moneyInput = it },
                label = { Text("Nuevo Total de Dinero ($)") },
                leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    val parsed = moneyInput.toDoubleOrNull() ?: 0.0
                    onUpdateMoney(parsed)
                    showSuccessMessage = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar Dinero Disponible")
            }

            if (showSuccessMessage) {
                Snackbar(
                    action = {
                        TextButton(onClick = { showSuccessMessage = false }) {
                            Text("Cerrar")
                        }
                    }
                ) {
                    Text("¡Dinero actualizado con éxito!")
                }
            }
        }
    }
}
