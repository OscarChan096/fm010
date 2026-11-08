package com.oscar.fm010.util

import com.oscar.fm010.data.model.ExpenseItem
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.min

object DateUtils {
    private val outputFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")

    fun parseDate(dateStr: String, today: LocalDate = LocalDate.now()): LocalDate? {
        if (dateStr.isBlank()) return null
        val trimmed = dateStr.trim()
        return try {
            val parts = trimmed.split("/")
            when (parts.size) {
                2 -> {
                    val day = parts[0].toIntOrNull() ?: return null
                    val month = parts[1].toIntOrNull() ?: return null
                    var year = today.year
                    if (month > today.monthValue) {
                        year = today.year - 1
                    }
                    LocalDate.of(year, month, day)
                }
                3 -> {
                    val day = parts[0].toIntOrNull() ?: return null
                    val month = parts[1].toIntOrNull() ?: return null
                    var year = parts[2].toIntOrNull() ?: return null
                    if (year < 100) year += 2000
                    LocalDate.of(year, month, day)
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun formatDate(date: LocalDate): String {
        return date.format(outputFormatter)
    }

    /**
     * Actualiza automáticamente la fecha límite de pago (y la fecha de corte si aplica)
     * sumando un mes a la vez hasta que la fecha límite no haya vencido (sea >= hoy).
     *
     * Ejemplo:
     * - Fecha límite agregada: "27/10"
     * - Cuando pasa ese día (ej. hoy es 28/10):
     * - Se actualiza automáticamente a "27/11".
     */
    fun updateExpenseDueDateIfNeeded(expense: ExpenseItem, today: LocalDate = LocalDate.now()): ExpenseItem {
        val currentDueDate = parseDate(expense.dueDate, today) ?: return expense

        if (!currentDueDate.isBefore(today)) {
            val formattedCurrentDue = formatDate(currentDueDate)
            val formattedCurrentCutoff = expense.cutoffDate?.let { cutoff ->
                parseDate(cutoff, today)?.let { formatDate(it) } ?: cutoff
            }
            if (formattedCurrentDue != expense.dueDate || formattedCurrentCutoff != expense.cutoffDate) {
                return expense.copy(
                    dueDate = formattedCurrentDue,
                    cutoffDate = formattedCurrentCutoff
                )
            }
            return expense
        }

        var updatedDueDate = currentDueDate
        val originalDueDay = currentDueDate.dayOfMonth
        var monthsAdded = 0

        while (updatedDueDate.isBefore(today)) {
            monthsAdded++
            val nextYearMonth = YearMonth.from(currentDueDate).plusMonths(monthsAdded.toLong())
            val newDay = min(originalDueDay, nextYearMonth.lengthOfMonth())
            updatedDueDate = nextYearMonth.atDay(newDay)
        }

        val newDueDateStr = formatDate(updatedDueDate)

        val newCutoffDateStr = expense.cutoffDate?.let { cutoffStr ->
            val currentCutoffDate = parseDate(cutoffStr, today)
            if (currentCutoffDate != null) {
                val originalCutoffDay = currentCutoffDate.dayOfMonth
                val nextCutoffYearMonth = YearMonth.from(currentCutoffDate).plusMonths(monthsAdded.toLong())
                val newCutoffDay = min(originalCutoffDay, nextCutoffYearMonth.lengthOfMonth())
                formatDate(nextCutoffYearMonth.atDay(newCutoffDay))
            } else {
                cutoffStr
            }
        }

        return expense.copy(
            dueDate = newDueDateStr,
            cutoffDate = newCutoffDateStr
        )
    }
}
