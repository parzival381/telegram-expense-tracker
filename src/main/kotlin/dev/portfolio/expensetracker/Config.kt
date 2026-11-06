package dev.portfolio.expensetracker

import java.nio.file.Path
import java.time.ZoneId

/** Reads configuration from environment; no credentials are hard-coded. */
data class Config(
    val botToken: String,
    val allowedUserIds: Set<Long>,
    val timeZone: ZoneId,
    val currency: String,
    val storageMode: String,
    val spreadsheetId: String?,
    val serviceAccountPath: Path?,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): Config {
            val token = env["TELEGRAM_BOT_TOKEN"]?.trim().orEmpty()
            require(token.isNotBlank()) { "TELEGRAM_BOT_TOKEN is required" }
            val allowed = env["TELEGRAM_ALLOWED_USER_IDS"].orEmpty()
                .split(',').map { it.trim() }.filter { it.isNotEmpty() }
                .map { it.toLongOrNull() ?: error("Invalid TELEGRAM_ALLOWED_USER_IDS") }.toSet()
            val mode = env["STORAGE_MODE"]?.lowercase()?.trim() ?: "memory"
            require(mode in setOf("memory", "sheets")) { "STORAGE_MODE must be memory or sheets" }
            val currency = (env["CURRENCY"] ?: "EUR").uppercase().trim()
            require(currency.matches(Regex("[A-Z]{3}"))) { "CURRENCY must be a 3-letter code" }
            val id = env["SHEETS_ID"]?.trim()?.takeIf { it.isNotEmpty() }
            val credentials = env["GOOGLE_APPLICATION_CREDENTIALS"]?.trim()
                ?.takeIf { it.isNotEmpty() }?.let(Path::of)
            if (mode == "sheets") {
                require(id != null) { "SHEETS_ID is required for sheets storage" }
                require(credentials != null) { "GOOGLE_APPLICATION_CREDENTIALS is required" }
            }
            return Config(
                token, allowed, ZoneId.of(env["APP_TIME_ZONE"] ?: "Europe/Madrid"),
                currency, mode, id, credentials,
            )
        }
    }
}
