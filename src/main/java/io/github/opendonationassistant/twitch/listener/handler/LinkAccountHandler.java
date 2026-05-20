package io.github.opendonationassistant.twitch.listener.handler;

import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Singleton;
import java.io.IOException;

@Singleton
public class LinkAccountHandler
  extends AbstractMessageHandler<LinkAccountHandler.LinkTwitchAccount> {

  private final TwitchAccountRepository repository;

  public LinkAccountHandler(
    ObjectMapper mapper,
    TwitchAccountRepository repository
  ) {
    super(mapper);
    this.repository = repository;
  }

  @Override
  public void handle(LinkTwitchAccount command) throws IOException {
    repository.create(
      command.recipientId(),
      command.twitchId(),
      command.twitchLogin(),
      command.refreshTokenId()
    );
  }

  @Serdeable
  public static record LinkTwitchAccount(
    String recipientId,
    String twitchId,
    String twitchLogin,
    String refreshTokenId
  ) {}
}
