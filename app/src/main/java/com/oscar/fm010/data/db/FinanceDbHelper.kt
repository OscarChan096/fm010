package com.oscar.fm010.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.oscar.fm010.data.model.ExpenseItem
import com.oscar.fm010.data.model.WalletInfo
import com.oscar.fm010.util.DateUtils

class FinanceDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "finance.db"
        private const val DATABASE_VERSION = 2

        const val TABLE_WALLET = "wallet"
        const val TABLE_EXPENSES = "expenses"
        const val TABLE_SETTINGS = "settings"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_WALLET (" +
                    "id INTEGER PRIMARY KEY CHECK (id = 1)," +
                    "total_money REAL NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE $TABLE_EXPENSES (" +
                    "id TEXT PRIMARY KEY," +
                    "title TEXT NOT NULL," +
                    "amount REAL NOT NULL," +
                    "due_date TEXT NOT NULL," +
                    "cutoff_date TEXT," +
                    "is_credit_card INTEGER NOT NULL," +
                    "category TEXT NOT NULL," +
                    "reminder_days TEXT NOT NULL DEFAULT '5, 1')"
        )
        db.execSQL(
            "CREATE TABLE $TABLE_SETTINGS (" +
                    "id INTEGER PRIMARY KEY CHECK (id = 1)," +
                    "is_dark_mode INTEGER NOT NULL)"
        )

        // Insert initial default wallet
        db.execSQL("INSERT INTO $TABLE_WALLET (id, total_money) VALUES (1, 10000.0)")
        // Insert initial settings (dark mode false)
        db.execSQL("INSERT INTO $TABLE_SETTINGS (id, is_dark_mode) VALUES (1, 0)")

        // Insert initial default expenses
        db.execSQL(
            "INSERT INTO $TABLE_EXPENSES (id, title, amount, due_date, cutoff_date, is_credit_card, category, reminder_days) " +
                    "VALUES ('1', 'Tarjeta de Crédito Bancomer', 2500.0, '20/11', '10/11', 1, 'Tarjetas', '5, 1')"
        )
        db.execSQL(
            "INSERT INTO $TABLE_EXPENSES (id, title, amount, due_date, cutoff_date, is_credit_card, category, reminder_days) " +
                    "VALUES ('2', 'Renta de Departamento', 4500.0, '01/12', NULL, 0, 'Hogar', '5, 1')"
        )
        db.execSQL(
            "INSERT INTO $TABLE_EXPENSES (id, title, amount, due_date, cutoff_date, is_credit_card, category, reminder_days) " +
                    "VALUES ('3', 'Servicio de Luz', 650.0, '25/11', NULL, 0, 'Servicios', '5, 1')"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_EXPENSES ADD COLUMN reminder_days TEXT DEFAULT '5, 1'")
            } catch (_: Exception) {
            }
        }
    }

    fun getWallet(): WalletInfo {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT total_money FROM $TABLE_WALLET WHERE id = 1", null)
        var total = 10000.0
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0)
        }
        cursor.close()
        return WalletInfo(totalMoney = total)
    }

    fun updateWallet(totalMoney: Double) {
        val db = writableDatabase
        db.execSQL("INSERT OR REPLACE INTO $TABLE_WALLET (id, total_money) VALUES (1, ?)", arrayOf(totalMoney))
    }

    fun getExpenses(): List<ExpenseItem> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, title, amount, due_date, cutoff_date, is_credit_card, category, reminder_days FROM $TABLE_EXPENSES", null)
        val rawList = mutableListOf<ExpenseItem>()
        while (cursor.moveToNext()) {
            rawList.add(
                ExpenseItem(
                    id = cursor.getString(0),
                    title = cursor.getString(1),
                    amount = cursor.getDouble(2),
                    dueDate = cursor.getString(3),
                    cutoffDate = cursor.getString(4),
                    isCreditCard = cursor.getInt(5) == 1,
                    category = cursor.getString(6),
                    reminderDays = if (!cursor.isNull(7)) cursor.getString(7) else "5, 1"
                )
            )
        }
        cursor.close()

        val updatedList = mutableListOf<ExpenseItem>()
        for (item in rawList) {
            val updatedItem = DateUtils.updateExpenseDueDateIfNeeded(item)
            if (updatedItem != item) {
                saveExpenseDirectly(updatedItem)
            }
            updatedList.add(updatedItem)
        }
        return updatedList
    }

    fun addExpense(expense: ExpenseItem) {
        val checkedExpense = DateUtils.updateExpenseDueDateIfNeeded(expense)
        saveExpenseDirectly(checkedExpense)
    }

    private fun saveExpenseDirectly(expense: ExpenseItem) {
        val db = writableDatabase
        db.execSQL(
            "INSERT OR REPLACE INTO $TABLE_EXPENSES (id, title, amount, due_date, cutoff_date, is_credit_card, category, reminder_days) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(
                expense.id,
                expense.title,
                expense.amount,
                expense.dueDate,
                expense.cutoffDate ?: "",
                if (expense.isCreditCard) 1 else 0,
                expense.category,
                expense.reminderDays
            )
        )
    }

    fun deleteExpense(id: String) {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_EXPENSES WHERE id = ?", arrayOf(id))
    }

    fun getDarkMode(): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT is_dark_mode FROM $TABLE_SETTINGS WHERE id = 1", null)
        var darkMode = false
        if (cursor.moveToFirst()) {
            darkMode = cursor.getInt(0) == 1
        }
        cursor.close()
        return darkMode
    }

    fun setDarkMode(enabled: Boolean) {
        val db = writableDatabase
        db.execSQL("INSERT OR REPLACE INTO $TABLE_SETTINGS (id, is_dark_mode) VALUES (1, ?)", arrayOf(if (enabled) 1 else 0))
    }
}
