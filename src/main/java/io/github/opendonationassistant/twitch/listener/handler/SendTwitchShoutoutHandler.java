package io.github.opendonationassistant.twitch.listener.handler;

import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.integration.twitch.TwitchClient;
import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
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

  private final TwitchClient twitch;
  private final TwitchAccountRepository repository;

  @Inject
  public SendTwitchShoutoutHandler(
    ObjectMapper mapper,
    TwitchClient apiClient,
    TwitchAccountRepository repository
  ) {
    super(mapper);
    this.twitch = apiClient;
    this.repository = repository;
  }

  @Override
  public void handle(TwitchShoutoutCommand message) throws IOException {
    var account = repository.findByRecipientId(message.recipientId());
    if (account.isEmpty()) {
      return;
    }
    var refreshTokenId = account.get().refreshTokenId();
    var twitchId = account.get().twitchId();
    twitch
      .sendShoutout(
        message.recipientId(),
        refreshTokenId,
        twitchId,
        message.targetTwitchId(),
        twitchId
      )
      .join();
  }

  @Serdeable
  public static record TwitchShoutoutCommand(
    String recipientId,
    String targetTwitchId
  ) {}
}
