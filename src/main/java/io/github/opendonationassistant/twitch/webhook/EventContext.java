package io.github.opendonationassistant.twitch.webhook;

import io.github.opendonationassistant.twitch.repository.TwitchAccount;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public record EventContext(
  String id,
  @Nullable String messageId,
  TwitchAccount account,
  String username,
  Optional<TwitchEventsWebhook.Event> event
) {}
