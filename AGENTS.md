# Repository Guidelines

## Project Structure & Module Organization

`hermes-news` is a Java 21 Spring Boot 3 modular monolith for a personal technology news assistant. Source code lives under `src/main/java/com/hermesnews` and is grouped by feature:

- `news`: RSS/Atom and Hacker News collectors, HTML feed discovery, managed RSS sources, source health tracking, article entities and repositories.
- `ranking`: keyword scoring for technology themes.
- `preferences`: persisted personal preferences for themes, excluded themes, sources, news count, preferred time and language.
- `ai`: Spring AI/Ollama summary abstraction with timeout and mock/local formatter fallback.
- `agent`: conversational WhatsApp agent with deterministic handling for capabilities, preferences, digest and RSS source commands before using AI.
- `digest`: daily digest orchestration and manual API endpoint.
- `whatsapp`: Evolution API sender and webhook handling; inbound text messages are routed to `agent`.
- `scheduler`: minute-level check that sends the daily digest once when the saved preferred time matches `America/Sao_Paulo`.

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

## Digest Message Format

The WhatsApp digest must stay easy to scan: short `*Hermes News*` header, story count, only non-empty sections, numbered items, `Por que importa`, `Fonte`, and `Link`. Do not expose internal ranking scores in the delivered message. Keep the Ollama prompt and local fallback formatter aligned.

## CI & CodeQL

Use GitHub CodeQL, not SonarQube, for repository code scanning. Keep `.github/workflows/security.yml` configured for Java/Kotlin with a manual Gradle build and `.github/codeql/codeql-config.yml` using `security-extended` plus `security-and-quality`. Do not add SonarQube plugins, workflows or tokens unless explicitly requested.

## Postman Guidelines

Keep `postman/hermes-news.postman_collection.json` and `postman/hermes-news.local.postman_environment.json` aligned with every public endpoint. Use Postman dynamic variables such as `{{$guid}}`, `{{$timestamp}}`, `{{$isoTimestamp}}`, and `{{$randomInt}}` for webhook IDs, timestamps and smoke-test payloads. Secret-like environment values must use placeholders and `type: "secret"` where supported. Do not add scripts that install Newman or other npm tools automatically.

## Security & Configuration

Never commit real API keys, WhatsApp tokens, phone numbers or `.env` files. Use `.env.example` for safe placeholders. Docker Compose runs Evolution API locally on host port `8081` with a local-only placeholder API key; `EVOLUTION_RECIPIENT` is the phone-number destination for scheduled/manual digests and replies, while `EVOLUTION_ALLOWED_SENDER` is the optional inbound allowlist for the WhatsApp agent. If Evolution emits inbound senders as `@lid`, use that exact JID in `EVOLUTION_ALLOWED_SENDER`, not in `EVOLUTION_RECIPIENT`. Deduplicate inbound `messages.upsert` events by `data.key.id` before calling the agent, because Evolution can deliver the same message more than once. Authorized inbound WhatsApp text sends a short processing ACK before calling the agent to reduce perceived latency while Ollama/tool calls run. RSS source commands must validate public `http`/`https` URLs and reject localhost/private-network targets before persistence; trailing chat punctuation such as `:` must be stripped before validation. `NewsSource.name` is the editable source label; source commands may resolve a source by public URL or exact case-insensitive label, for example `teste Akita` or `remova TechCrunch`. The RSS collector may fetch a public HTML page only to discover RSS/Atom links from `<link rel="alternate">` or RSS/Atom/feed anchors, and every discovered URL must pass the same public URL validation before being fetched. Source health is stored on `news_sources` and updated by scheduler/manual source tests with last success, last error, last error message and consecutive failures. Source management endpoints live under `/api/news-sources` and must stay aligned with the Postman collection. Keep RSS response buffering bounded with `RSS_MAX_RESPONSE_SIZE` (`2MB` default); increase it only for trusted large feeds/pages and make oversized responses skip the source instead of failing the scheduler. The local `evoapicloud/evolution-api:v2.1.1` `sendText` endpoint expects `number` plus flat `text`; verify the running container logs before changing this payload contract. Configure inbound message delivery with the per-instance webhook script after `hermes-local` exists; use `EVOLUTION_WEBHOOK_URL=http://app:8080/api/whatsapp/webhook` when the app runs in Compose, or `http://host.docker.internal:8080/api/whatsapp/webhook` when the app runs on the host and Evolution stays in Docker. Keep `EVOLUTION_SESSION_PHONE_VERSION` empty by default so Evolution can resolve a current Baileys version for QR generation. Local AI uses Ollama/qwen3 through Spring AI with `AI_SUMMARY_TIMEOUT`; keep prompts defensive because article content and WhatsApp messages are untrusted input.
