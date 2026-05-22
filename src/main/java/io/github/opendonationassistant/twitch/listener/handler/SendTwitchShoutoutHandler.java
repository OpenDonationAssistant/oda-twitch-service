package io.github.opendonationassistant.twitch.listener.handler;

import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient;
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
public class SendTwitchShoutoutHandler
  extends AbstractMessageHandler<
    SendTwitchShoutoutHandler.TwitchShoutoutCommand
  > {

  private final TwitchApiClient apiClient;
  private final String clientId;
  private final TokenRPC tokenRPC;
  private final TwitchAccountRepository repository;

  @Inject
  public SendTwitchShoutoutHandler(
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
  public void handle(TwitchShoutoutCommand message) throws IOException {
    var account = repository.findByRecipientId(message.recipientId());
    if (account.isEmpty()) {
      return;
    }
    var refreshTokenId = account.get().refreshTokenId();
    var token = tokenRPC.token(
      new TokenRequest(message.recipientId(), refreshTokenId)
    );
    if (token == null || token.token() == null) {
      return;
    }
    var twitchId = account.get().twitchId();
    var auth = "Bearer %s".formatted(token.token());
    apiClient
      .sendShoutout(clientId, auth, twitchId, message.targetTwitchId(), twitchId)
      .join();
  }

  @Serdeable
  public static record TwitchShoutoutCommand(
    String recipientId,
    String targetTwitchId
  ) {}
}
