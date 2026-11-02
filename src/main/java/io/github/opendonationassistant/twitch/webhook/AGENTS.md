# twitch/webhook — EventSub Webhook Handling

**Generated:** 2026-09-08

## OVERVIEW
Receives Twitch EventSub notifications at `POST /twitch/events` (anonymous), answers the challenge handshake, and dispatches to per-event-type handlers.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Add a new event type | `TwitchEventHandler.java` + new `*Handler` | implement interface, `@Singleton` |
| Change dispatch logic | `TwitchEventsWebhook.java` | `canHandle(type)` picks first match |
| Event payload shape | `TwitchEventsWebhook.Event` | nested `@Serdeable` record |
| Handler input | `EventContext.java` | id, account, username, `Optional<Event>` |

## CONVENTIONS
- `TwitchEventsWebhook` injects `List<TwitchEventHandler>`; dispatch = first handler whose `canHandle(subscription.type)` returns true.
- Account resolution: `TwitchAccountRepository.findByTwitchId` on `broadcaster_user_id` (falls back to `to_broadcaster_user_id`) returns a `TwitchAccount`; missing account → `eventNoAccount` metric, no event emitted.
- Handlers emit ODA domain events via `TwitchFacade` (external lib) or ask the resolved `TwitchAccount` for its linked `TwitchReward` — never publish to Rabbit directly.
- Every path updates `TwitchMetrics`: `eventReceived` / `eventHandled` / `eventUnhandled` / `eventNoAccount`.
- `ChannelPointsRedemptionHandler` calls `context.account().reward("music")` and then `TwitchReward.addMedia` on the `event-listener` executor. On failure `TwitchReward` posts the reason through the account's messaging function and cancels the redemption so the viewer's points are returned.

## ANTI-PATTERNS
- Do NOT add a handler without wiring its metric path in `TwitchMetrics`.

## NOTES
- Challenge handshake: `webhook_callback_verification` returns `message.challenge()` as 200 body.
- 8 handlers: follow, cheer, ban, raid, subscription gift/message, points redemption, stream online/offline.
- `@Secured(SecurityRule.IS_ANONYMOUS)` — Twitch signs requests, no JWT here.