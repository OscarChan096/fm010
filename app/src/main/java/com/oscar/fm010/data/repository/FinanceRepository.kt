package com.oscar.fm010.data.repository

import android.content.Context
import com.oscar.fm010.data.db.FinanceDbHelper
import com.oscar.fm010.data.model.ExpenseItem
import com.oscar.fm010.data.model.WalletInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface FinanceRepository {
    val wallet: StateFlow<WalletInfo>
    val expenses: StateFlow<List<ExpenseItem>>
    val isDarkMode: StateFlow<Boolean>
    suspend fun updateWallet(totalMoney: Double)
    suspend fun addExpense(expense: ExpenseItem)
    suspend fun deleteExpense(id: String)
    suspend fun updateExpense(expense: ExpenseItem)
    suspend fun setDarkMode(enabled: Boolean)
}

class SqliteFinanceRepository(context: Context) : FinanceRepository {
    private val dbHelper = FinanceDbHelper(context.applicationContext)

    private val _wallet = MutableStateFlow(dbHelper.getWallet())
    override val wallet: StateFlow<WalletInfo> = _wallet.asStateFlow()

    private val _expenses = MutableStateFlow(dbHelper.getExpenses())
    override val expenses: StateFlow<List<ExpenseItem>> = _expenses.asStateFlow()

    private val _isDarkMode = MutableStateFlow(dbHelper.getDarkMode())
    override val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    override suspend fun updateWallet(totalMoney: Double) {
        dbHelper.updateWallet(totalMoney)
        _wallet.value = dbHelper.getWallet()
    }

    override suspend fun addExpense(expense: ExpenseItem) {
        dbHelper.addExpense(expense)
        _expenses.value = dbHelper.getExpenses()
    }

    override suspend fun deleteExpense(id: String) {
        dbHelper.deleteExpense(id)
        _expenses.value = dbHelper.getExpenses()
    }

    override suspend fun updateExpense(expense: ExpenseItem) {
        dbHelper.addExpense(expense)
        _expenses.value = dbHelper.getExpenses()
    }

    override suspend fun setDarkMode(enabled: Boolean) {
        dbHelper.setDarkMode(enabled)
        _isDarkMode.value = dbHelper.getDarkMode()
    }
}
