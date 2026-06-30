# hermes-news

Personal technology news assistant built with Java 21 and Spring Boot. It collects technology, AI, backend and cloud news from RSS and Hacker News, ranks items by configured interests, summarizes with Spring AI and local Ollama/qwen3, and sends the digest through Evolution API when WhatsApp is configured.

## Architecture

This is a modular monolith under `com.hermesnews`:

- `news`: RSS parser/collector, Hacker News client, articles and sources.
- `ranking`: keyword-based scoring.
- `ai`: Spring AI abstraction for Ollama/qwen3 with a mock fallback in tests.
- `agent`: WhatsApp conversational agent that chooses safe internal actions.
- `digest`: orchestration and `POST /api/digests/send-daily`.
- `whatsapp`: Evolution API send client and `POST /api/whatsapp/webhook`; inbound text is sent to the agent.
- `scheduler`: daily digest at 08:00 in `America/Sao_Paulo`.

Data is stored in PostgreSQL with Flyway migrations. Redis is available for caching/queueing and is also used by the local Evolution API container. Ollama runs locally through Docker Compose for low-cost LLM use.

## Running Locally

Create local configuration from the example if you need to override defaults:

```bash
cp .env.example .env
```

Start infrastructure, Evolution API and the app:

```bash
docker compose up -d
```

Pull the local model before expecting real AI summaries:

```bash
docker compose exec ollama ollama pull qwen3
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
- `AI_PROVIDER`, `OLLAMA_BASE_URL`, `OLLAMA_MODEL`, `OLLAMA_TEMPERATURE`
- `RSS_FEEDS`, `HACKER_NEWS_BASE_URL`, `HACKER_NEWS_MAX_ITEMS`
- `RANKING_KEYWORDS`
- `EVOLUTION_BASE_URL`, `EVOLUTION_API_KEY`, `EVOLUTION_INSTANCE`, `EVOLUTION_RECIPIENT`
- `EVOLUTION_SERVER_URL`, `EVOLUTION_POSTGRES_DB`, `EVOLUTION_POSTGRES_USER`, `EVOLUTION_POSTGRES_PASSWORD`
- `APP_DAILY_DIGEST_CRON`, `APP_SCHEDULER_ZONE`

Do not commit real credentials.

## Local AI with Ollama

The local profile defaults to:

```text
AI_PROVIDER=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen3
```

When the app runs inside Docker Compose, it talks to Ollama through `http://ollama:11434`. If Ollama or `qwen3` is unavailable, the digest falls back to the local formatter instead of failing the whole flow.

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

To connect WhatsApp locally, open:

```text
http://localhost:8081/manager/
```

Use the local API key `change-me-local-only` if prompted, create or select the `hermes-local` instance, then scan the QR code. Docker Compose overrides Evolution's bundled WhatsApp Web version with `EVOLUTION_SESSION_PHONE_VERSION=` so Evolution can resolve a current Baileys version; leave this empty unless you intentionally need to pin a version.

## Postman

Import these files into Postman:

- `postman/hermes-news.postman_collection.json`
- `postman/hermes-news.local.postman_environment.json`

The collection includes health, manual digest, webhook, Evolution root and Evolution send-text requests. It uses Postman dynamic variables such as `{{$guid}}`, `{{$timestamp}}`, `{{$isoTimestamp}}`, and `{{$randomInt}}` in pre-request scripts and sample payloads.

The default webhook request uses `fromMe: true` so it only records the event and does not trigger an agent reply to a fake number. To test the conversational agent through Postman, set `fromMe` to `false` and use a real `remoteJid`.

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

- Add richer RSS source management in the database.
- Add duplicate detection beyond URL matching.
- Add persisted personal preferences for agent-driven topics, sources and schedule changes.
- Add an optional hosted LLM adapter for deployment outside the local Ollama setup.
- Use Redis for digest job locking or cache if the app grows.
