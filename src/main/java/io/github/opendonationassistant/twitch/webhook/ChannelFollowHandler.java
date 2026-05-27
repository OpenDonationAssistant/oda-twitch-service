package io.github.opendonationassistant.twitch.webhook;

import io.github.opendonationassistant.events.twitch.TwitchFacade;
import io.github.opendonationassistant.events.twitch.events.TwitchChannelFollowEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Singleton
public class ChannelFollowHandler implements TwitchEventHandler {

  private final TwitchFacade facade;

  @Inject
  public ChannelFollowHandler(TwitchFacade facade) {
    this.facade = facade;
  }

  @Override
  public boolean canHandle(String type) {
    return "channel.follow".equals(type);
  }

  @Override
  public CompletableFuture<?> handle(EventContext context) {
    var timestamp = context.event()
      .map(TwitchEventsWebhook.Event::timestamp)
      .map(Instant::parse)
      .orElse(Instant.now());
    return facade.sendEvent(
      new TwitchChannelFollowEvent(
        context.id(),
        context.account().recipientId(),
        context.username(),
        timestamp
      )
    );
  }
}
