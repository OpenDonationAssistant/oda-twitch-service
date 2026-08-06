package io.github.opendonationassistant.twitch.listener.handler;

import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.DataWrapper;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SendChatMessageRequest;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SendChatMessageResponse;
import io.github.opendonationassistant.integration.twitch.TwitchClient;
import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.List;

@Singleton
public class SendAndPinChatMessageHandler
  extends AbstractMessageHandler<
    SendAndPinChatMessageHandler.SendAndPinChatMessageCommand
  > {

  private final TwitchClient twitch;
  private final TwitchAccountRepository repository;

  @Inject
  public SendAndPinChatMessageHandler(
    ObjectMapper mapper,
    TwitchClient twitch,
    TwitchAccountRepository repository
  ) {
    super(mapper);
    this.twitch = twitch;
    this.repository = repository;
  }

  @Override
  public void handle(SendAndPinChatMessageCommand message) throws IOException {
    final var account = repository.findByRefreshTokenId(
      message.senderRefreshTokenId()
    );
    if (account.isEmpty()) {
      return;
    }

    final DataWrapper<List<SendChatMessageResponse>> sendResponse = twitch
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

    if (sendResponse.data() == null || sendResponse.data().isEmpty()) {
      return;
    }
    var sent = sendResponse.data().getFirst();
    if (!sent.isSent()) {
      return;
    }
    twitch
      .pinChatMessage(
        message.recipientId(),
        message.senderRefreshTokenId(),
        message.recipientTwitchId(),
        account.get().twitchId(),
        sent.messageId(),
        null
      )
      .join();
  }

  @Serdeable
  public static record SendAndPinChatMessageCommand(
    String recipientId,
    String senderRefreshTokenId,
    String recipientTwitchId,
    String message
  ) {}
}
