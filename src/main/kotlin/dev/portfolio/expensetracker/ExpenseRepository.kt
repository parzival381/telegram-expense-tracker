package dev.portfolio.expensetracker

interface ExpenseRepository {
    fun all(): List<Expense>
    /** Returns false if the Telegram update was already saved. */
    fun add(expense: Expense): Boolean
}

/** Handy for a quick first run and isolated tests. Resets on restart. */
class InMemoryExpenseRepository : ExpenseRepository {
    private val entries = mutableListOf<Expense>()
    override fun all(): List<Expense> = entries.toList()
    override fun add(expense: Expense): Boolean {
        if (entries.any { it.updateId == expense.updateId }) return false
        entries += expense
        return true
    }
}
