package io.github.opendonationassistant.integration.twitch;

import io.github.opendonationassistant.integration.twitch.TwitchApiClient.DataWrapper;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.GetUserResponse;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SendChatMessageRequest;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SendChatMessageResponse;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.Stream;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SubscribeRequest;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SubscribeResponse;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.Subscription;
import io.github.opendonationassistant.integration.twitch.TwitchIdClient.GetAccessRecordResponse;
import io.github.opendonationassistant.integration.twitch.TwitchIdClient.ValidationResponse;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

@Singleton
public class TwitchClient {

  private final TwitchApiClient api;
  private final TwitchIdClient id;
  private final String clientId;
  private final String clientSecret;

  @Inject
  public TwitchClient(
    TwitchApiClient api,
    TwitchIdClient id,
    @Value("${twitch.client.id}") String clientId,
    @Value("${twitch.client.secret}") String clientSecret
  ) {
    this.api = api;
    this.id = id;
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
    String auth,
    SubscribeRequest request
  ) {
    return api.subscribe(clientId, auth, request);
  }

  public CompletableFuture<DataWrapper<Subscription[]>> getSubscriptions(
    String auth
  ) {
    return api.getSubscriptions(clientId, auth);
  }

  public CompletableFuture<Void> deleteSubscription(
    String auth,
    @Nullable String status,
    @Nullable String id
  ) {
    return api.deleteSubscription(clientId, auth, status, id);
  }

  public CompletableFuture<DataWrapper<List<Stream>>> getStreams(
    String auth,
    String userId,
    String type
  ) {
    return api.getStreams(clientId, auth, userId, type);
  }

  public CompletableFuture<DataWrapper<GetUserResponse>> getUser(
    String auth,
    String login
  ) {
    return api.getUser(clientId, auth, login);
  }

  public CompletableFuture<
    DataWrapper<List<SendChatMessageResponse>>
  > sendChatMessage(String auth, SendChatMessageRequest request) {
    return api.sendChatMessage(clientId, auth, request);
  }

  public CompletableFuture<Void> sendShoutout(
    String auth,
    String fromBroadcasterId,
    String toBroadcasterId,
    String moderatorId
  ) {
    return api.sendShoutout(
      clientId,
      auth,
      fromBroadcasterId,
      toBroadcasterId,
      moderatorId
    );
  }

  public CompletableFuture<Void> pinChatMessage(
    String auth,
    String broadcasterId,
    String moderatorId,
    String messageId,
    @Nullable Integer durationSeconds
  ) {
    return api.pinChatMessage(
      clientId,
      auth,
      broadcasterId,
      moderatorId,
      messageId,
      durationSeconds
    );
  }
}
