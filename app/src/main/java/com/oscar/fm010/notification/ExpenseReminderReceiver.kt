package com.oscar.fm010.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oscar.fm010.data.db.FinanceDbHelper

class ExpenseReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val dbHelper = FinanceDbHelper(context)
            val expenses = dbHelper.getExpenses()
            NotificationHelper.scheduleRemindersForExpenses(context, expenses)
            return
        }

        val expenseId = intent.getStringExtra("expense_id") ?: return
        val title = intent.getStringExtra("expense_title") ?: return
        val amount = intent.getDoubleExtra("expense_amount", 0.0)
        val dueDate = intent.getStringExtra("expense_due_date") ?: ""
        val daysBefore = intent.getIntExtra("days_before", 1)

        NotificationHelper.showNotification(
            context = context,
            expenseId = expenseId,
            title = title,
            amount = amount,
            dueDate = dueDate,
            daysBefore = daysBefore
        )
    }
}
