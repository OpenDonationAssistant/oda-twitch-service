package io.github.opendonationassistant.twitch.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

@Controller
public class TwitchEventsWebhook {

  private final ODALogger log = new ODALogger(this);
  private final TimeBasedEpochGenerator uuid =
    Generators.timeBasedEpochGenerator();
  private final TwitchAccountRepository repository;
  private final List<TwitchEventHandler> handlers;

  @Inject
  public TwitchEventsWebhook(
    TwitchAccountRepository repository,
    List<TwitchEventHandler> handlers
  ) {
    this.repository = repository;
    this.handlers = handlers;
  }

  @Post("/twitch/events")
  @Secured(SecurityRule.IS_ANONYMOUS)
  @Operation(hidden = true)
  public CompletableFuture<HttpResponse<String>> twitchWebhook(
    @Header("Twitch-Eventsub-Message-Type") String type,
    @Body Message message
  ) {
    log.debug(
      "Received webhook message",
      Map.of("type", type, "message", message)
    );
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
            var context = new EventContext(id, account, username, event);
            return handlers.stream()
              .filter(h -> h.canHandle(message.subscription.type))
              .findFirst()
              .map(h -> h.handle(context))
              .orElseGet(() -> CompletableFuture.completedFuture(null));
          })
          .orElseGet(() -> {
            log.info(
              "No account found for user in webhook",
              Map.of("message", message)
            );
            return CompletableFuture.completedFuture(null);
          })
          .thenApply(response -> HttpResponse.ok(""));
      default:
        log.info("Unknown webhook message type", Map.of("type", type));
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
    @Nullable @JsonProperty("broadcaster_user_id") String broadcasterId,
    @Nullable @JsonProperty("user_name") String userName,
    @Nullable @JsonProperty("tier") String tier,
    @Nullable @JsonProperty("is_gift") Boolean isGift,
    @Nullable @JsonProperty("is_anonymous") Boolean isAnonymous,
    @Nullable @JsonProperty("is_permanent") Boolean isPermanent,
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
    @Nullable @JsonProperty("duration_months") Integer durationMonths,
    @Nullable @JsonProperty("user_input") String userInput,
    @Nullable @JsonProperty("reward") Reward reward
  ) {}

  @Serdeable
  public static record Reward(
    String id,
    String title,
    Integer cost,
    @Nullable String prompt
  ) {}

  @Serdeable
  public static record AddMediaCommand(
    String url,
    String requester,
    String recipientId,
    String system
  ) {}
}
