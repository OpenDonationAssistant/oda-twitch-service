package io.github.opendonationassistant.twitch.webhook;

import io.github.opendonationassistant.events.twitch.TwitchFacade;
import io.github.opendonationassistant.events.twitch.events.TwitchChannelSubscriptionGiftEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class ChannelSubscriptionGiftHandler implements TwitchEventHandler {

  private final TwitchFacade facade;

  @Inject
  public ChannelSubscriptionGiftHandler(TwitchFacade facade) {
    this.facade = facade;
  }

  @Override
  public boolean canHandle(String type) {
    return "channel.subscription.gift".equals(type);
  }

  @Override
  public CompletableFuture<?> handle(EventContext context) {
    return facade.sendEvent(
      new TwitchChannelSubscriptionGiftEvent(
        context.id(),
        context.account().recipientId(),
        context.username(),
        context.event().map(TwitchEventsWebhook.Event::tier).orElse(""),
        context.event().map(TwitchEventsWebhook.Event::total).orElse(0),
        context.event().map(TwitchEventsWebhook.Event::cumulativeTotal).orElse(0)
      )
    );
  }
}
