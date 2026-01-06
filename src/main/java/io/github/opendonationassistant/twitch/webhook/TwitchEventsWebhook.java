package io.github.opendonationassistant.twitch.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Controller
public class TwitchEventsWebhook {

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
        System.out.println(message.event.userName);
        return CompletableFuture.completedFuture(HttpResponse.ok(""));
      default:
        return CompletableFuture.completedFuture(HttpResponse.ok(""));
    }
  }

  @Serdeable
  public static record Message(
    String challenge,
    Subscription subscription,
    Event event
  ) {}

  @Serdeable
  public static record Subscription(
    String type,
    Map<String, String> condition
  ) {}

  @Serdeable
  public static record Event(@JsonProperty("user_name") String userName) {}
}
