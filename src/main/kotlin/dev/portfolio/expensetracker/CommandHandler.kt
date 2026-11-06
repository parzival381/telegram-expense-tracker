package dev.portfolio.expensetracker

import java.math.BigDecimal
import java.time.Clock
import java.time.YearMonth
import java.time.ZoneId

/** Domain logic separated from Telegram transport for easy unit testing. */
class CommandHandler(
    private val repository: ExpenseRepository,
    private val zoneId: ZoneId,
    private val currency: String,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun handle(message: String, userId: Long, updateId: Long): String {
        val trimmed = message.trim()
        val command = trimmed.substringBefore(' ').substringBefore('@').lowercase()
        return when (command) {
            "/start", "/help" -> """
                Expense Tracker Bot 💸
                /add 12.50 food coffee — save an expense
                /today — today's total
                /month — current month's total
                /last — last 5 expenses
                /id — your Telegram user ID
                Currency: $currency
            """.trimIndent()
            "/id" -> "Your Telegram user ID: $userId"
            "/add" -> {
                try {
                    val (amount, category, note) = ExpenseParser.parse(trimmed)
                    val expense = Expense(updateId, clock.instant(), userId, amount, category, note, currency)
                    if (repository.add(expense)) "Saved: $amount $currency · $category" +
                        if (note.isNotBlank()) " · $note" else ""
                    else "This expense was already saved."
                } catch (e: InvalidExpense) {
                    e.message ?: "Invalid input"
                }
            }
            "/today" -> {
                val today = clock.instant().atZone(zoneId).toLocalDate()
                val expenses = repository.all().filter {
                    it.userId == userId && it.currency == currency &&
                        it.timestamp.atZone(zoneId).toLocalDate() == today
                }
                summary("Today", expenses)
            }
            "/month" -> {
                val month = YearMonth.from(clock.instant().atZone(zoneId))
                val expenses = repository.all().filter {
                    it.userId == userId && it.currency == currency &&
                        YearMonth.from(it.timestamp.atZone(zoneId)) == month
                }
                summary("This month", expenses)
            }
            "/last" -> {
                val expenses = repository.all().filter { it.userId == userId && it.currency == currency }
                    .sortedWith(compareByDescending<Expense> { it.timestamp }.thenByDescending { it.updateId })
                    .take(5)
                if (expenses.isEmpty()) "No expenses yet. Try /add 12.50 food coffee"
                else expenses.joinToString("\n") { e ->
                    "${e.amount} $currency · ${e.category}" +
                        if (e.note.isNotBlank()) " · ${e.note}" else ""
                }
            }
            else -> "Unknown command. Send /help to see available commands."
        }
    }

    private fun summary(label: String, expenses: List<Expense>): String {
        val total = expenses.fold(BigDecimal.ZERO) { sum, it -> sum + it.amount }
        if (expenses.isEmpty()) return "$label: 0.00 $currency (no expenses)"
        val byCategory = expenses.groupBy { it.category }.mapValues { (_, records) ->
            records.fold(BigDecimal.ZERO) { sum, it -> sum + it.amount }
        }.entries.sortedByDescending { it.value }
        return buildString {
            appendLine("$label: ${total.setScale(2)} $currency (${expenses.size} expenses)")
            for ((category, amount) in byCategory) appendLine("• $category: ${amount.setScale(2)} $currency")
        }.trimEnd()
    }
}
