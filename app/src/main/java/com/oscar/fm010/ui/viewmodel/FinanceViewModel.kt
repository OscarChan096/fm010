package com.oscar.fm010.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscar.fm010.data.model.ExpenseItem
import com.oscar.fm010.data.model.WalletInfo
import com.oscar.fm010.data.repository.FinanceRepository
import com.oscar.fm010.data.repository.SqliteFinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FinanceUiState(
    val wallet: WalletInfo = WalletInfo(),
    val expenses: List<ExpenseItem> = emptyList(),
    val totalExpenses: Double = 0.0,
    val netBalance: Double = 0.0,
    val isDarkMode: Boolean = false
)

class FinanceViewModel(
    private val repository: FinanceRepository = RepositoryProvider.repository
) : ViewModel() {

    val uiState: StateFlow<FinanceUiState> = combine(
        repository.wallet,
        repository.expenses,
        repository.isDarkMode
    ) { wallet, expenses, isDarkMode ->
        val totalExpenses = expenses.sumOf { it.amount }
        val netBalance = wallet.totalMoney - totalExpenses
        FinanceUiState(
            wallet = wallet,
            expenses = expenses,
            totalExpenses = totalExpenses,
            netBalance = netBalance,
            isDarkMode = isDarkMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinanceUiState()
    )

    fun updateWallet(totalMoney: Double) {
        viewModelScope.launch {
            repository.updateWallet(totalMoney)
        }
    }

    fun addExpense(
        context: Context,
        title: String,
        amount: Double,
        dueDate: String,
        cutoffDate: String?,
        isCreditCard: Boolean,
        category: String,
        reminderDays: String = "5, 1"
    ) {
        viewModelScope.launch {
            val expense = ExpenseItem(
                title = title,
                amount = amount,
                dueDate = dueDate,
                cutoffDate = cutoffDate.takeIf { isCreditCard && !it.isNullOrBlank() },
                isCreditCard = isCreditCard,
                category = category,
                reminderDays = reminderDays.ifBlank { "5, 1" }
            )
            repository.addExpense(expense)
            com.oscar.fm010.notification.NotificationHelper.scheduleRemindersForExpense(context.applicationContext, expense)
        }
    }

    fun deleteExpense(context: Context, id: String, reminderDays: String = "5, 1") {
        viewModelScope.launch {
            repository.deleteExpense(id)
            com.oscar.fm010.notification.NotificationHelper.cancelRemindersForExpense(context.applicationContext, id, reminderDays)
        }
    }

    fun updateExpense(context: Context, expense: ExpenseItem) {
        viewModelScope.launch {
            repository.updateExpense(expense)
            com.oscar.fm010.notification.NotificationHelper.scheduleRemindersForExpense(context.applicationContext, expense)
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDarkMode(enabled)
        }
    }
}

object RepositoryProvider {
    private var instance: FinanceRepository? = null

    fun initialize(context: Context) {
        if (instance == null) {
            instance = SqliteFinanceRepository(context)
        }
    }

    val repository: FinanceRepository
        get() = instance ?: throw IllegalStateException("RepositoryProvider not initialized. Call initialize(context) first.")
}
