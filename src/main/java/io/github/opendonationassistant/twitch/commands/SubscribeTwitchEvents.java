package io.github.opendonationassistant.twitch.commands;

import io.github.opendonationassistant.commons.micronaut.BaseController;
import io.github.opendonationassistant.events.twitch.TwitchCommand.LinkAccount;
import io.github.opendonationassistant.events.twitch.TwitchCommand.SubscribeEvent;
import io.github.opendonationassistant.events.twitch.TwitchFacade;
import io.github.opendonationassistant.integration.twitch.TwitchIdClient;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class SubscribeTwitchEvents extends BaseController {

  private final TwitchIdClient idClient;
  private final TwitchFacade facade;

  @Inject
  public SubscribeTwitchEvents(TwitchIdClient idClient, TwitchFacade facade) {
    this.idClient = idClient;
    this.facade = facade;
  }

  @Controller
  public static class HttpWrapper extends BaseController {

    private final SubscribeTwitchEvents subscribe;

    @Inject
    public HttpWrapper(SubscribeTwitchEvents subscribe) {
      this.subscribe = subscribe;
    }

    @Post("/twitch/subscribe")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public CompletableFuture<HttpResponse<Void>> subscribeTwitchEvents(
      Authentication auth,
      @Body SubscribeTwitchEventsCommand command
    ) {
      var recipientId = getOwnerId(auth);
      if (recipientId.isEmpty()) {
        return CompletableFuture.completedFuture(HttpResponse.unauthorized());
      }
      return CompletableFuture.completedFuture(HttpResponse.ok());
    }
  }

  @Serdeable
  public static record SubscribeTwitchEventsCommand(String userAccessToken) {}
}
