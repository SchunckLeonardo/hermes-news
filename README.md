# hermes-news

Personal technology news assistant built with Java 21 and Spring Boot. It collects technology, AI, backend and cloud news from RSS and Hacker News, ranks items by configured interests, creates a mock AI summary, and sends the digest through Evolution API when WhatsApp credentials are configured.

## Architecture

This is a modular monolith under `com.hermesnews`:

- `news`: RSS parser/collector, Hacker News client, articles and sources.
- `ranking`: keyword-based scoring.
- `ai`: summary abstraction, currently mocked for low-cost local use.
- `digest`: orchestration and `POST /api/digests/send-daily`.
- `whatsapp`: Evolution API send client and `POST /api/whatsapp/webhook`.
- `scheduler`: daily digest at 08:00 in `America/Sao_Paulo`.

Data is stored in PostgreSQL with Flyway migrations. Redis is available for future caching or queueing.

## Running Locally

Create local configuration from the example if you need to override defaults:

```bash
cp .env.example .env
```

Start infrastructure and the app:

```bash
docker compose up -d
```

Run from the host instead:

```bash
./gradlew bootRun
```

Health check:

```bash
curl http://localhost:8080/actuator/health
```

Trigger the digest manually:

```bash
curl -X POST http://localhost:8080/api/digests/send-daily
```

## Gradle Commands

```bash
./gradlew clean build
./gradlew test
./gradlew bootRun
docker compose up -d
```

## Environment Variables

Key variables are documented in `.env.example`:

- `POSTGRES_JDBC_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`
- `RSS_FEEDS`, `HACKER_NEWS_BASE_URL`, `HACKER_NEWS_MAX_ITEMS`
- `RANKING_KEYWORDS`
- `EVOLUTION_BASE_URL`, `EVOLUTION_API_KEY`, `EVOLUTION_INSTANCE`, `EVOLUTION_RECIPIENT`
- `APP_DAILY_DIGEST_CRON`, `APP_SCHEDULER_ZONE`

Do not commit real credentials.

## Evolution API

Set the Evolution variables only when a real instance is ready. Without complete Evolution configuration, WhatsApp sending is skipped safely and the digest endpoint still works.

The sender posts to:

```text
POST /message/sendText/{instance}
```

with `apikey` header and body containing `number` and `text`.

## Testing

The test profile uses H2 in PostgreSQL mode and runs Flyway migrations. External services are mocked in tests.

```bash
./gradlew test
```

## Next Steps

- Replace `MockAiSummaryService` with an OpenAI or alternative LLM adapter.
- Add richer RSS source management in the database.
- Add duplicate detection beyond URL matching.
- Use Redis for digest job locking or cache if the app grows.
