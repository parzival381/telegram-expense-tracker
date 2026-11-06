package dev.portfolio.expensetracker

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class CommandHandlerTest {
    private val repo = InMemoryExpenseRepository()
    private val clock = Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC)
    private val handler = CommandHandler(repo, ZoneId.of("Europe/Madrid"), "EUR", clock)

    @Test
    fun `tracks multiple expenses and total`() {
        assertContains(handler.handle("/add 12.50 food coffee", 123, 1), "Saved")
        assertContains(handler.handle("/add 5.00 transport bus", 123, 2), "Saved")
        val today = handler.handle("/today", 123, 3)
        assertContains(today, "17.50 EUR")
        assertContains(today, "food: 12.50 EUR")
        assertContains(handler.handle("/month", 123, 4), "17.50 EUR")
        assertContains(handler.handle("/last", 123, 5), "bus")
    }

    @Test
    fun `prevents duplicate updates`() {
        handler.handle("/add 3.00 food tea", 123, 101)
        assertContains(handler.handle("/add 3.00 food tea", 123, 101), "already saved")
        assertEquals(1, repo.all().size)
    }

    @Test
    fun `separates users`() {
        handler.handle("/add 3.00 food tea", 123, 201)
        handler.handle("/add 50.00 food pizza", 999, 202)
        val today = handler.handle("/today", 123, 203)
        assertContains(today, "3.00 EUR")
        assertEquals(false, today.contains("50.00 EUR"))
    }
}
