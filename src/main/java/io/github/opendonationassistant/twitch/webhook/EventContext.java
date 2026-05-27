package io.github.opendonationassistant.twitch.webhook;

import io.github.opendonationassistant.twitch.repository.TwitchAccountData;
import java.util.Optional;

public record EventContext(
  String id,
  TwitchAccountData account,
  String username,
  Optional<TwitchEventsWebhook.Event> event
) {}
