package com.oscar.fm010

import com.oscar.fm010.data.model.ExpenseItem
import com.oscar.fm010.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun testDueDatePasses_updatesToNextMonth() {
        // Given an expense due on 27/10
        val expense = ExpenseItem(
            id = "1",
            title = "Renta",
            amount = 5000.0,
            dueDate = "27/10",
            cutoffDate = null
        )

        // When today is 28/10/2026 (day after due date)
        val today = LocalDate.of(2026, 10, 28)
        val updated = DateUtils.updateExpenseDueDateIfNeeded(expense, today)

        // Then due date should automatically update to 27/11
        assertEquals("27/11", updated.dueDate)
    }

    @Test
    fun testDueDateWithCutoffDate_updatesBothToNextMonth() {
        // Given a credit card expense with cutoff date 05/10 and due date 27/10
        val expense = ExpenseItem(
            id = "2",
            title = "Tarjeta Bancomer",
            amount = 1500.0,
            dueDate = "27/10",
            cutoffDate = "05/10",
            isCreditCard = true
        )

        // When today is 28/10/2026
        val today = LocalDate.of(2026, 10, 28)
        val updated = DateUtils.updateExpenseDueDateIfNeeded(expense, today)

        // Then due date updates to 27/11 and cutoff date updates to 05/11
        assertEquals("27/11", updated.dueDate)
        assertEquals("05/11", updated.cutoffDate)
    }

    @Test
    fun testMultipleMonthsPast_updatesToNextUpcomingMonth() {
        // Given an expense due on 27/10
        val expense = ExpenseItem(
            id = "3",
            title = "Servicio",
            amount = 300.0,
            dueDate = "27/10"
        )

        // When today is 15/01/2027 (almost 3 months later)
        val today = LocalDate.of(2027, 1, 15)
        val updated = DateUtils.updateExpenseDueDateIfNeeded(expense, today)

        // Then due date should jump to 27/01
        assertEquals("27/01", updated.dueDate)
    }

    @Test
    fun testDueDateIsToday_doesNotUpdateYet() {
        // Given an expense due on 27/10
        val expense = ExpenseItem(
            id = "4",
            title = "Luz",
            amount = 400.0,
            dueDate = "27/10"
        )

        // When today is 27/10/2026 (the exact due date)
        val today = LocalDate.of(2026, 10, 27)
        val updated = DateUtils.updateExpenseDueDateIfNeeded(expense, today)

        // Then due date remains 27/10
        assertEquals("27/10", updated.dueDate)
    }

    @Test
    fun testLegacyFullDate_normalizesToDdMm() {
        // Given an expense with legacy format 27/10/2026
        val expense = ExpenseItem(
            id = "5",
            title = "Agua",
            amount = 200.0,
            dueDate = "27/10/2026"
        )

        // When today is 10/10/2026
        val today = LocalDate.of(2026, 10, 10)
        val updated = DateUtils.updateExpenseDueDateIfNeeded(expense, today)

        // Then due date normalizes to dd/mm format 27/10
        assertEquals("27/10", updated.dueDate)
    }

    @Test
    fun testParseReminderDays_defaultAndCustom() {
        val defaultDays = com.oscar.fm010.notification.NotificationHelper.parseReminderDays("5, 1")
        assertEquals(listOf(5, 1), defaultDays)

        val customDays = com.oscar.fm010.notification.NotificationHelper.parseReminderDays("7, 3, 1")
        assertEquals(listOf(7, 3, 1), customDays)

        val emptyDays = com.oscar.fm010.notification.NotificationHelper.parseReminderDays("")
        assertEquals(listOf(5, 1), emptyDays)
    }
}
