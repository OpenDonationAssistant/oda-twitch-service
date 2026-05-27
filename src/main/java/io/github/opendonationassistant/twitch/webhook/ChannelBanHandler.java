package io.github.opendonationassistant.twitch.webhook;

import io.github.opendonationassistant.events.twitch.TwitchFacade;
import io.github.opendonationassistant.events.twitch.events.TwitchUserBannedEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class ChannelBanHandler implements TwitchEventHandler {

  private final TwitchFacade facade;

  @Inject
  public ChannelBanHandler(TwitchFacade facade) {
    this.facade = facade;
  }

  @Override
  public boolean canHandle(String type) {
    return "channel.ban".equals(type);
  }

  @Override
  public CompletableFuture<?> handle(EventContext context) {
    var isPermanent = context.event()
      .map(TwitchEventsWebhook.Event::isPermanent)
      .orElse(false);
    return facade.sendEvent(
      new TwitchUserBannedEvent(
        context.id(),
        context.account().recipientId(),
        context.username(),
        isPermanent
      )
    );
  }
}
