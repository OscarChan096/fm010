package com.oscar.fm010.data.model

import java.util.UUID

data class WalletInfo(
    val totalMoney: Double = 0.0
)

data class ExpenseItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val dueDate: String,          // Fecha límite de pago (e.g., "15/10")
    val cutoffDate: String? = null, // Fecha de corte (para tarjetas de crédito, e.g., "05/10")
    val isCreditCard: Boolean = false,
    val category: String = "General",
    val reminderDays: String = "5, 1"
)
