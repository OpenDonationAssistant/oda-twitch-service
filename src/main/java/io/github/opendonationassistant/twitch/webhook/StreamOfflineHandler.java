package io.github.opendonationassistant.twitch.webhook;

import io.github.opendonationassistant.events.twitch.TwitchFacade;
import io.github.opendonationassistant.events.twitch.events.TwitchStreamEndedEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class StreamOfflineHandler implements TwitchEventHandler {

  private final TwitchFacade facade;

  @Inject
  public StreamOfflineHandler(TwitchFacade facade) {
    this.facade = facade;
  }

  @Override
  public boolean canHandle(String type) {
    return "stream.offline".equals(type);
  }

  @Override
  public CompletableFuture<?> handle(EventContext context) {
    return facade.sendEvent(
      new TwitchStreamEndedEvent(context.id(), context.account().recipientId())
    );
  }
}
