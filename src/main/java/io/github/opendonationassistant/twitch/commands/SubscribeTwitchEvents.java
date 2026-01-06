package io.github.opendonationassistant.twitch.commands;

import io.github.opendonationassistant.integration.twitch.TwitchApiClient;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SubscribeRequest;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.Transport;
import io.github.opendonationassistant.integration.twitch.TwitchIdClient;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Controller
public class SubscribeTwitchEvents {

  private final String clientId;
  private final TwitchIdClient idClient;
  private final TwitchApiClient apiClient;

  @Inject
  public SubscribeTwitchEvents(
    @Value("${twitch.client.id}") String clientId,
    TwitchIdClient idClient,
    TwitchApiClient apiClient
  ) {
    this.clientId = clientId;
    this.idClient = idClient;
    this.apiClient = apiClient;
  }

  @Post("/twitch/subscribe")
  @Secured(SecurityRule.IS_AUTHENTICATED)
  public CompletableFuture<HttpResponse<Void>> subscribeTwitchEvents(
    @Body SubscribeTwitchEventsCommand command
  ) {
    return idClient
      .validate("Bearer %s".formatted(command.userAccessToken()))
      .thenCompose(response -> {
        return apiClient.subscribe(
          clientId,
          "Bearer %s".formatted(command.userAccessToken()),
          new SubscribeRequest(
            "channel.follow",
            "2",
            Map.of("broadcaster_user_id", response.userId()),
            new Transport(
              "webhook",
              "https://api.oda.digital/twitch/events",
              "oda-client-secret"
            )
          )
        );
      })
      .thenApply(response -> {
        return HttpResponse.ok();
      });
  }

  @Serdeable
  public static record SubscribeTwitchEventsCommand(String userAccessToken) {}
}
