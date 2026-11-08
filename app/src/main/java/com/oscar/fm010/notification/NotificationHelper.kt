package com.oscar.fm010.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.oscar.fm010.MainActivity
import com.oscar.fm010.R
import com.oscar.fm010.data.model.ExpenseItem
import com.oscar.fm010.util.DateUtils
import java.time.ZoneId
import java.util.Locale

object NotificationHelper {
    const val CHANNEL_ID = "expense_reminders_channel"
    const val CHANNEL_NAME = "Recordatorios de Pagos"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de fechas límite de pago de gastos"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun parseReminderDays(reminderDaysStr: String): List<Int> {
        if (reminderDaysStr.isBlank()) return listOf(5, 1)
        val list = reminderDaysStr.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it >= 0 }
            .distinct()
        return if (list.isEmpty()) listOf(5, 1) else list
    }

    fun scheduleRemindersForExpenses(context: Context, expenses: List<ExpenseItem>) {
        for (expense in expenses) {
            scheduleRemindersForExpense(context, expense)
        }
    }

    fun scheduleRemindersForExpense(context: Context, expense: ExpenseItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val daysList = parseReminderDays(expense.reminderDays)
        val dueDate = DateUtils.parseDate(expense.dueDate) ?: return

        for (daysBefore in daysList) {
            val targetDate = dueDate.minusDays(daysBefore.toLong())
            val targetDateTime = targetDate.atTime(9, 0)
            val triggerMillis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (triggerMillis > System.currentTimeMillis()) {
                val pendingIntent = getReminderPendingIntent(context, expense, daysBefore)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    }
                } catch (_: SecurityException) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            }
        }
    }

    fun cancelRemindersForExpense(context: Context, expenseId: String, reminderDaysStr: String = "5, 1") {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val daysList = parseReminderDays(reminderDaysStr)
        for (daysBefore in daysList) {
            val requestCode = getRequestCode(expenseId, daysBefore)
            val intent = Intent(context, ExpenseReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun getReminderPendingIntent(context: Context, expense: ExpenseItem, daysBefore: Int): PendingIntent {
        val intent = Intent(context, ExpenseReminderReceiver::class.java).apply {
            putExtra("expense_id", expense.id)
            putExtra("expense_title", expense.title)
            putExtra("expense_amount", expense.amount)
            putExtra("expense_due_date", expense.dueDate)
            putExtra("days_before", daysBefore)
        }
        val requestCode = getRequestCode(expense.id, daysBefore)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getRequestCode(expenseId: String, daysBefore: Int): Int {
        return (expenseId.hashCode() * 31 + daysBefore) and 0x7FFFFFFF
    }

    @SuppressLint("MissingPermission")
    fun showNotification(
        context: Context,
        expenseId: String,
        title: String,
        amount: Double,
        dueDate: String,
        daysBefore: Int
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            expenseId.hashCode() and 0x7FFFFFFF,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val daysText = when (daysBefore) {
            0 -> "vence hoy"
            1 -> "es mañana (1 día restante)"
            else -> "vence en $daysBefore días"
        }

        val formattedAmount = String.format(Locale.getDefault(), "%.2f", amount)
        val contentText = "Tu pago de $$formattedAmount (límite: $dueDate) $daysText."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_favorite)
            .setContentTitle("⏰ Recordatorio de Pago: $title")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = getRequestCode(expenseId, daysBefore)
        notificationManager.notify(notificationId, notification)
    }
}
