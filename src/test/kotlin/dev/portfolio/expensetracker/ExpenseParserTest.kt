package dev.portfolio.expensetracker

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExpenseParserTest {
    @Test
    fun `parses amount category and note`() {
        val (amount, category, note) = ExpenseParser.parse("/add 12.50 Food coffee and croissant")
        assertEquals(BigDecimal("12.50"), amount)
        assertEquals("food", category)
        assertEquals("coffee and croissant", note)
    }

    @Test
    fun `accepts comma decimals and optional note`() {
        val (amount, category, note) = ExpenseParser.parse("/add 5,2 transport")
        assertEquals(BigDecimal("5.20"), amount)
        assertEquals("transport", category)
        assertEquals("", note)
    }

    @Test
    fun `rejects negative and excessive precision`() {
        assertFailsWith<InvalidExpense> { ExpenseParser.parse("/add -1.00 food") }
        assertFailsWith<InvalidExpense> { ExpenseParser.parse("/add 1.234 food") }
        assertFailsWith<InvalidExpense> { ExpenseParser.parse("/add invalid food") }
    }
}
