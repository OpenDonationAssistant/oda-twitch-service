# integration/twitch — Twitch HTTP Clients

**Generated:** 2026-09-08

## OVERVIEW
Declarative Micronaut HTTP clients for the Twitch Helix API (`twitch-api`) and ID endpoint (`twitch-id`), wrapped by the `TwitchClient` facade that handles OAuth token acquisition via Rabbit RPC.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Call Helix API | `TwitchApiClient.java` | declarative, `@Retryable` |
| Call ID endpoint | `TwitchIdClient.java` | token/validation |
| Token handling / facade | `TwitchClient.java` | `runWithToken()` + `getAppToken()` |

## CONVENTIONS
- All outbound Twitch calls go through `TwitchClient`; it obtains a user token via `TokenRPC` (Rabbit RPC) then invokes the declarative client.
- `runWithToken(recipientId, refreshTokenId, fn)` fails with a `Problem` "Unauthorized" when the token RPC returns null.
- App-level calls (`subscribe`, `getSubscriptions`, `deleteSubscription`) use `getAppToken()` (client_credentials grant).
- `@Client("twitch-api")`/`@Client("twitch-id")` base URLs come from `micronaut.http.services.*.urls` env vars — set at deploy time, not in repo config.

## ANTI-PATTERNS
- Do NOT call `TwitchApiClient`/`TwitchIdClient` directly from handlers — always via `TwitchClient`.

## NOTES
- Known TODO: `createCustomReward` returns an empty 200 `DataWrapper` when the account is missing instead of throwing.
- `TwitchApiClient` is the largest file in the project (~260 LOC) — keep new endpoints declarative, no imperative HTTP.