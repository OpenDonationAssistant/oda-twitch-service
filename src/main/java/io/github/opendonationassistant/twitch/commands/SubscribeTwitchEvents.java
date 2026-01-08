package io.github.opendonationassistant.twitch.commands;

import io.github.opendonationassistant.events.twitch.TwitchCommand.SubscribeEvent;
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
import java.util.function.Function;

public class SubscribeTwitchEvents {

  private final TwitchIdClient idClient;
  private final TwitchFacade facade;

  @Inject
  public SubscribeTwitchEvents(TwitchIdClient idClient, TwitchFacade facade) {
    this.idClient = idClient;
    this.facade = facade;
  }

  public CompletableFuture<?> byToken(String token) {
    return idClient
      .validate("Bearer %s".formatted(token))
      .thenCompose(response -> {
        Function<String, CompletableFuture<Void>> subscribe = type ->
          facade.subscribe(new SubscribeEvent(response.userId(), type));
        return CompletableFuture.allOf(
          subscribe.apply("channel.follow"),
          subscribe.apply("channel.subscribe"),
          subscribe.apply("channel.subscription.gift"),
          subscribe.apply("channel.subscription.message"),
          subscribe.apply("channel.cheer"),
          subscribe.apply("channel.raid"),
          subscribe.apply("channel.poll.begin"),
          subscribe.apply("channel.poll.end"),
          subscribe.apply("channel.prediction.begin"),
          subscribe.apply("channel.prediction.end"),
          subscribe.apply("channel.hype_train.begin"),
          subscribe.apply("channel.hype_train.end"),
          subscribe.apply("channel.shoutout.create"),
          subscribe.apply("channel.shoutout.receive"),
          subscribe.apply("stream.online"),
          subscribe.apply("stream.offline"),
          subscribe.apply("channel.goal.begin"),
          subscribe.apply("channel.goal.progress"),
          subscribe.apply("channel.goal.end"),
          subscribe.apply("user.authorization.revoke")
        );
      });
  }

  @Controller
  public static class HttpWrapper {

    private final SubscribeTwitchEvents subscribe;

    @Inject
    public HttpWrapper(SubscribeTwitchEvents subscribe) {
      this.subscribe = subscribe;
    }

    @Post("/twitch/subscribe")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public CompletableFuture<HttpResponse<Void>> subscribeTwitchEvents(
      @Body SubscribeTwitchEventsCommand command
    ) {
      return subscribe
        .byToken(command.userAccessToken())
        .thenApply(response -> {
          return HttpResponse.ok();
        });
    }
  }

  @Serdeable
  public static record SubscribeTwitchEventsCommand(String userAccessToken) {}
}
