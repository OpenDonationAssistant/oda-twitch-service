package io.github.opendonationassistant.twitch.listener.handler;

import com.fasterxml.uuid.Generators;
import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SubscribeRequest;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.Transport;
import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import io.github.opendonationassistant.twitch.repository.TwitchWebhook;
import io.github.opendonationassistant.twitch.repository.TwitchWebhookRepository;
import io.micronaut.context.annotation.Value;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class SubscribeEventsHandler
  extends AbstractMessageHandler<
    SubscribeEventsHandler.SubcribeTwitchEventsCommand
  > {

  private final ODALogger log = new ODALogger(this);
  private final TwitchApiClient apiClient;
  private final String clientId;
  private final TwitchWebhookRepository webhookRepository;

  @Inject
  public SubscribeEventsHandler(
    ObjectMapper mapper,
    TwitchApiClient apiClient,
    TwitchAccountRepository accountRepository,
    TwitchWebhookRepository webhookRepository,
    @Value("${twitch.client.id}") String clientId
  ) {
    super(mapper);
    this.apiClient = apiClient;
    this.clientId = clientId;
    this.webhookRepository = webhookRepository;
  }

  @Override
  public void handle(SubcribeTwitchEventsCommand command) throws IOException {
    var conditions = new HashMap<String, String>();
    conditions.put("broadcaster_user_id", command.twitchId());
    conditions.put("moderator_user_id", command.twitchId());
    if ("user.authorization.revoke".equals(command.event())) {
      conditions.put("user_id", command.twitchId());
    }
    if ("user.authorization.revoke".equals(command.event())) {
      conditions.put("client_id", clientId);
    }
    if ("channel.raid".equals(command.event())) {
      conditions.put("to_broadcaster_user_id", command.twitchId());
    }
    try {
      var response = apiClient
        .subscribe(
          clientId,
          "Bearer %s".formatted(command.token()),
          new SubscribeRequest(
            command.event(),
            version(command.event()),
            conditions,
            new Transport(
              "webhook",
              "https://api.oda.digital/twitch/events",
              "oda-client-secret"
            )
          )
        )
        .join();
      var subscriptionId = response.data()[0].id();
      webhookRepository
        .findByRecipientIdAndRefreshTokenId(
          command.recipientId(),
          command.refreshTokenId()
        )
        .ifPresentOrElse(
          existing -> {
            var ids = new ArrayList<String>();
            ids.addAll(existing.subscriptionIds());
            ids.add(subscriptionId);
            webhookRepository.update(
              new TwitchWebhook(
                existing.id(),
                command.recipientId(),
                command.twitchId(),
                command.refreshTokenId(),
                ids
              )
            );
          },
          () -> {
            webhookRepository.save(
              new TwitchWebhook(
                Generators.timeBasedEpochGenerator().generate().toString(),
                command.recipientId(),
                command.twitchId(),
                command.refreshTokenId(),
                List.of(subscriptionId)
              )
            );
          }
        );
    } catch (Exception e) {
      log.error(
        "Error subscribing to twitch event",
        Map.of(
          "error",
          e.getMessage(),
          "event",
          command.event(),
          "recipientId",
          command.recipientId()
        )
      );
      throw e;
    }
  }

  private String version(String type) {
    return switch (type) {
      case "channel.follow" -> "2";
      case "channel.subscribe" -> "1";
      case "channel.subscription.gift" -> "1";
      case "channel.subscription.message" -> "1";
      case "channel.raid" -> "1";
      case "channel.poll.begin" -> "1";
      case "channel.poll.end" -> "1";
      case "channel.prediction.begin" -> "1";
      case "channel.prediction.end" -> "1";
      case "channel.hype_train.begin" -> "2";
      case "channel.hype_train.end" -> "2";
      case "channel.shoutout.create" -> "1";
      case "channel.shoutout.receive" -> "1";
      case "stream.online" -> "1";
      case "stream.offline" -> "1";
      case "channel.goal.begin" -> "1";
      case "channel.goal.progress" -> "1";
      case "channel.goal.end" -> "1";
      case "user.authorization.revoke" -> "1";
      default -> "1";
    };
  }

  @Serdeable
  public static record SubcribeTwitchEventsCommand(
    String token,
    String recipientId,
    String twitchId,
    String refreshTokenId,
    String event
  ) {}
}
