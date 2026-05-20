package io.github.opendonationassistant.twitch.listener.handler;

import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.rabbit.TokenRPC;
import io.github.opendonationassistant.rabbit.TokenRPC.TokenRequest;
import io.github.opendonationassistant.twitch.listener.handler.SubscribeEventsHandler.SubcribeTwitchEventsCommand;
import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.IOException;

@Singleton
public class SubscribeAllEventsHandler
  extends AbstractMessageHandler<
    SubscribeAllEventsHandler.SubscribeAllTwitchEventsCommand
  > {

  private final TokenRPC tokenRPC;
  private final RabbitClient rabbit;
  private final TwitchAccountRepository repository;

  private final java.util.List<String> events = java.util.List.of(
    "channel.follow",
    "channel.ban",
    "channel.subscribe",
    "channel.subscription.gift",
    "channel.subscription.message",
    "channel.cheer",
    "channel.raid",
    "channel.poll.begin",
    "channel.poll.end",
    "channel.prediction.begin",
    "channel.prediction.end",
    "channel.hype_train.begin",
    "channel.hype_train.end",
    "channel.shoutout.create",
    "channel.shoutout.receive",
    "stream.online",
    "stream.offline",
    "channel.goal.begin",
    "channel.goal.progress",
    "channel.goal.end",
    "user.authorization.revoke"
  );

  public SubscribeAllEventsHandler(
    ObjectMapper mapper,
    TokenRPC tokenRPC,
    @Named("commands") RabbitClient rabbit,
    TwitchAccountRepository repository
  ) {
    super(mapper);
    this.tokenRPC = tokenRPC;
    this.rabbit = rabbit;
    this.repository = repository;
  }

  @Override
  public void handle(SubscribeAllTwitchEventsCommand message)
    throws IOException {
    var token = tokenRPC.token(
      new TokenRequest(message.recipientId(), message.refreshTokenId())
    );
    if (token == null || token.token() == null) {
      return;
    }
    var twitchId = repository
      .findByRecipientId(message.recipientId())
      .get()
      .twitchId();
    events.forEach(event -> {
      rabbit.sendCommand(
        new SubcribeTwitchEventsCommand(token.token(), twitchId, event)
      );
    });
  }

  @Serdeable
  public static record SubscribeAllTwitchEventsCommand(
    String recipientId,
    String refreshTokenId
  ) {}
}
