# twitch/repository — Persistence Layer

**Generated:** 2026-09-08

## OVERVIEW
Micronaut Data JDBC persistence for Twitch accounts, EventSub webhook subscriptions, and custom rewards. Accounts and webhook subscriptions use `XxxData` + `XxxDataRepository` + `XxxRepository`; reward rows are read/written through `TwitchRewardDataRepository` directly and wrapped by the account/reward domain objects.

## WHERE TO LOOK
| Task | Location |
|------|----------|
| Account persistence | `TwitchAccountData` + `TwitchAccountDataRepository` |
| Linked account domain object | `TwitchAccount` (returns `Optional<TwitchAccount>` from `TwitchAccountRepository`) |
| Webhook subscriptions | `TwitchWebhook` + `TwitchWebhookRepository` |
| Custom rewards | `TwitchRewardData` + `TwitchRewardDataRepository` + `TwitchReward` |

## CONVENTIONS
- `XxxData` = `@MappedEntity` record with `@MappedProperty` snake_case columns; `XxxDataRepository` = `@JdbcRepository(dialect = POSTGRES)` extending `CrudRepository`; `XxxRepository` = `@Singleton` facade holding business logic.
- Schema `twitch`, managed by Flyway migrations `V1..V6` in `src/main/resources/db/migration/`.
- Account/webhook facades are the classes injected elsewhere; reward rows are the exception and are accessed through `TwitchRewardDataRepository` directly.
- Lookups by natural key (twitchId, recipientId, refreshTokenId) return `Optional`.

## ANTI-PATTERNS
- Do NOT bypass `TwitchAccountRepository`/`TwitchWebhookRepository` (reward rows use `TwitchRewardDataRepository` directly by design).
- Do NOT add columns without a matching Flyway migration.

## NOTES
- `TwitchAccount` is the account aggregate: it owns the account's chat/refund calls and resolves its linked reward via `reward(type)`. It constructs `TwitchReward` with the account-bound `Messaging` and `RedemptionRefund` functions, so the reward never looks the account up again.
- `TwitchReward` is a domain object, not a row wrapper: it sends the `media.add` RPC and, when that fails, sends the failure reason through its `Messaging` function and cancels the redemption through its `RedemptionRefund` function (`CANCELED`, returns channel points).
- `TwitchRewardDataRepository` is used directly by `TwitchAccount` (lookup) and `ConfigListener` (create/find by widget); reward objects are created by `TwitchAccount`.
- Known TODO: `TwitchAccountRepository.create` saves without checking for duplicate accounts.
- `TwitchAccountData` maps to table `accounts`; `twitch_id` is the `@Id`.