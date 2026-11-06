package dev.portfolio.expensetracker

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Minimal Telegram Bot API adapter using the JDK HTTP client. */
class TelegramApi(
    token: String,
    private val mapper: ObjectMapper,
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
) {
    private val base = "https://api.telegram.org/bot$token/"

    fun getUpdates(offset: Long): List<JsonNode> {
        val payload = mapper.writeValueAsString(mapOf(
            "offset" to offset, "timeout" to 20, "allowed_updates" to listOf("message"),
        ))
        val response = call("getUpdates", payload, 35)
        return response.path("result").takeIf { it.isArray }?.toList().orEmpty()
    }

    fun sendMessage(chatId: Long, text: String) {
        val payload = mapper.writeValueAsString(mapOf("chat_id" to chatId, "text" to text))
        call("sendMessage", payload, 15)
    }

    private fun call(endpoint: String, body: String, timeoutSeconds: Long): JsonNode {
        val request = HttpRequest.newBuilder(URI.create("$base$endpoint"))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("Content-Type", "application/json; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            // Do not log the URL because it contains the bot token.
            throw IllegalStateException("Telegram API returned HTTP ${response.statusCode()} ($endpoint)")
        }
        val json = mapper.readTree(response.body())
        if (!json.path("ok").asBoolean(false)) throw IllegalStateException("Telegram API returned ok=false ($endpoint)")
        return json
    }
}
