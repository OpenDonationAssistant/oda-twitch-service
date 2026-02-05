package io.github.opendonationassistant.twitch.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import io.github.opendonationassistant.events.twitch.TwitchFacade;
import io.github.opendonationassistant.events.twitch.events.TwitchChannelCheerEvent;
import io.github.opendonationassistant.events.twitch.events.TwitchChannelFollowEvent;
import io.github.opendonationassistant.events.twitch.events.TwitchChannelSubscriptionGiftEvent;
import io.github.opendonationassistant.events.twitch.events.TwitchChannelSubscriptionMessageEvent;
import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

@Controller
public class TwitchEventsWebhook {

  private final TwitchFacade facade;
  private final TimeBasedEpochGenerator uuid =
    Generators.timeBasedEpochGenerator();
  private final TwitchAccountRepository repository;

  @Inject
  public TwitchEventsWebhook(
    TwitchFacade facade,
    TwitchAccountRepository repository
  ) {
    this.facade = facade;
    this.repository = repository;
  }

  @Post("/twitch/events")
  @Secured(SecurityRule.IS_ANONYMOUS)
  public CompletableFuture<HttpResponse<String>> twitchWebhook(
    @Header("Twitch-Eventsub-Message-Type") String type,
    @Body Message message
  ) {
    switch (type) {
      case "webhook_callback_verification":
        return CompletableFuture.completedFuture(
          HttpResponse.ok(message.challenge)
        );
      case "notification":
        return Optional.ofNullable(
          message.subscription().condition().get("broadcaster_user_id")
        )
          .flatMap(userId -> repository.findByTwitchId(userId))
          .map(account -> {
            final Optional<Event> event = Optional.ofNullable(message.event());
            String username = event.map(it -> it.userName()).orElse("");
            var id = uuid.generate().toString();
            switch (message.subscription.type) {
              case "channel.follow":
                var timestamp = event
                  .map(TwitchEventsWebhook.Event::timestamp)
                  .map(Instant::parse)
                  .orElse(Instant.now());
                return facade.sendEvent(
                  new TwitchChannelFollowEvent(
                    id,
                    account.recipientId(),
                    username,
                    timestamp
                  )
                );
              case "channel.subscription.gift":
                return facade.sendEvent(
                  new TwitchChannelSubscriptionGiftEvent(
                    id,
                    account.recipientId(),
                    username,
                    event.map(TwitchEventsWebhook.Event::tier).orElse(""),
                    event.map(TwitchEventsWebhook.Event::total).orElse(0),
                    event
                      .map(TwitchEventsWebhook.Event::cumulativeTotal)
                      .orElse(0)
                  )
                );
              case "channel.subscription.message":
                return facade.sendEvent(
                  new TwitchChannelSubscriptionMessageEvent(
                    id,
                    account.recipientId(),
                    username,
                    event.map(TwitchEventsWebhook.Event::tier).orElse(""),
                    new TwitchChannelSubscriptionMessageEvent.Message(
                      String.valueOf(
                        event
                          .map(it -> (Map<String, Object>) it.message())
                          .map(it -> (String) it.get("text"))
                          .orElse("")
                      ),
                      List.of()
                    ),
                    event.map(TwitchEventsWebhook.Event::total).orElse(0),
                    event
                      .map(TwitchEventsWebhook.Event::cumulativeTotal)
                      .orElse(0),
                    event.map(TwitchEventsWebhook.Event::streakMonths).orElse(0)
                  )
                );
              case "channel.cheer":
                return facade.sendEvent(
                  new TwitchChannelCheerEvent(
                    id,
                    account.recipientId(),
                    username,
                    event
                      .map(TwitchEventsWebhook.Event::message)
                      .map(String::valueOf)
                      .orElse(""),
                    event
                      .map(TwitchEventsWebhook.Event::bits)
                      .map(String::valueOf)
                      .orElse("0")
                  )
                );
              default:
                return CompletableFuture.completedFuture(null);
            }
          })
          .orElseGet(() -> CompletableFuture.completedFuture(null))
          .thenApply(response -> HttpResponse.ok(""));
      default:
        return CompletableFuture.completedFuture(HttpResponse.ok(""));
    }
  }

  @Serdeable
  public static record Message(
    @Nullable String challenge,
    Subscription subscription,
    @Nullable Event event
  ) {}

  @Serdeable
  public static record Subscription(
    String type,
    Map<String, String> condition
  ) {}

  @Serdeable
  public static record Event(
    @Nullable @JsonProperty("user_name") String userName,
    @Nullable @JsonProperty("tier") String tier,
    @Nullable @JsonProperty("is_gift") Boolean isGift,
    @Nullable @JsonProperty("is_anonymous") Boolean isAnonymous,
    @Nullable @JsonProperty("message") Object message,
    @Nullable @JsonProperty("bits") Integer bits,
    @Nullable @JsonProperty("followed_at") String timestamp,
    @Nullable @JsonProperty(
      "from_broadcaster_user_name"
    ) String fromBroadcasterName,
    @Nullable @JsonProperty("viewers") Integer viewers,
    @Nullable @JsonProperty("total") Integer total,
    @Nullable @JsonProperty("cumulative_total") Integer cumulativeTotal,
    @Nullable @JsonProperty("streak_months") Integer streakMonths,
    @Nullable @JsonProperty("duration_months") Integer durationMonths
  ) {}
}
