package dev.portfolio.expensetracker

import java.math.BigDecimal
import java.time.Instant

/** updateId is used for retry-safe deduplication in this single-instance demo. */
data class Expense(
    val updateId: Long,
    val timestamp: Instant,
    val userId: Long,
    val amount: BigDecimal,
    val category: String,
    val note: String,
    val currency: String,
)

class InvalidExpense(message: String) : IllegalArgumentException(message)

object ExpenseParser {
    /** Example: /add 12.50 food coffee and croissant */
    fun parse(message: String): Triple<BigDecimal, String, String> {
        val parts = message.trim().split(Regex("\\s+"), limit = 4)
        if (parts.size < 3) {
            throw InvalidExpense("Usage: /add 12.50 food coffee and croissant")
        }
        val amount = try {
            BigDecimal(parts[1].replace(',', '.')).setScale(2)
        } catch (_: Exception) {
            throw InvalidExpense("Amount must be a positive number with up to 2 decimal places.")
        }
        if (amount <= BigDecimal.ZERO || amount > BigDecimal("1000000.00")) {
            throw InvalidExpense("Amount must be between 0.01 and 1,000,000.")
        }
        val category = parts[2].lowercase()
        if (!category.matches(Regex("[\\p{L}\\p{N}_-]{2,24}"))) {
            throw InvalidExpense("Category must be 2–24 letters, numbers, underscores or hyphens.")
        }
        val note = parts.getOrNull(3)?.trim().orEmpty()
        if (note.length > 120) throw InvalidExpense("Note is too long (max 120 characters).")
        return Triple(amount, category, note)
    }
}
