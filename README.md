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

Data is stored in PostgreSQL with Flyway migrations. Redis is available for caching/queueing and is also used by the local Evolution API container.

## Running Locally

Create local configuration from the example if you need to override defaults:

```bash
cp .env.example .env
```

Start infrastructure, Evolution API and the app:

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
- `EVOLUTION_SERVER_URL`, `EVOLUTION_POSTGRES_DB`, `EVOLUTION_POSTGRES_USER`, `EVOLUTION_POSTGRES_PASSWORD`
- `APP_DAILY_DIGEST_CRON`, `APP_SCHEDULER_ZONE`

Do not commit real credentials.

## Evolution API

Docker Compose runs a local Evolution API at:

```text
http://localhost:8081
```

The app container talks to it internally through `http://evolution-api:8080`. Evolution has its own PostgreSQL container and uses Redis database `1`, so it does not share the application schema managed by Flyway.

Local defaults are safe placeholders:

```text
EVOLUTION_API_KEY=change-me-local-only
EVOLUTION_INSTANCE=hermes-local
EVOLUTION_RECIPIENT=
```

Keep `EVOLUTION_RECIPIENT` empty until you want to send a real WhatsApp message. Without a recipient, Hermes News skips WhatsApp sending safely and the digest endpoint still works.

The sender posts to:

```text
POST /message/sendText/{instance}
```

with `apikey` header and body containing `number` and `text`.

## Postman

Import these files into Postman:

- `postman/hermes-news.postman_collection.json`
- `postman/hermes-news.local.postman_environment.json`

The collection includes health, manual digest, webhook, Evolution root and Evolution send-text requests. It uses Postman dynamic variables such as `{{$guid}}`, `{{$timestamp}}`, `{{$isoTimestamp}}`, and `{{$randomInt}}` in pre-request scripts and sample payloads.

Run the collection directly in Postman with the `Hermes News Local` environment selected.

If you already have Newman installed, you can also run:

```bash
./scripts/run-postman-local.sh
```

The script does not install Newman or any npm package automatically.

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
