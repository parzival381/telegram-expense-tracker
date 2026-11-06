package dev.portfolio.expensetracker

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.auth.oauth2.GoogleCredentials
import java.math.BigDecimal
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.Duration

/**
 * One-user demo persistence via Sheets API. Requires a tab named Expenses.
 * Not transaction-safe for multiple simultaneously running bot instances.
 */
class GoogleSheetsExpenseRepository(
    private val spreadsheetId: String,
    keyPath: Path,
    private val mapper: ObjectMapper,
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
) : ExpenseRepository {
    private val credentials: GoogleCredentials = Files.newInputStream(keyPath).use {
        GoogleCredentials.fromStream(it).createScoped("https://www.googleapis.com/auth/spreadsheets")
    }
    private val range = URLEncoder.encode("Expenses!A:G", StandardCharsets.UTF_8)
    private val base = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/$range"

    override fun all(): List<Expense> {
        val json = request("GET", base)
        val rows = json.path("values")
        if (!rows.isArray) return emptyList()
        return rows.mapNotNull { row ->
            if (!row.isArray || row.size() < 7) return@mapNotNull null
            try {
                Expense(
                    updateId = row[0].asText().toLong(),
                    timestamp = Instant.parse(row[1].asText()),
                    userId = row[2].asText().toLong(),
                    amount = BigDecimal(row[3].asText()),
                    category = row[4].asText(),
                    note = row[5].asText(),
                    currency = row[6].asText(),
                )
            } catch (_: Exception) {
                null // Header row or unrelated/invalid row.
            }
        }
    }

    override fun add(expense: Expense): Boolean {
        if (all().any { it.updateId == expense.updateId }) return false
        val payload = mapper.writeValueAsString(
            mapOf("values" to listOf(listOf(
                expense.updateId.toString(), expense.timestamp.toString(),
                expense.userId.toString(), expense.amount,
                expense.category, expense.note, expense.currency,
            )))
        )
        request("POST", "$base:append?valueInputOption=RAW&insertDataOption=INSERT_ROWS", payload)
        return true
    }

    private fun request(method: String, url: String, jsonBody: String? = null): JsonNode {
        synchronized(credentials) { credentials.refreshIfExpired() }
        val token = requireNotNull(credentials.accessToken?.tokenValue) { "Missing Google access token" }
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json; charset=utf-8")
            .method(method, if (jsonBody == null) HttpRequest.BodyPublishers.noBody()
                else HttpRequest.BodyPublishers.ofString(jsonBody))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("Google Sheets API returned HTTP ${response.statusCode()}. Check sharing, tab name and credentials.")
        }
        return mapper.readTree(response.body())
    }
}
