# Repository Guidelines

## Project Structure & Module Organization

`hermes-news` is a Java 21 Spring Boot 3 modular monolith for a personal technology news assistant. Source code lives under `src/main/java/com/hermesnews` and is grouped by feature:

- `news`: RSS and Hacker News collectors, article entities and repositories.
- `ranking`: keyword scoring for technology themes.
- `ai`: summary abstraction with a mock implementation.
- `digest`: daily digest orchestration and manual API endpoint.
- `whatsapp`: Evolution API sender and webhook handling.
- `scheduler`: daily 08:00 `America/Sao_Paulo` job.

Tests live in `src/test/java/com/hermesnews`. Flyway migrations live in `src/main/resources/db/migration`. Postman artifacts live in `postman/` and local automation scripts live in `scripts/`.

## Build, Test, and Development Commands

- `./gradlew clean build`: compile, test and package the app.
- `./gradlew test`: run the full JUnit 5 suite.
- `./gradlew bootRun`: run locally using the default `local` profile.
- `docker compose up -d`: start PostgreSQL, Redis, Evolution API and the app.
- `./scripts/run-postman-local.sh`: run the Postman collection only when `newman` is already installed; do not auto-install npm packages.

Use Gradle only. Do not add Maven or `pom.xml`.

## Coding Style & Naming Conventions

Use Java records for simple DTOs and constructor injection for Spring beans. Keep packages feature-oriented, not layered by technical type. Use 4-space indentation in Java and 2 spaces in YAML. Name tests after the behavior under test, for example `RankingServiceTest` or `WhatsAppWebhookControllerTest`.

## TDD Workflow

Follow red-green-refactor for every change. Write or update the failing test first, run the targeted test, implement the smallest production change, rerun the targeted test, then run `./gradlew test`. After changing config, migrations, or Spring wiring, run the full suite.

## Testing Guidelines

Tests use JUnit 5, AssertJ, Mockito and Spring test slices. The `test` profile uses H2 in PostgreSQL mode with Flyway enabled. Do not require live RSS, Hacker News, LLM or Evolution API access during tests; mock external boundaries.

## Postman Guidelines

Keep `postman/hermes-news.postman_collection.json` and `postman/hermes-news.local.postman_environment.json` aligned with every public endpoint. Use Postman dynamic variables such as `{{$guid}}`, `{{$timestamp}}`, `{{$isoTimestamp}}`, and `{{$randomInt}}` for webhook IDs, timestamps and smoke-test payloads. Secret-like environment values must use placeholders and `type: "secret"` where supported. Do not add scripts that install Newman or other npm tools automatically.

## Security & Configuration

Never commit real API keys, WhatsApp tokens, phone numbers or `.env` files. Use `.env.example` for safe placeholders. Docker Compose runs Evolution API locally on host port `8081` with a local-only placeholder API key; keep `EVOLUTION_RECIPIENT` empty unless intentionally sending a real WhatsApp message.
