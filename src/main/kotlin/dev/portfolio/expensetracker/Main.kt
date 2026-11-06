package dev.portfolio.expensetracker

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Clock

fun main() {
    val config = Config.fromEnv()
    val mapper = jacksonObjectMapper()
    val repository: ExpenseRepository = if (config.storageMode == "sheets") {
        GoogleSheetsExpenseRepository(
            requireNotNull(config.spreadsheetId), requireNotNull(config.serviceAccountPath), mapper,
        )
    } else InMemoryExpenseRepository()
    val handler = CommandHandler(repository, config.timeZone, config.currency, Clock.systemUTC())
    val telegram = TelegramApi(config.botToken, mapper)
    println("Expense Tracker started in ${config.storageMode} mode. Press Ctrl+C to stop.")
    if (config.allowedUserIds.isEmpty()) {
        println("No allowed users configured. Send /id to the bot, then set TELEGRAM_ALLOWED_USER_IDS and restart.")
    }
    var offset = 0L
    while (!Thread.currentThread().isInterrupted) {
        try {
            for (update in telegram.getUpdates(offset)) {
                val updateId = update.path("update_id").asLong(-1)
                if (updateId < 0) continue
                // Advance after handling each update, so a transient failure in sending a reply
                // cannot cause the same expense to be appended twice in the normal flow.
                try {
                    val message = update.path("message")
                    if (message.path("chat").path("type").asText() != "private") continue
                    val chatId = message.path("chat").path("id").asLong(0)
                    val userId = message.path("from").path("id").asLong(0)
                    val text = message.path("text").asText("")
                    if (chatId == 0L || userId == 0L || !text.startsWith('/')) continue
                    val command = text.substringBefore(' ').substringBefore('@').lowercase()
                    if (userId !in config.allowedUserIds && command != "/id") {
                        telegram.sendMessage(chatId, "Access restricted. Send /id to see your user ID.")
                    } else {
                        telegram.sendMessage(chatId, handler.handle(text, userId, updateId))
                    }
                } catch (e: Exception) {
                    println("Update $updateId failed: ${e.javaClass.simpleName}")
                    val chatId = update.path("message").path("chat").path("id").asLong(0)
                    if (chatId != 0L) {
                        try { telegram.sendMessage(chatId, "Temporary error. Please check /last before retrying.") }
                        catch (_: Exception) { /* Network issue: keep polling. */ }
                    }
                } finally {
                    offset = maxOf(offset, updateId + 1)
                }
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (e: Exception) {
            println("Polling interrupted (${e.javaClass.simpleName}); retrying in 5 seconds.")
            try { Thread.sleep(5_000) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
        }
    }
}
