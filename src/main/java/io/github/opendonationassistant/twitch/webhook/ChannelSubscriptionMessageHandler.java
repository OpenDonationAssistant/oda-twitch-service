package io.github.opendonationassistant.twitch.webhook;

import io.github.opendonationassistant.events.twitch.TwitchFacade;
import io.github.opendonationassistant.events.twitch.events.TwitchChannelSubscriptionMessageEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Singleton
public class ChannelSubscriptionMessageHandler implements TwitchEventHandler {

  private final TwitchFacade facade;

  @Inject
  public ChannelSubscriptionMessageHandler(TwitchFacade facade) {
    this.facade = facade;
  }

  @Override
  public boolean canHandle(String type) {
    return "channel.subscription.message".equals(type);
  }

  @Override
  public CompletableFuture<?> handle(EventContext context) {
    return facade.sendEvent(
      new TwitchChannelSubscriptionMessageEvent(
        context.id(),
        context.account().recipientId(),
        context.username(),
        context.event().map(TwitchEventsWebhook.Event::tier).orElse(""),
        new TwitchChannelSubscriptionMessageEvent.Message(
          String.valueOf(
            context.event()
              .map(it -> (Map<String, Object>) it.message())
              .map(it -> (String) it.get("text"))
              .orElse("")
          ),
          List.of()
        ),
        context.event().map(TwitchEventsWebhook.Event::total).orElse(0),
        context.event().map(TwitchEventsWebhook.Event::cumulativeTotal).orElse(0),
        context.event().map(TwitchEventsWebhook.Event::streakMonths).orElse(0)
      )
    );
  }
}
