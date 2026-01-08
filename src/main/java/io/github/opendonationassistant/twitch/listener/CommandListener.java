package io.github.opendonationassistant.twitch.listener;

import io.github.opendonationassistant.events.twitch.TwitchCommand.SubscribeEvent;
import io.github.opendonationassistant.events.twitch.TwitchCommand.UnsubscribeAllEvent;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SubscribeRequest;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.Transport;
import io.github.opendonationassistant.integration.twitch.TwitchIdClient;
import io.micronaut.context.annotation.Value;
import io.micronaut.messaging.annotation.MessageHeader;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RabbitListener
public class CommandListener {

  private final String clientId;
  private final String clientSecret;
  private final TwitchIdClient idClient;
  private final TwitchApiClient apiClient;

  @Inject
  public CommandListener(
    @Value("${twitch.client.id}") String clientId,
    @Value("${twitch.client.secret}") String clientSecret,
    TwitchIdClient idClient,
    TwitchApiClient apiClient
  ) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.idClient = idClient;
    this.apiClient = apiClient;
  }

  @Queue(io.github.opendonationassistant.rabbit.Queue.Twitch.COMMAND)
  public CompletableFuture<?> listen(
    @MessageHeader String type,
    byte[] payload
  ) throws IOException {
    switch (type) {
      case "SubscribeEvent":
        return handleSubscribeEvent(
          ObjectMapper.getDefault().readValue(payload, SubscribeEvent.class)
        );
      case "UnsubscribeAllEvent":
        return handleUnsubscribeAllEvent(
          ObjectMapper.getDefault()
            .readValue(payload, UnsubscribeAllEvent.class)
        );
      default:
        return CompletableFuture.completedFuture(null);
    }
  }

  private CompletableFuture<?> handleSubscribeEvent(SubscribeEvent event) {
    var params = new HashMap<String, String>();
    params.put("client_id", clientId);
    params.put("client_secret", clientSecret);
    params.put("grant_type", "client_credentials");
    return idClient
      .getToken(params)
      .thenCompose(token -> {
        return apiClient.subscribe(
          clientId,
          "Bearer %s".formatted(token.accessToken()),
          new SubscribeRequest(
            event.event(),
            version(event.event()),
            Map.of(
              "broadcaster_user_id",
              event.twitchId(),
              "moderator_user_id",
              event.twitchId()
            ),
            new Transport(
              "webhook",
              "https://api.oda.digital/twitch/events",
              "oda-client-secret"
            )
          )
        );
      });
  }

  private CompletableFuture<?> handleUnsubscribeAllEvent(
    UnsubscribeAllEvent event
  ) {
    var params = new HashMap<String, String>();
    params.put("client_id", clientId);
    params.put("client_secret", clientSecret);
    params.put("grant_type", "client_credentials");
    return idClient
      .getToken(params)
      .thenCompose(token -> {
        return apiClient
          .getSubscriptions(
            clientId,
            "Bearer %s".formatted(token.accessToken())
          )
          .thenCompose(subscriptions -> {
            return CompletableFuture.allOf(
              (CompletableFuture[]) Arrays.stream(subscriptions.data())
                .map(subscription -> {
                  return apiClient.deleteSubscription(
                    clientId,
                    "Bearer %s".formatted(token.accessToken()),
                    null,
                    subscription.id()
                  );
                })
                .toArray()
            );
          });
      });
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
}
