package io.github.opendonationassistant.twitch.listener.handler;

import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient;
import io.github.opendonationassistant.rabbit.TokenRPC;
import io.github.opendonationassistant.rabbit.TokenRPC.TokenRequest;
import io.micronaut.context.annotation.Value;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

@Singleton
public class UnsubscribeAllTwitchEventsHandler
  extends AbstractMessageHandler<
    UnsubscribeAllTwitchEventsHandler.UnsubscribeAllTwitchEventsCommand
  > {

  private final TwitchApiClient apiClient;
  private final String clientId;
  private final TokenRPC tokenRPC;

  @Inject
  public UnsubscribeAllTwitchEventsHandler(
    ObjectMapper mapper,
    TwitchApiClient apiClient,
    @Value("${twitch.client.id}") String clientId,
    TokenRPC tokenRPC
  ) {
    super(mapper);
    this.apiClient = apiClient;
    this.clientId = clientId;
    this.tokenRPC = tokenRPC;
  }

  @Override
  public void handle(UnsubscribeAllTwitchEventsCommand message)
    throws IOException {
    var token = tokenRPC.token(
      new TokenRequest(message.recipientId(), message.refreshTokenId())
    );
    if (token == null || token.token() == null) {
      return;
    }
    apiClient
      .getSubscriptions(clientId, "Bearer %s".formatted(token.token()))
      .thenCompose(subscriptions -> {
        return CompletableFuture.allOf(
          (CompletableFuture[]) Arrays.stream(subscriptions.data())
            .map(subscription -> {
              return apiClient.deleteSubscription(
                clientId,
                "Bearer %s".formatted(token.token()),
                ANY_STATUS,
                subscription.id()
              );
            })
            .toArray()
        );
      })
      .join();
  }

  @Serdeable
  public static record UnsubscribeAllTwitchEventsCommand(
    String recipientId,
    String refreshTokenId
  ) {}

  @Nullable
  private static final String ANY_STATUS = null;
}
