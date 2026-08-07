package io.github.opendonationassistant.twitch.webhook;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.twitch.TwitchFacade;
import io.github.opendonationassistant.events.twitch.events.TwitchStreamStartedEvent;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient;
import io.github.opendonationassistant.integration.twitch.TwitchClient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
public class StreamOnlineHandler implements TwitchEventHandler {

  private ODALogger log = new ODALogger(this);
  private final TwitchFacade facade;
  private final TwitchClient client;

  @Inject
  public StreamOnlineHandler(TwitchFacade facade, TwitchClient client) {
    this.facade = facade;
    this.client = client;
  }

  @Override
  public boolean canHandle(String type) {
    return "stream.online".equals(type);
  }

  @Override
  public CompletableFuture<?> handle(EventContext context) {
    return client
      .getStreams(
        context.account().recipientId(),
        context.account().refreshTokenId(),
        context.account().twitchId(),
        "live"
      )
      .thenAccept(stream -> {
        final Optional<TwitchApiClient.Stream> first = stream
          .data()
          .stream()
          .findFirst();
        first
          .map(TwitchApiClient.Stream::thumbnailUrl)
          .ifPresentOrElse(
            thumbnailUrl -> {
              facade.sendEvent(
                new TwitchStreamStartedEvent(
                  context.id(),
                  context.account().recipientId(),
                  thumbnailUrl
                )
              );
            },
            () -> {
              log.error(
                "Could find stream online",
                Map.of("recipientId", context.account().recipientId())
              );
            }
          );
      });
  }
}
