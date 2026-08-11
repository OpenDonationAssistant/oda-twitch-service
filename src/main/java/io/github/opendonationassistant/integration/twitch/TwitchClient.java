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
import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.zalando.problem.Problem;

@Singleton
public class TwitchClient {

  private final TwitchApiClient api;
  private final TwitchIdClient id;
  private final String clientId;
  private final String clientSecret;
  private final TokenRPC tokenRPC;
  private final TwitchAccountRepository accountRepository;

  @Inject
  public TwitchClient(
    TwitchApiClient api,
    TwitchIdClient id,
    TokenRPC tokenRPC,
    TwitchAccountRepository accountRepository,
    @Value("${twitch.client.id}") String clientId,
    @Value("${twitch.client.secret}") String clientSecret
  ) {
    this.api = api;
    this.id = id;
    this.tokenRPC = tokenRPC;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.accountRepository = accountRepository;
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
    return runWithToken(recipientId, refreshTokenId, auth ->
      api.getStreams(clientId, auth, userId, type)
    );
  }

  public CompletableFuture<DataWrapper<GetUserResponse>> getUser(
    String recipientId,
    String refreshTokenId,
    String login
  ) {
    return runWithToken(recipientId, refreshTokenId, auth ->
      api.getUser(clientId, auth, login)
    );
  }

  public CompletableFuture<
    DataWrapper<List<SendChatMessageResponse>>
  > sendChatMessage(
    String recipientId,
    String refreshTokenId,
    SendChatMessageRequest request
  ) {
    return runWithToken(recipientId, refreshTokenId, auth ->
      api.sendChatMessage(clientId, auth, request)
    );
  }

  public CompletableFuture<Void> sendShoutout(
    String recipientId,
    String refreshTokenId,
    String fromBroadcasterId,
    String toBroadcasterId,
    String moderatorId
  ) {
    return runWithToken(recipientId, refreshTokenId, auth ->
      api.sendShoutout(
        clientId,
        auth,
        fromBroadcasterId,
        toBroadcasterId,
        moderatorId
      )
    );
  }

  public CompletableFuture<DataWrapper<Void>> pinChatMessage(
    String recipientId,
    String refreshTokenId,
    String broadcasterId,
    String moderatorId,
    String messageId,
    @Nullable Integer durationSeconds
  ) {
    return runWithToken(recipientId, refreshTokenId, auth ->
      api.pinChatMessage(
        clientId,
        auth,
        broadcasterId,
        moderatorId,
        messageId,
        durationSeconds
      )
    );
  }

  public CompletableFuture<DataWrapper<List<CustomReward>>> createCustomReward(
    String recipientId,
    String refreshTokenId,
    CreateCustomRewardRequest request
  ) {
    return accountRepository
      .findByRecipientId(recipientId)
      .map(account ->
        runWithToken(recipientId, refreshTokenId, auth ->
          api.createCustomReward(clientId, auth, account.twitchId(), request)
        )
      )
      // TODO or throw error
      .orElseGet(() ->
        CompletableFuture.completedFuture(
          new DataWrapper<>(List.of(), null, null, 200)
        )
      );
  }

  public CompletableFuture<DataWrapper<List<CustomReward>>> updateCustomReward(
    String recipientId,
    String refreshTokenId,
    String broadcasterId,
    String id,
    UpdateCustomRewardRequest request
  ) {
    return runWithToken(recipientId, refreshTokenId, auth ->
      api.updateCustomReward(clientId, auth, broadcasterId, id, request)
    );
  }

  public CompletableFuture<Void> deleteCustomReward(
    String recipientId,
    String refreshTokenId,
    String broadcasterId,
    String id
  ) {
    return runWithToken(recipientId, refreshTokenId, auth ->
      api.deleteCustomReward(clientId, auth, broadcasterId, id)
    );
  }

  private <T> CompletableFuture<T> runWithToken(
    String recipientId,
    String refreshTokenId,
    Function<String, CompletableFuture<T>> fn
  ) {
    return CompletableFuture.supplyAsync(() ->
      tokenRPC.token(new TokenRequest(recipientId, refreshTokenId))
    ).thenCompose(token -> {
      if (token == null || token.token() == null) {
        return CompletableFuture.failedFuture(
          Problem.builder()
            .withTitle("Unauthorized")
            .withDetail(
              Optional.ofNullable(token.message()).orElse("Unauthorized")
            )
            .build()
        );
      }
      var authHeader = "Bearer %s".formatted(token.token());
      return fn.apply(authHeader);
    });
  }
}
