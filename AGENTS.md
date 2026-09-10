# PROJECT KNOWLEDGE BASE

**Generated:** 2026-09-08
**Commit:** 845334a
**Branch:** main

## OVERVIEW
Micronaut 5.1 / Java 25 microservice in the OpenDonationAssistant ecosystem. Bridges Twitch (EventSub webhooks + Helix API) with the ODA platform via RabbitMQ commands and PostgreSQL persistence. Ships as a GraalVM native image.

## STRUCTURE
```
oda-twitch-service/
├── src/main/java/io/github/opendonationassistant/
│   ├── Application.java          # main() + RabbitMQ @Factory + env pinning
│   ├── integration/twitch/       # Twitch HTTP clients (Helix + ID)
│   └── twitch/
│       ├── commands/             # HTTP wrappers for subscribe/unsubscribe
│       ├── listener/             # RabbitMQ consumers (command + config)
│       │   └── handler/          # typed command handlers
│       ├── metrics/              # Micrometer counters
│       ├── repository/           # Micronaut Data JDBC + facades
│       └── webhook/              # EventSub webhook controller + handlers
├── src/main/resources/
│   ├── application*.yml          # 3 env profiles (standalone forced)
│   └── db/migration/V1..V6       # Flyway, twitch schema
├── src/test/java/                # 2 unit tests (metrics, webhook)
├── pom.xml                       # Micronaut BOM, ErrorProne+NullAway gates
├── aot-{jar,native-image}.properties
├── Dockerfile                    # copies native binary, fedora:44
└── .github/workflows/maven.yml   # delegates to oda-libraries release workflow
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Add an EventSub event handler | `twitch/webhook/` | implement `TwitchEventHandler` |
| Add a command handler | `twitch/listener/handler/` | extend `AbstractMessageHandler<T>` |
| Call Twitch Helix API | `integration/twitch/TwitchClient.java` | facade handles token RPC |
| Persist accounts/webhooks/rewards | `twitch/repository/` | Data + DataRepository + facade |
| Add a DB column/table | `src/main/resources/db/migration/` | next `V{n}__*.sql` |
| Add a metric | `twitch/metrics/TwitchMetrics.java` | Micrometer counters |
| Change RabbitMQ wiring | `Application.java` | `@Factory` declares exchanges/queues |
| Change HTTP endpoints | `twitch/commands/` + `twitch/webhook/` | nested `HttpWrapper` controllers |

## CODE MAP
| Symbol | Type | Location | Refs | Role |
|--------|------|----------|------|------|
| `Application` | class + `@Factory` | root | — | Bootstrap, RabbitMQ wiring, pins `standalone` env |
| `TwitchEventsWebhook` | `@Controller` | twitch/webhook | — | `POST /twitch/events`, challenge handshake, dispatch |
| `TwitchEventHandler` | interface | twitch/webhook | 8 impls | Event dispatch contract |
| `TwitchClient` | `@Singleton` | integration/twitch | many | Twitch API facade, `runWithToken()` |
| `TwitchApiClient` | `@Client("twitch-api")` | integration/twitch | TwitchClient | Declarative Helix client, `@Retryable` |
| `TwitchIdClient` | `@Client("twitch-id")` | integration/twitch | commands | Declarative ID client |
| `CommandListener` | `@RabbitListener` | twitch/listener | — | Consumes `twitch.command` queue |
| `ConfigListener` | `@RabbitListener` | twitch/listener | — | Consumes `twitch.config`, syncs rewards |
| `MessageProcessor` | external (oda-rabbit-conf) | — | handlers | Routes commands to typed handlers |
| `TwitchAccountRepository` | `@Singleton` | twitch/repository | many | Account persistence facade |
| `TwitchWebhookRepository` | `@Singleton` | twitch/repository | SubscribeEventsHandler | Webhook subscription persistence |
| `TwitchRewardRepository` | `@Singleton` | twitch/repository | ConfigListener | Custom reward persistence |
| `TwitchMetrics` | `@Singleton` | twitch/metrics | webhook | Micrometer counters |

## CONVENTIONS
- **Constructor injection only** (`@Inject`), no field injection.
- **DTOs are Java records** annotated `@Serdeable`; nullable fields marked `@Nullable` (JSpecify).
- **NullAway is ERROR** on `io.github.opendonationassistant` (JSpecify mode) — nullness violations fail the build. Root package is `@NullUnmarked`.
- **Data/Repository split**: `XxxData` record + `XxxDataRepository` (Micronaut Data JDBC interface) + `XxxRepository` `@Singleton` facade.
- **Handlers**: webhook handlers implement `TwitchEventHandler`; command handlers extend `AbstractMessageHandler<T>` from oda-rabbit-conf.
- **Flyway migrations**: `V{n}__snake-or-kebab-description.sql`, schema `twitch`.
- **Metric constants**: `UPPER_SNAKE_CASE`, values like `twitch.events.received`.
- **Tests**: plain JUnit 5 + Mockito unit tests (no `@MicronautTest`), `*Test.java`, inline fixtures.
- **Git commits**: lowercase, short imperative ("Add metrics for twitch events").

## ANTI-PATTERNS (THIS PROJECT)
- **Do NOT add field injection** — constructor injection is enforced by convention.
- **Do NOT write null-unsafe code** — NullAway fails the build; annotate `@Nullable` explicitly.
- **Do NOT add a new RabbitMQ exchange/queue in a listener** — declare it in `Application`'s `@Factory`.
- **Do NOT call Twitch APIs directly from handlers** — go through `TwitchClient` (token handling lives there).
- **Do NOT add `@MicronautTest`/Testcontainers tests** — existing tests are context-free unit tests.
- **Do NOT commit `.mvn/wrapper/`, `target/`, `.tmp/`, `.micronaut/`, IDE files** — gitignored.

## UNIQUE STYLES
- `Application.java` doubles as bootstrap AND `@Factory` (RabbitMQ wiring) AND pins `defaultEnvironments("standalone")` via inner `@ContextConfigurer`.
- Nested controller pattern: `SubscribeTwitchEvents`/`UnsubscribeTwitchEvents` are plain classes with `@Controller static class HttpWrapper` inner classes.
- Native-image-first: `-Dpackaging=native-image` produces `target/oda-twitch-service` binary; Dockerfile has no JRE fallback.
- AOT configs deliberately disable JWKS build-time fetch (`micronaut.security.jwks.enabled=false`).
- Rabbit listeners each run on dedicated single-thread executors (`command-listener`, `config-listener`, `event-listener`).

## COMMANDS
```bash
./mvnw test                          # unit tests + JaCoCo
./mvnw verify                        # tests + coverage report
./mvnw clean package -Dpackaging=native-image -DskipTests   # CI native build
./mvnw clean package -Dpackaging=jar # JVM jar build
```

## NOTES
- Requires env vars: `TWITCH_CLIENT_ID`, `TWITCH_CLIENT_SECRET` (required); `JDBC_URL`, `JDBC_USER`, `JDBC_PASSWORD`, `RABBITMQ_HOST` (defaults in `application-standalone.yml`).
- `@Client("twitch-api")`/`@Client("twitch-id")` URLs come from `micronaut.http.services.*.urls` env vars — NOT in repo config; must be set at deploy time.
- Shared logic lives in `oda-rabbit-conf` (0.11.232): `AMQPConfiguration`, `MessageProcessor`, `AbstractMessageHandler`, `TwitchFacade`, `TokenRPC`, `events.twitch` domain.
- CI is externalized: `.github/workflows/maven.yml` delegates to `OpenDonationAssistant/oda-libraries@master`; release tags = GitHub RUN_NUMBER.
- Known TODOs: `TwitchClient.createCustomReward` returns empty 200 on missing account; `TwitchAccountRepository.create` doesn't check duplicates.
- `.mvn/wrapper/` is gitignored — clean checkouts rely on `mvnw` bootstrap.