package io.github.opendonationassistant.twitch.commands;

import io.github.opendonationassistant.events.twitch.TwitchCommand.UnsubscribeAllEvent;
import io.github.opendonationassistant.events.twitch.TwitchFacade;
import io.github.opendonationassistant.integration.twitch.TwitchIdClient;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import java.util.concurrent.CompletableFuture;

public class UnsubscribeTwitchEvents {

  private final TwitchIdClient idClient;
  private final TwitchFacade facade;

  @Inject
  public UnsubscribeTwitchEvents(TwitchIdClient idClient, TwitchFacade facade) {
    this.idClient = idClient;
    this.facade = facade;
  }

  public CompletableFuture<?> byToken(String accessToken) {
    return idClient
      .validate("Bearer %s".formatted(accessToken))
      .thenCompose(response ->
        facade.unsubscribe(new UnsubscribeAllEvent(response.login()))
      );
  }

  @Controller
  public static class HttpWrapper {

    private UnsubscribeTwitchEvents unsubscribe;

    @Inject
    public HttpWrapper(UnsubscribeTwitchEvents unsubscribe) {
      this.unsubscribe = unsubscribe;
    }

    @Post("/twitch/unsubscribeAll")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public CompletableFuture<HttpResponse<Void>> subscribeTwitchEvents(
      @Body UnsubscribeAllTwitchEventCommand command
    ) {
      return unsubscribe
        .byToken(command.userAccessToken())
        .thenApply(response -> HttpResponse.ok());
    }
  }

  @Serdeable
  public static record UnsubscribeAllTwitchEventCommand(
    String userAccessToken
  ) {}
}
