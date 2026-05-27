package io.github.opendonationassistant.twitch.webhook;

import io.github.opendonationassistant.events.twitch.TwitchFacade;
import io.github.opendonationassistant.events.twitch.events.TwitchChannelCheerEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class ChannelCheerHandler implements TwitchEventHandler {

  private final TwitchFacade facade;

  @Inject
  public ChannelCheerHandler(TwitchFacade facade) {
    this.facade = facade;
  }

  @Override
  public boolean canHandle(String type) {
    return "channel.cheer".equals(type);
  }

  @Override
  public CompletableFuture<?> handle(EventContext context) {
    return facade.sendEvent(
      new TwitchChannelCheerEvent(
        context.id(),
        context.account().recipientId(),
        context.username(),
        context.event()
          .map(TwitchEventsWebhook.Event::message)
          .map(String::valueOf)
          .orElse(""),
        context.event()
          .map(TwitchEventsWebhook.Event::bits)
          .map(String::valueOf)
          .orElse("0")
      )
    );
  }
}
