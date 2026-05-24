package io.github.opendonationassistant.twitch.listener.handler;

import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.integration.twitch.TwitchClient;
import io.github.opendonationassistant.twitch.repository.TwitchWebhook;
import io.github.opendonationassistant.twitch.repository.TwitchWebhookRepository;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

@Singleton
public class UnsubscribeAllTwitchEventsHandler
  extends AbstractMessageHandler<
    UnsubscribeAllTwitchEventsHandler.UnsubscribeAllTwitchEventsCommand
  > {

  private final TwitchClient twitch;
  private final TwitchWebhookRepository webhookRepository;

  @Inject
  public UnsubscribeAllTwitchEventsHandler(
    ObjectMapper mapper,
    TwitchClient twitch,
    TwitchWebhookRepository webhookRepository
  ) {
    super(mapper);
    this.twitch = twitch;
    this.webhookRepository = webhookRepository;
  }

  @Override
  public void handle(UnsubscribeAllTwitchEventsCommand message)
    throws IOException {
    var token = twitch.getAppToken().join().accessToken();
    var auth = "Bearer %s".formatted(token);
    webhookRepository
      .findById(message.recipientId())
      .ifPresent(webhook -> {
        CompletableFuture.allOf(
          webhook
            .subscriptionIds()
            .stream()
            .map(id -> twitch.deleteSubscription(auth, ANY_STATUS, id))
            .toArray(CompletableFuture[]::new)
        ).join();
        webhookRepository.save(
          new TwitchWebhook(
            webhook.recipientId(),
            webhook.twitchId(),
            List.of()
          )
        );
      });
  }

  @Serdeable
  public static record UnsubscribeAllTwitchEventsCommand(
    String recipientId,
    String refreshTokenId
  ) {}

  @Nullable
  private static final String ANY_STATUS = null;
}
