package io.github.opendonationassistant.twitch.listener.handler;

import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.integration.twitch.TwitchClient;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import org.jspecify.annotations.Nullable;

@Singleton
public class TwitchAnnounceHandler
  extends AbstractMessageHandler<TwitchAnnounceHandler.TwitchAnnounceCommand> {

  private final TwitchClient twitch;

  @Inject
  public TwitchAnnounceHandler(ObjectMapper mapper, TwitchClient twitch) {
    super(mapper);
    this.twitch = twitch;
  }

  @Override
  public void handle(TwitchAnnounceCommand message) throws IOException {
    twitch
      .sendAnnouncement(
        message.recipientId(),
        message.senderRefreshTokenId(),
        message.recipientTwitchId(),
        message.moderatorTwitchId(),
        message.message(),
        message.color()
      )
      .join();
  }

  @Serdeable
  public static record TwitchAnnounceCommand(
    String recipientId,
    String senderRefreshTokenId,
    String recipientTwitchId,
    String moderatorTwitchId,
    String message,
    @Nullable String color
  ) {}
}
