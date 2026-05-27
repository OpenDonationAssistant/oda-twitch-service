package io.github.opendonationassistant.twitch.webhook;

import java.util.concurrent.CompletableFuture;

public interface TwitchEventHandler {
  boolean canHandle(String type);
  CompletableFuture<?> handle(EventContext context);
}
