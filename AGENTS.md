# Repository Guidelines

## Project Structure & Module Organization

`hermes-news` is a Java 21 Spring Boot 3 modular monolith for a personal technology news assistant. Source code lives under `src/main/java/com/hermesnews` and is grouped by feature:

- `news`: RSS and Hacker News collectors, article entities and repositories.
- `ranking`: keyword scoring for technology themes.
- `preferences`: persisted personal preferences for themes, excluded themes, sources, news count, preferred time and language.
- `ai`: Spring AI/Ollama summary abstraction with mock fallback for tests.
- `agent`: conversational WhatsApp agent that interprets free-form requests and selects safe internal actions.
- `digest`: daily digest orchestration and manual API endpoint.
- `whatsapp`: Evolution API sender and webhook handling; inbound text messages are routed to `agent`.
- `scheduler`: daily 08:00 `America/Sao_Paulo` job.

Tests live in `src/test/java/com/hermesnews`. Flyway migrations live in `src/main/resources/db/migration`. Postman artifacts live in `postman/` and local automation scripts live in `scripts/`.

## Build, Test, and Development Commands

- `./gradlew clean build`: compile, test and package the app.
- `./gradlew test`: run the full JUnit 5 suite.
- `./gradlew bootRun`: run locally using the default `local` profile.
- `docker compose up -d`: start PostgreSQL, Redis, Ollama, Evolution API and the app.
- `docker compose exec ollama ollama pull qwen3`: download the local LLM model used by Spring AI.
- `./scripts/configure-evolution-webhook.sh`: configure the `hermes-local` Evolution instance webhook after the instance exists.
- `./scripts/run-postman-local.sh`: run the Postman collection only when `newman` is already installed; do not auto-install npm packages.

Use Gradle only. Do not add Maven or `pom.xml`.

## Coding Style & Naming Conventions

Use Java records for simple DTOs and constructor injection for Spring beans. Keep packages feature-oriented, not layered by technical type. Use 4-space indentation in Java and 2 spaces in YAML. Name tests after the behavior under test, for example `RankingServiceTest` or `WhatsAppWebhookControllerTest`.

## TDD Workflow

Follow red-green-refactor for every change. Write or update the failing test first, run the targeted test, implement the smallest production change, rerun the targeted test, then run `./gradlew test`. After changing config, migrations, or Spring wiring, run the full suite.

## Testing Guidelines

Tests use JUnit 5, AssertJ, Mockito and Spring test slices. The `test` profile uses H2 in PostgreSQL mode with Flyway enabled and `AI_PROVIDER=mock`. Do not require live RSS, Hacker News, Ollama, LLM or Evolution API access during tests; mock external boundaries.

## Postman Guidelines

Keep `postman/hermes-news.postman_collection.json` and `postman/hermes-news.local.postman_environment.json` aligned with every public endpoint. Use Postman dynamic variables such as `{{$guid}}`, `{{$timestamp}}`, `{{$isoTimestamp}}`, and `{{$randomInt}}` for webhook IDs, timestamps and smoke-test payloads. Secret-like environment values must use placeholders and `type: "secret"` where supported. Do not add scripts that install Newman or other npm tools automatically.

## Security & Configuration

Never commit real API keys, WhatsApp tokens, phone numbers or `.env` files. Use `.env.example` for safe placeholders. Docker Compose runs Evolution API locally on host port `8081` with a local-only placeholder API key; `EVOLUTION_RECIPIENT` is the phone-number destination for scheduled/manual digests and replies, while `EVOLUTION_ALLOWED_SENDER` is the optional inbound allowlist for the WhatsApp agent. If Evolution emits inbound senders as `@lid`, use that exact JID in `EVOLUTION_ALLOWED_SENDER`, not in `EVOLUTION_RECIPIENT`. Evolution `sendText` expects `number` plus `textMessage.text`; do not send the old flat `text` field. Configure inbound message delivery with the per-instance webhook script after `hermes-local` exists; use `EVOLUTION_WEBHOOK_URL=http://app:8080/api/whatsapp/webhook` when the app runs in Compose, or `http://host.docker.internal:8080/api/whatsapp/webhook` when the app runs on the host and Evolution stays in Docker. Keep `EVOLUTION_SESSION_PHONE_VERSION` empty by default so Evolution can resolve a current Baileys version for QR generation. Local AI uses Ollama/qwen3 through Spring AI; keep prompts defensive because article content and WhatsApp messages are untrusted input.
