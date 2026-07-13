# hermes-news

Personal technology news assistant built with Java 21 and Spring Boot. It collects technology, AI, backend and cloud news from RSS and Hacker News, ranks items by configured interests, summarizes with Spring AI and local Ollama/qwen3, and sends the digest through Evolution API when WhatsApp is configured.

## Architecture

This is a modular monolith under `com.hermesnews`:

- `news`: RSS/Atom parser and collector, HTML feed discovery, Hacker News client, articles and managed RSS sources.
- `ranking`: explainable scoring, persisted feedback signals and semantic event clustering.
- `feedback`: positive/negative feedback linked to items from the latest digest.
- `preferences`: persisted personal preferences for themes, sources, news count, preferred time and language.
- `history`: bounded text search over previously delivered articles.
- `watchlist`: urgent topic monitoring with freshness, score threshold, URL deduplication and cooldown.
- `ai`: Spring AI abstraction for Ollama/qwen3 with timeout and local formatter fallback.
- `agent`: WhatsApp conversational agent with deterministic handling for core commands before using AI.
- `digest`: orchestration and `POST /api/digests/send-daily`.
- `whatsapp`: Evolution API gateway, durable outbox with retries and `POST /api/whatsapp/webhook`.
- `observability`: Micrometer counters for collection, digest, watchlist and outbox attempts.
- `scheduler`: daily digest check, urgent watchlist scan and outbox retry processing.

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

## CI and Security Review

GitHub Actions uses:

- `.github/workflows/ci.yml`: runs `./gradlew clean build` on pushes and pull requests.
- `.github/workflows/security.yml`: runs GitHub CodeQL for Java/Kotlin and dependency review for pull requests.
- `.github/codeql/codeql-config.yml`: enables the `security-extended` and `security-and-quality` CodeQL query suites for `src/main/java`.

CodeQL runs with a manual Gradle build so Spring Boot, annotation processing and dependency resolution are analyzed from the compiled project. No external SonarQube token or hosted scanner is required.

## Environment Variables

Key variables are documented in `.env.example`:

- `POSTGRES_JDBC_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`
- `AI_PROVIDER`, `OLLAMA_BASE_URL`, `OLLAMA_MODEL`, `OLLAMA_TEMPERATURE`, `AI_SUMMARY_TIMEOUT`
- `RSS_FEEDS`, `RSS_MAX_RESPONSE_SIZE`, `HACKER_NEWS_BASE_URL`, `HACKER_NEWS_MAX_ITEMS`
- `RANKING_KEYWORDS`, `RANKING_OFFICIAL_SOURCES`, `RANKING_PRIORITY_ENTITIES`, `RANKING_LAUNCH_KEYWORDS`
- `WATCHLIST_SCAN_CRON`, `WATCHLIST_DEFAULT_COOLDOWN`, `WATCHLIST_MAX_ARTICLE_AGE`, `WATCHLIST_MIN_SCORE`
- `OUTBOX_RETRY_CRON`, `OUTBOX_MAX_ATTEMPTS`, `OUTBOX_BASE_DELAY`
- `EVOLUTION_BASE_URL`, `EVOLUTION_API_KEY`, `EVOLUTION_INSTANCE`, `EVOLUTION_RECIPIENT`
- `EVOLUTION_SERVER_URL`, `EVOLUTION_POSTGRES_DB`, `EVOLUTION_POSTGRES_USER`, `EVOLUTION_POSTGRES_PASSWORD`
- `APP_DIGEST_CHECK_CRON`, `APP_SCHEDULER_ZONE`

Do not commit real credentials.

## Official Sources and Ranking

The default RSS list includes the official OpenAI news feed:

```text
https://openai.com/news/rss.xml
```

Ranking evaluates saved preferences, feedback from earlier digests and these configured signals:

- official source domains from `RANKING_OFFICIAL_SOURCES`, including OpenAI, Anthropic, Google DeepMind, AWS, Google Cloud, Azure, Spring, OpenJDK and Kubernetes defaults;
- recent articles from the last 24 hours, 72 hours and 7 days;
- priority entities from `RANKING_PRIORITY_ENTITIES`, such as `openai`, `sol`, `terra` and `luna`;
- launch signals from `RANKING_LAUNCH_KEYWORDS`, such as `announces`, `launch`, `release` and `preview`.

This makes official launch news, for example an OpenAI announcement about Sol, Terra and Luna, rank above generic posts with many broad technology keywords.

Every selected digest item stores its score explanation. Articles about the same event are clustered by normalized title entities and launch semantics before the digest limit is applied; the highest-ranked representative is kept. The displayed item order is preserved so commands such as `gostei da noticia 2` always refer to the visible item 2.

## Local AI with Ollama

The local profile defaults to:

```text
AI_PROVIDER=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen3
```

When the app runs inside Docker Compose, it talks to Ollama through `http://ollama:11434`. If Ollama or `qwen3` is unavailable or slower than `AI_SUMMARY_TIMEOUT`, the digest falls back to the local formatter instead of failing the whole flow.

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
EVOLUTION_ALLOWED_SENDER=
EVOLUTION_WEBHOOK_URL=http://app:8080/api/whatsapp/webhook
```

`EVOLUTION_RECIPIENT` must be the phone number that receives scheduled/manual digests, using country code and digits only, for example `5511999999999`. `EVOLUTION_ALLOWED_SENDER` is optional and controls who can trigger the WhatsApp agent. If Evolution emits inbound senders as `@lid`, put that exact JID in `EVOLUTION_ALLOWED_SENDER`, for example `24155080654903@lid`, and keep `EVOLUTION_RECIPIENT` as the real phone number. If `EVOLUTION_ALLOWED_SENDER` is empty, the app falls back to `EVOLUTION_RECIPIENT` for the allowlist.

The sender posts to:

```text
POST /message/sendText/{instance}
```

with `apikey` header and body containing `number` and `text` for the local `evoapicloud/evolution-api:v2.1.1` image.

To connect WhatsApp locally, open:

```text
http://localhost:8081/manager/
```

Use the local API key `change-me-local-only` if prompted, create or select the `hermes-local` instance, then scan the QR code with the WhatsApp account that should behave as Hermes News. Docker Compose overrides Evolution's bundled WhatsApp Web version with `EVOLUTION_SESSION_PHONE_VERSION=` so Evolution can resolve a current Baileys version; leave this empty unless you intentionally need to pin a version.

After the instance exists, configure its webhook:

```bash
./scripts/configure-evolution-webhook.sh
```

The default webhook URL is from the Evolution container to the app container:

```text
http://app:8080/api/whatsapp/webhook
```

If you run the app from IntelliJ or `./gradlew bootRun` on the host while Evolution stays in Docker, configure the instance with the host URL instead:

```bash
EVOLUTION_WEBHOOK_URL=http://host.docker.internal:8080/api/whatsapp/webhook ./scripts/configure-evolution-webhook.sh
```

To confirm delivery, send a WhatsApp message to the connected number and watch:

```bash
docker compose logs -f app evolution-api
```

## Agent and Preferences

The agent supports only these actions:

- Generate and send the daily technology digest.
- Answer direct questions about what the agent can do.
- Update personal preferences for themes, sources, news count, preferred time and language.
- Show saved preferences without calling the LLM.
- Add, label, list, test, enable and disable public RSS source URLs.
- Use saved themes, excluded themes, preferred sources, RSS sources, news count and preferred time in ranking/digest generation.
- Record positive or negative feedback about an item from the latest digest and use similar feedback in future ranking.
- Explain why an item was selected using the persisted ranking signals.
- Add, remove and list urgent watchlist terms.
- Search previously delivered articles from today, the current week or the last 30 days.

RSS sources can be direct RSS/Atom URLs or public HTML pages that expose a feed with `<link rel="alternate">` or a visible RSS/Atom/feed anchor. URLs received from WhatsApp have common trailing punctuation removed before validation, so `https://example.com/blog/:` is stored as `https://example.com/blog/`. Discovered feed URLs still pass the public `http`/`https` validation before the app fetches them. `RSS_MAX_RESPONSE_SIZE` defaults to `2MB`; increase it only for trusted large feeds/pages. Source health is persisted with last success, last error, last error message and consecutive failure count.

Source management endpoints:

```text
GET  /api/news-sources
POST /api/news-sources/rss
POST /api/news-sources/test
POST /api/news-sources/label
POST /api/news-sources/enable
POST /api/news-sources/disable
```

All `POST /api/news-sources/*` endpoints use a JSON body like:

```json
{ "url": "https://hnrss.org/frontpage" }
```

To edit a source label, use:

```json
{ "url": "https://hnrss.org/frontpage", "name": "Hacker News" }
```

Example preference commands:

```text
quero mais noticias de Java e menos frontend
quero 7 noticias por dia
priorize InfoQ e Hacker News
quais sao minhas preferencias?
quais fontes estao ativas?
adicione fonte https://news.ycombinator.com/rss
adicione fonte https://akitaonrails.com/en/
renomeie fonte https://akitaonrails.com/en/ para Akita
teste a fonte https://hnrss.org/frontpage
teste Akita
remova TechCrunch
desative fonte https://example.com/feed
me envie o digest
gostei da noticia 2
nao gostei da noticia 4
por que a noticia 2 foi selecionada?
monitore OpenAI
o que voce esta monitorando?
pare de monitorar OpenAI
o que saiu sobre OpenAI esta semana?
busque noticias sobre Java hoje
```

The scheduler checks every minute by default and sends the digest once per day when the saved preferred time matches `APP_SCHEDULER_ZONE`. The urgent watchlist scans every ten minutes by default, only considers recent articles above `WATCHLIST_MIN_SCORE`, and applies a six-hour cooldown per term.

## Delivery Reliability and Metrics

Every digest, agent reply, processing ACK and urgent alert is persisted in `whatsapp_outbox` before the Evolution API call. Failed messages retry with exponential backoff until `OUTBOX_MAX_ATTEMPTS`; missing local credentials produce `SKIPPED` and are not retried.

Actuator exposes custom metrics after the corresponding flow runs:

```text
GET /actuator/metrics/hermes.digest.runs
GET /actuator/metrics/hermes.news.collected
GET /actuator/metrics/hermes.watchlist.alerts
GET /actuator/metrics/hermes.whatsapp.outbox.attempts
```

## Digest Format

The digest is formatted for quick WhatsApp reading with a short header, the number of fresh stories, only non-empty sections, and numbered items:

```text
*Hermes News*
Digest de tecnologia

2 noticias novas selecionadas.

*IA*
1. *LLM routing patterns for production apps*
Por que importa: Teams are using routing to reduce cost and improve reliability in AI features.
Fonte: InfoQ
Link: https://example.com/llm-routing
```

Sections may be `IA`, `Java`, `Backend`, `Cloud` and `Outras`. Each item includes title, practical reason, source and original link; internal ranking scores are not shown. URLs already saved in `articles` are skipped so the same article is not sent again.

## Postman

Import these files into Postman:

- `postman/hermes-news.postman_collection.json`
- `postman/hermes-news.local.postman_environment.json`

The collection includes health, metrics, manual digest, source management, webhook, conversational agent commands, Evolution root and Evolution send-text requests. It uses Postman dynamic variables such as `{{$guid}}`, `{{$timestamp}}`, `{{$isoTimestamp}}`, and `{{$randomInt}}` in pre-request scripts and sample payloads.

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

- Add an optional hosted LLM adapter for deployment outside the local Ollama setup.
- Add retention rules for old webhook payloads, outbox messages and historical articles.
- Add a small offline relevance dataset to compare ranking changes with Precision@K.
- Add source health summaries to the daily digest when a preferred source keeps failing.
- Add delayed ACK instead of always sending the WhatsApp processing message.
