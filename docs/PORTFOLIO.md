# Telegram Expense Tracker — Portfolio Demo

**Type:** Self-initiated demo (not a commercial client project)

**Overview:** A Kotlin/JVM Telegram bot that tracks expenses and stores them in a private Google Sheet. It provides chat commands to record expenses, calculate daily/monthly totals, and display recent transactions.

**Tech stack:** Kotlin, Gradle, Telegram Bot API, Google Sheets API, Google service-account authentication, HTTP/JSON, JUnit 5.

**Implementation highlights:**
- Lightweight HTTP adapters for Telegram long polling and Google Sheets append/read.
- Testable command-processing logic separated from infrastructure.
- Input validation (money format, categories, note length) and clear error messages.
- Telegram-user allowlist and idempotency using Telegram update IDs.
- Unit tests for parsing, reporting, multiuser separation, and duplicate updates.

**Limitations:** Runs locally unless deployed; optimized for one bot instance and light usage. The demo is not a financial system or a multi-tenant SaaS product.

**Screenshots/video:** Add after first successful live run; do not fake or invent client results.

**Suggested freelance service:** "I can build a custom Telegram bot that saves customer requests or data to Google Sheets and generates simple reports."
