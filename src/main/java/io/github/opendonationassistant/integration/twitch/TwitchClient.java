package io.github.opendonationassistant.integration.twitch;

import io.github.opendonationassistant.integration.twitch.TwitchApiClient.CreateCustomRewardRequest;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.CustomReward;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.DataWrapper;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.GetUserResponse;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SendChatMessageRequest;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SendChatMessageResponse;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.Stream;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SubscribeRequest;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.Subscription;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.UpdateCustomRewardRequest;
import io.github.opendonationassistant.integration.twitch.TwitchIdClient.GetAccessRecordResponse;
import io.github.opendonationassistant.integration.twitch.TwitchIdClient.ValidationResponse;
import io.github.opendonationassistant.rabbit.TokenRPC;
import io.github.opendonationassistant.rabbit.TokenRPC.TokenRequest;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;
import org.zalando.problem.Problem;

@Singleton
public class TwitchClient {

  private final TwitchApiClient api;
  private final TwitchIdClient id;
  private final String clientId;
  private final String clientSecret;
  private final TokenRPC tokenRPC;

  @Inject
  public TwitchClient(
    TwitchApiClient api,
    TwitchIdClient id,
    TokenRPC tokenRPC,
    @Value("${twitch.client.id}") String clientId,
    @Value("${twitch.client.secret}") String clientSecret
  ) {
    this.api = api;
    this.id = id;
    this.tokenRPC = tokenRPC;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }

  public CompletableFuture<GetAccessRecordResponse> getToken(
    Map<String, String> request
  ) {
    return id.getToken(request);
  }

  public CompletableFuture<GetAccessRecordResponse> getAppToken() {
    return id.getToken(
      Map.of(
        "client_id",
        clientId,
        "client_secret",
        clientSecret,
        "grant_type",
        "client_credentials"
      )
    );
  }

  public CompletableFuture<ValidationResponse> validate(String auth) {
    return id.validate(auth);
  }

  public CompletableFuture<DataWrapper<Subscription[]>> subscribe(
    SubscribeRequest request
  ) {
    return getAppToken()
      .thenCompose(response ->
        api.subscribe(
          clientId,
          "Bearer %s".formatted(response.accessToken()),
          request
        )
      );
  }

  public CompletableFuture<DataWrapper<Subscription[]>> getSubscriptions() {
    return getAppToken()
      .thenCompose(response ->
        api.getSubscriptions(
          clientId,
          "Bearer %s".formatted(response.accessToken())
        )
      );
  }

  public CompletableFuture<Void> deleteSubscription(
    @Nullable String status,
    @Nullable String id
  ) {
    return getAppToken()
      .thenCompose(response ->
        api.deleteSubscription(
          clientId,
          "Bearer %s".formatted(response.accessToken()),
          status,
          id
        )
      );
  }

  public CompletableFuture<DataWrapper<List<Stream>>> getStreams(
    String recipientId,
    String refreshTokenId,
    String userId,
    String type
  ) {
    var token = tokenRPC.token(new TokenRequest(recipientId, refreshTokenId));
    if (token == null || token.token() == null) {
      return CompletableFuture.completedFuture(new DataWrapper<>(List.of()));
    }
    return api.getStreams(
      clientId,
      "Bearer %s".formatted(token.token()),
      userId,
      type
    );
  }

  public CompletableFuture<DataWrapper<GetUserResponse>> getUser(
    String recipientId,
    String refreshTokenId,
    String login
  ) {
    var token = tokenRPC.token(new TokenRequest(recipientId, refreshTokenId));
    if (token == null || token.token() == null) {
      throw Problem.builder().withTitle("Unauthorized").build();
    }
    return api.getUser(clientId, "Bearer %s".formatted(token.token()), login);
  }

  public CompletableFuture<
    DataWrapper<List<SendChatMessageResponse>>
  > sendChatMessage(
    String recipientId,
    String refreshTokenId,
    SendChatMessageRequest request
  ) {
    var token = tokenRPC.token(new TokenRequest(recipientId, refreshTokenId));
    if (token == null || token.token() == null) {
      return CompletableFuture.completedFuture(new DataWrapper<>(List.of()));
    }
    return api.sendChatMessage(
      clientId,
      "Bearer %s".formatted(token.token()),
      request
    );
  }

  public CompletableFuture<Void> sendShoutout(
    String recipientId,
    String refreshTokenId,
    String fromBroadcasterId,
    String toBroadcasterId,
    String moderatorId
  ) {
    var token = tokenRPC.token(new TokenRequest(recipientId, refreshTokenId));
    if (token == null || token.token() == null) {
      return CompletableFuture.completedFuture(null);
    }
    return api.sendShoutout(
      clientId,
      "Bearer %s".formatted(token.token()),
      fromBroadcasterId,
      toBroadcasterId,
      moderatorId
    );
  }

  public CompletableFuture<Void> pinChatMessage(
    String recipientId,
    String refreshTokenId,
    String broadcasterId,
    String moderatorId,
    String messageId,
    @Nullable Integer durationSeconds
  ) {
    var token = tokenRPC.token(new TokenRequest(recipientId, refreshTokenId));
    if (token == null || token.token() == null) {
      return CompletableFuture.completedFuture(null);
    }
    return api.pinChatMessage(
      clientId,
      "Bearer %s".formatted(token.token()),
      broadcasterId,
      moderatorId,
      messageId,
      durationSeconds
    );
  }

  public CompletableFuture<DataWrapper<List<CustomReward>>> createCustomReward(
    String recipientId,
    String refreshTokenId,
    String broadcasterId,
    CreateCustomRewardRequest request
  ) {
    var token = tokenRPC.token(new TokenRequest(recipientId, refreshTokenId));
    if (token == null || token.token() == null) {
      return CompletableFuture.completedFuture(null);
    }
    return api.createCustomReward(
      clientId,
      "Bearer %s".formatted(token.token()),
      broadcasterId,
      request
    );
  }

  public CompletableFuture<DataWrapper<List<CustomReward>>> updateCustomReward(
    String recipientId,
    String refreshTokenId,
    String broadcasterId,
    String id,
    UpdateCustomRewardRequest request
  ) {
    var token = tokenRPC.token(new TokenRequest(recipientId, refreshTokenId));
    if (token == null || token.token() == null) {
      return CompletableFuture.completedFuture(null);
    }
    return api.updateCustomReward(
      clientId,
      "Bearer %s".formatted(token.token()),
      broadcasterId,
      id,
      request
    );
  }

  public CompletableFuture<Void> deleteCustomReward(
    String recipientId,
    String refreshTokenId,
    String broadcasterId,
    String id
  ) {
    var token = tokenRPC.token(new TokenRequest(recipientId, refreshTokenId));
    if (token == null || token.token() == null) {
      return CompletableFuture.completedFuture(null);
    }
    return api.deleteCustomReward(
      clientId,
      "Bearer %s".formatted(token.token()),
      broadcasterId,
      id
    );
  }
}
