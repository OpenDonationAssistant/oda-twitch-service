package io.github.opendonationassistant.twitch.listener.handler;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SendChatMessageRequest;
import io.github.opendonationassistant.integration.twitch.TwitchClient;
import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Singleton
public class SendChatMessageHandler
  extends AbstractMessageHandler<
    SendChatMessageHandler.SendTwitchMessageCommand
  > {

  private final ODALogger log = new ODALogger(this);
  private final TwitchClient twitch;
  private final TwitchAccountRepository repository;

  @Inject
  public SendChatMessageHandler(
    ObjectMapper mapper,
    TwitchClient twitch,
    TwitchAccountRepository repository
  ) {
    super(mapper);
    this.twitch = twitch;
    this.repository = repository;
  }

  @Override
  public void handle(SendTwitchMessageCommand message) {
    final var account = repository.findByRefreshTokenId(
      message.senderRefreshTokenId()
    );
    if (account.isEmpty()) {
      log.warn(
        "Account not found",
        Map.of("refreshTokenId", message.senderRefreshTokenId())
      );
      return;
    }

    try {
      final var response = twitch
        .sendChatMessage(
          message.recipientId(),
          message.senderRefreshTokenId(),
          new SendChatMessageRequest(
            message.recipientTwitchId(),
            account.get().twitchId(),
            message.message()
          )
        )
        .join();

      log.debug("sendResponse", Map.of("response", response));
      if (response.error() != null) {
        log.error(
          "Failed to send message",
          Map.of(
            "recipientId",
            message.recipientId(),
            "recipientTwitchId",
            message.recipientTwitchId(),
            "error",
            response.error(),
            "errorMessage",
            Optional.ofNullable(response.message()).orElse("")
          )
        );
      }
    } catch (Exception e) {
      log.error(
        "Failed to execute SendTwitchMessageCommand",
        Map.of(
          "recipientId",
          message.recipientId(),
          "recipientTwitchId",
          message.recipientTwitchId(),
          "error",
          Objects.toString(e.getMessage(), "")
        )
      );
    }
  }

  @Serdeable
  public static record SendTwitchMessageCommand(
    String recipientId,
    String senderRefreshTokenId,
    String recipientTwitchId,
    String message
  ) {}
}
