package io.github.opendonationassistant.twitch.webhook;

import io.github.opendonationassistant.events.twitch.TwitchFacade;
import io.github.opendonationassistant.events.twitch.events.TwitchChannelRaidEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class ChannelRaidHandler implements TwitchEventHandler {

  private final TwitchFacade facade;

  @Inject
  public ChannelRaidHandler(TwitchFacade facade) {
    this.facade = facade;
  }

  @Override
  public boolean canHandle(String type) {
    return "channel.raid".equals(type);
  }

  @Override
  public CompletableFuture<?> handle(EventContext context) {
    return facade.sendEvent(
      new TwitchChannelRaidEvent(
        context.id(),
        context.account().recipientId(),
        context.event().map(it -> it.fromBroadcasterName()).orElse(""),
        context.event().map(it -> it.viewers()).orElse(0)
      )
    );
  }
}
