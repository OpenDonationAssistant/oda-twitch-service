# twitch/repository — Persistence Layer

**Generated:** 2026-09-08

## OVERVIEW
Micronaut Data JDBC persistence for Twitch accounts, EventSub webhook subscriptions, and custom rewards. Three layers per entity: `XxxData` record, `XxxDataRepository` (Micronaut Data interface), `XxxRepository` `@Singleton` facade.

## WHERE TO LOOK
| Task | Location |
|------|----------|
| Account persistence | `TwitchAccountData` + `TwitchAccountDataRepository` + `TwitchAccountRepository` |
| Webhook subscriptions | `TwitchWebhook` + `TwitchWebhookRepository` |
| Custom rewards | `TwitchRewardData` + `TwitchRewardDataRepository` + `TwitchReward` + `TwitchRewardRepository` |

## CONVENTIONS
- `XxxData` = `@MappedEntity` record with `@MappedProperty` snake_case columns; `XxxDataRepository` = `@JdbcRepository(dialect = POSTGRES)` extending `CrudRepository`; `XxxRepository` = `@Singleton` facade holding business logic.
- Schema `twitch`, managed by Flyway migrations `V1..V6` in `src/main/resources/db/migration/`.
- Facades are the only classes injected elsewhere — never inject a `*DataRepository` directly.
- Lookups by natural key (twitchId, recipientId, refreshTokenId) return `Optional`.

## ANTI-PATTERNS
- Do NOT bypass the facade to use `*DataRepository` directly.
- Do NOT add columns without a matching Flyway migration.

## NOTES
- Known TODO: `TwitchAccountRepository.create` saves without checking for duplicate accounts.
- `TwitchAccountData` maps to table `accounts`; `twitch_id` is the `@Id`.