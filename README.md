# Telegram Expense Tracker Bot (Kotlin)

Небольшой законченный проект для junior backend / API / Telegram automation portfolio.

**Что умеет:** фиксирует расходы через Telegram, показывает расходы за сегодня и месяц, показывает последние пять записей, сохраняет в Google Sheets или (для проверки) во временную память. Поддерживает приватный режим по Telegram user ID и защиту от повторной обработки одного update ID.

## Стек

- Kotlin / JVM 17 / Gradle
- Telegram Bot API (long polling)
- Google Sheets REST API + Google service account
- JDK HttpClient, Jackson JSON, JUnit 5

## Команды

```text
/start
/help
/id
/add 12.50 food coffee
/add 18 transport bus ticket
/add 5,20 food breakfast
/today
/month
/last
```

Категория — одно слово (например `food`, `travel`, `shopping`); описание необязательно. Валюта общая для всего бота, по умолчанию EUR. Команды в этом MVP — на английском.

## 1. Первый запуск без Google Cloud

1. Установи JDK 17 и открой папку проекта в **IntelliJ IDEA** как Gradle project.
2. Telegram → **[@BotFather](https://t.me/BotFather)** → `/newbot` → выбери имя и уникальный username. Получи токен. Никому его не отправляй.
3. Выставь переменные окружения в `Run > Edit Configurations` для задачи `run` (или в терминале перед запуском):

```bash
export TELEGRAM_BOT_TOKEN='YOUR_SECRET_BOT_TOKEN'
export STORAGE_MODE='memory'
export APP_TIME_ZONE='Europe/Madrid'
export CURRENCY='EUR'
```

4. Запусти Gradle task `application > run` в IntelliJ. Если используешь установленный Gradle в терминале — `gradle run`.
5. Открой своего Telegram-бота и отправь `/id`. Он покажет числовой ID твоего аккаунта.
6. Останови бот и добавь в конфигурацию:

```bash
export TELEGRAM_ALLOWED_USER_IDS='YOUR_NUMERIC_ID'
```

7. Запусти снова и проверь `/add 12.50 food coffee`, `/today`, `/month`, `/last`.

**Важно:** при `STORAGE_MODE=memory` данные исчезают после перезапуска программы. Это режим демонстрации, не production.

## 2. Подключить постоянное хранение в Google Sheets

1. Создай новую Google Spreadsheet, например `Expense Tracker Portfolio`.
2. Создай вкладку с точным именем **`Expenses`**.
3. В строке 1 запиши заголовки по столбцам A–G:

```text
Update ID | Timestamp UTC | User ID | Amount | Category | Note | Currency
```

4. Открой [Google Cloud Console](https://console.cloud.google.com/) → создай проект → **APIs & Services > Library** → включи **Google Sheets API**.
5. **IAM & Admin > Service Accounts** → создай отдельный service account с минимальными правами. Для локального демонстрационного проекта допускается создать JSON key (если политика Google Cloud позволяет) и хранить его вне репозитория. Для реального размещения предпочтительна авторизация без долговременного JSON-ключа.
6. В Google Sheets нажми **Share** и добавь **email service account** как **Editor**. Не делай таблицу публичной.
7. Возьми ID таблицы из ссылки: `https://docs.google.com/spreadsheets/d/SPREADSHEET_ID/edit`.
8. Добавь переменные:

```bash
export STORAGE_MODE='sheets'
export SHEETS_ID='SPREADSHEET_ID'
export GOOGLE_APPLICATION_CREDENTIALS='/absolute/path/to/private-service-account.json'
```

9. Перезапусти бот, отправь `/add 12.50 food coffee` и проверь новую строку в Sheets. Попробуй `/today` и `/last` после перезапуска — данные должны сохраниться.

Google API: [`spreadsheets.values.append`](https://developers.google.com/workspace/sheets/api/reference/rest/v4/spreadsheets.values/append).

## 3. Тесты

В IntelliJ: Gradle `verification > test`, либо в терминале:

```bash
gradle test
```

Проверяются парсинг сумм, отклонение некорректного ввода, расчёт итогов, разделение пользователей и дедупликация повторов Telegram update ID. Интеграционные проверки Telegram/Google требуют реальных учётных данных и выполняются вручную.

## 4. Структура

```text
src/main/kotlin/dev/portfolio/expensetracker/
├── Config.kt                       # Env и валидация
├── Main.kt                         # Запуск и long polling
├── Expense.kt                      # Доменная модель + ввод
├── ExpenseRepository.kt            # Интерфейс и in-memory адаптер
├── CommandHandler.kt               # Бизнес-логика команд
├── TelegramApi.kt                  # Telegram HTTP adapter
└── GoogleSheetsExpenseRepository.kt # Google Sheets HTTP adapter
```

## 5. Границы MVP

- Запускается **локально**; бот отвечает, пока программа работает. Для круглосуточной работы нужен сервер/hosting.
- Предназначен для **одного работающего экземпляра**. Дедупликация проверкой строк Sheets не является транзакцией между несколькими инстансами.
- Google Sheets удобно для демонстрации, но не замена полноценной БД при высокой нагрузке.
- Для защиты от посторонних доступен allowlist Telegram user ID. Без allowlist бот отвечает только на `/id` и сообщение о запрете доступа.
- Не публикуй токен, ключ сервисного аккаунта и ссылки на личные финансовые таблицы в GitHub, README или видео.
- Не меняй `CURRENCY` при существующих записях, если не планируешь учитывать отдельные валюты: конвертация не реализована.

## 6. Как показывать на фрилансе

1. Демонстрация Telegram-чата: `/add`, `/today`, `/last`.
2. Демонстрация того, как строка появляется в Google Sheets.
3. Краткий разбор кода: `ExpenseRepository`, тестирование и конфигурация через env.
4. Описание честно маркировать как **self-initiated portfolio demo**, не как заказ клиента.

Английское описание для портфолио — в [`docs/PORTFOLIO.md`](docs/PORTFOLIO.md).
