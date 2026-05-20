package io.github.opendonationassistant.twitch.listener.handler;

import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SendChatMessageRequest;
import io.github.opendonationassistant.rabbit.TokenRPC;
import io.github.opendonationassistant.rabbit.TokenRPC.TokenRequest;
import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import io.micronaut.context.annotation.Value;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;

@Singleton
public class SendAndPinChatMessageHandler
  extends AbstractMessageHandler<
    SendAndPinChatMessageHandler.SendAndPinChatMessageCommand
  > {

  private final TwitchApiClient apiClient;
  private final String clientId;
  private final TokenRPC tokenRPC;
  private final TwitchAccountRepository repository;

  @Inject
  public SendAndPinChatMessageHandler(
    ObjectMapper mapper,
    TwitchApiClient apiClient,
    @Value("${twitch.client.id}") String clientId,
    TokenRPC tokenRPC,
    TwitchAccountRepository repository
  ) {
    super(mapper);
    this.apiClient = apiClient;
    this.clientId = clientId;
    this.tokenRPC = tokenRPC;
    this.repository = repository;
  }

  @Override
  public void handle(SendAndPinChatMessageCommand message) throws IOException {
    var token = tokenRPC.token(
      new TokenRequest(message.recipientId(), message.refreshTokenId())
    );
    if (token == null || token.token() == null) {
      return;
    }
    var account = repository.findByRecipientId(message.recipientId());
    if (account.isEmpty()) {
      return;
    }
    var twitchId = account.get().twitchId();
    var auth = "Bearer %s".formatted(token.token());
    var sendResponse = apiClient
      .sendChatMessage(
        clientId,
        auth,
        new SendChatMessageRequest(twitchId, twitchId, message.message())
      )
      .join();
    if (sendResponse.data() == null || sendResponse.data().isEmpty()) {
      return;
    }
    var sent = sendResponse.data().getFirst();
    if (!sent.isSent()) {
      return;
    }
    apiClient
      .pinChatMessage(
        clientId,
        auth,
        twitchId,
        twitchId,
        sent.messageId(),
        null
      )
      .join();
  }

  @Serdeable
  public static record SendAndPinChatMessageCommand(
    String recipientId,
    String refreshTokenId,
    String message
  ) {}
}
