package io.github.opendonationassistant.twitch.listener.handler;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.integration.twitch.TwitchIdClient;
import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.rabbit.TokenRPC;
import io.github.opendonationassistant.rabbit.TokenRPC.TokenRequest;
import io.github.opendonationassistant.twitch.listener.handler.SubscribeEventsHandler.SubcribeTwitchEventsCommand;
import io.github.opendonationassistant.twitch.repository.TwitchAccountData;
import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import io.micronaut.context.annotation.Value;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.Map;

@Singleton
public class SubscribeAllEventsHandler
  extends AbstractMessageHandler<
    SubscribeAllEventsHandler.SubscribeAllTwitchEventsCommand
  > {

  private ODALogger log = new ODALogger(this);
  private final RabbitClient rabbit;
  private final TwitchAccountRepository repository;
  private final TwitchIdClient twitchIdClient;
  private final String clientId;
  private final String clientSecret;

  private final java.util.List<String> events = java.util.List.of(
    "channel.follow"
    // "channel.ban",
    // "channel.subscribe",
    // "channel.subscription.gift",
    // "channel.subscription.message",
    // "channel.cheer",
    // "channel.raid",
    // "channel.poll.begin",
    // "channel.poll.end",
    // "channel.prediction.begin",
    // "channel.prediction.end",
    // "channel.hype_train.begin",
    // "channel.hype_train.end",
    // "channel.shoutout.create",
    // "channel.shoutout.receive",
    // "stream.online",
    // "stream.offline",
    // "channel.goal.begin",
    // "channel.goal.progress",
    // "channel.goal.end",
    // "user.authorization.revoke"
  );

  public SubscribeAllEventsHandler(
    ObjectMapper mapper,
    TwitchIdClient twitchIdClient,
    @Named("commands") RabbitClient rabbit,
    TwitchAccountRepository repository,
    @Value("${twitch.client.id}") String clientId,
    @Value("${twitch.client.secret}") String clientSecret
  ) {
    super(mapper);
    this.twitchIdClient = twitchIdClient;
    this.rabbit = rabbit;
    this.repository = repository;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }

  @Override
  public void handle(SubscribeAllTwitchEventsCommand message)
    throws IOException {
    // var token = tokenRPC.token(
    //   new TokenRequest(message.recipientId(), message.refreshTokenId())
    // );
    // if (token == null || token.token() == null) {
    //   log.error(
    //     "Failed to get token",
    //     Map.of(
    //       "recipientId",
    //       message.recipientId(),
    //       "refreshTokenId",
    //       message.refreshTokenId(),
    //       "token",
    //       token
    //     )
    //   );
    //   return;
    // }
    // var accessToken = token.token();
    var appToken = twitchIdClient
      .getToken(
        Map.of(
          "client_id",
          clientId,
          "client_secret",
          clientSecret,
          "grant_type",
          "client_credentials"
        )
      )
      .join()
      .accessToken();
    var twitchId = repository
      .findByRecipientId(message.recipientId())
      .map(TwitchAccountData::twitchId);
    twitchId.ifPresentOrElse(
      id ->
        events.forEach(event -> {
          rabbit.sendCommand(
            new SubcribeTwitchEventsCommand(appToken, id, event)
          );
        }),
      () ->
        log.error(
          "Can't find twitch id",
          Map.of("recipientId", message.recipientId())
        )
    );
  }

  @Serdeable
  public static record SubscribeAllTwitchEventsCommand(
    String recipientId,
    String refreshTokenId
  ) {}
}
