package io.github.opendonationassistant.integration.twitch;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Patch;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

@Client("twitch-api")
public interface TwitchApiClient {
  @Post("/helix/eventsub/subscriptions")
  public CompletableFuture<DataWrapper<Subscription[]>> subscribe(
    @Header("Client-Id") String clientId,
    @Header("Authorization") String auth,
    @Body SubscribeRequest request
  );

  @Get("/helix/eventsub/subscriptions")
  public CompletableFuture<DataWrapper<Subscription[]>> getSubscriptions(
    @Header("Client-Id") String clientId,
    @Header("Authorization") String auth
  );

  @Delete("/helix/eventsub/subscriptions")
  public CompletableFuture<Void> deleteSubscription(
    @Header("Client-Id") String clientId,
    @Header("Authorization") String auth,
    @Nullable @QueryValue("status") String status,
    @Nullable @QueryValue("id") String id
  );

  @Get("/helix/streams")
  public CompletableFuture<DataWrapper<List<Stream>>> getStreams(
    @Header("Client-Id") String clientId,
    @Header("Authorization") String auth,
    @QueryValue("user_id") String userId,
    @QueryValue("type") String type
  );

  @Get("/helix/users")
  public CompletableFuture<DataWrapper<GetUserResponse>> getUser(
    @Header("Client-Id") String clientId,
    @Header("Authorization") String auth,
    @QueryValue("login") String login
  );

  @Post("/helix/chat/messages")
  CompletableFuture<DataWrapper<List<SendChatMessageResponse>>> sendChatMessage(
    @Header("Client-Id") String clientId,
    @Header("Authorization") String auth,
    @Body SendChatMessageRequest request
  );

  @Post("/helix/chat/shoutouts")
  CompletableFuture<Void> sendShoutout(
    @Header("Client-Id") String clientId,
    @Header("Authorization") String auth,
    @QueryValue("from_broadcaster_id") String fromBroadcasterId,
    @QueryValue("to_broadcaster_id") String toBroadcasterId,
    @QueryValue("moderator_id") String moderatorId
  );

  @Put("/helix/chat/pins")
  CompletableFuture<Void> pinChatMessage(
    @Header("Client-Id") String clientId,
    @Header("Authorization") String auth,
    @QueryValue("broadcaster_id") String broadcasterId,
    @QueryValue("moderator_id") String moderatorId,
    @QueryValue("message_id") String messageId,
    @Nullable @QueryValue("duration_seconds") Integer durationSeconds
  );

  @Post("/helix/channel_points/custom_rewards")
  CompletableFuture<DataWrapper<List<CustomReward>>> createCustomReward(
    @Header("Client-Id") String clientId,
    @Header("Authorization") String auth,
    @QueryValue("broadcaster_id") String broadcasterId,
    @Body CreateCustomRewardRequest request
  );

  @Patch("/helix/channel_points/custom_rewards")
  CompletableFuture<DataWrapper<List<CustomReward>>> updateCustomReward(
    @Header("Client-Id") String clientId,
    @Header("Authorization") String auth,
    @QueryValue("broadcaster_id") String broadcasterId,
    @QueryValue("id") String id,
    @Body UpdateCustomRewardRequest request
  );

  @Delete("/helix/channel_points/custom_rewards")
  CompletableFuture<Void> deleteCustomReward(
    @Header("Client-Id") String clientId,
    @Header("Authorization") String auth,
    @QueryValue("broadcaster_id") String broadcasterId,
    @QueryValue("id") String id
  );

  @Serdeable
  public static record SubscribeRequest(
    String type,
    String version,
    Map<String, String> condition,
    Transport transport
  ) {}

  @Serdeable
  public static record Subscription(
    String id,
    String type,
    String version,
    Map<String, String> condition,
    Transport transport
  ) {}

  @Serdeable
  public static record Transport(
    String method,
    String callback,
    String secret
  ) {}

  @Serdeable
  public static record SubscribeResponse(Integer total) {}

  @Serdeable
  public static record DataWrapper<T>(T data) {}

  @Serdeable
  public static record GetUserResponse(
    String id,
    String login,
    String display_name
  ) {}

  @Serdeable
  public static record Stream(
    @JsonProperty("thumbnail_url") String thumbnailUrl
  ) {}

  @Serdeable
  public static record SendChatMessageRequest(
    @JsonProperty("broadcaster_id") String broadcasterId,
    @JsonProperty("sender_id") String senderId,
    String message
  ) {}

  @Serdeable
  public static record SendChatMessageResponse(
    @JsonProperty("message_id") String messageId,
    @JsonProperty("is_sent") boolean isSent
  ) {}

  @Serdeable
  public static record CustomReward(
    String id,
    @JsonProperty("broadcaster_id") String broadcasterId,
    @JsonProperty("broadcaster_login") String broadcasterLogin,
    @JsonProperty("broadcaster_name") String broadcasterName,
    String title,
    Integer cost,
    @JsonProperty("is_enabled") Boolean isEnabled,
    @JsonProperty("is_paused") Boolean isPaused,
    @JsonProperty("is_in_stock") Boolean isInStock,
    @JsonProperty("should_redemptions_skip_request_queue")
      Boolean shouldRedemptionsSkipRequestQueue,
    @Nullable String prompt,
    @JsonProperty("is_user_input_required") Boolean isUserInputRequired,
    @JsonProperty("background_color") String backgroundColor
  ) {}

  @Serdeable
  public static record CreateCustomRewardRequest(
    String title,
    Integer cost,
    @Nullable String prompt,
    @Nullable @JsonProperty("is_enabled") Boolean isEnabled,
    @Nullable @JsonProperty("background_color") String backgroundColor,
    @Nullable @JsonProperty("is_user_input_required") Boolean isUserInputRequired,
    @Nullable @JsonProperty("is_max_per_stream_enabled") Boolean isMaxPerStreamEnabled,
    @Nullable @JsonProperty("max_per_stream") Integer maxPerStream,
    @Nullable @JsonProperty("is_max_per_user_per_stream_enabled")
      Boolean isMaxPerUserPerStreamEnabled,
    @Nullable @JsonProperty("max_per_user_per_stream") Integer maxPerUserPerStream,
    @Nullable @JsonProperty("is_global_cooldown_enabled") Boolean isGlobalCooldownEnabled,
    @Nullable @JsonProperty("global_cooldown_seconds") Integer globalCooldownSeconds,
    @Nullable @JsonProperty("should_redemptions_skip_request_queue")
      Boolean shouldRedemptionsSkipRequestQueue
  ) {}

  @Serdeable
  public static record UpdateCustomRewardRequest(
    @Nullable String title,
    @Nullable Integer cost,
    @Nullable String prompt,
    @Nullable @JsonProperty("is_enabled") Boolean isEnabled,
    @Nullable @JsonProperty("background_color") String backgroundColor,
    @Nullable @JsonProperty("is_user_input_required") Boolean isUserInputRequired,
    @Nullable @JsonProperty("is_max_per_stream_enabled") Boolean isMaxPerStreamEnabled,
    @Nullable @JsonProperty("max_per_stream") Integer maxPerStream,
    @Nullable @JsonProperty("is_max_per_user_per_stream_enabled")
      Boolean isMaxPerUserPerStreamEnabled,
    @Nullable @JsonProperty("max_per_user_per_stream") Integer maxPerUserPerStream,
    @Nullable @JsonProperty("is_global_cooldown_enabled") Boolean isGlobalCooldownEnabled,
    @Nullable @JsonProperty("global_cooldown_seconds") Integer globalCooldownSeconds,
    @Nullable @JsonProperty("should_redemptions_skip_request_queue")
      Boolean shouldRedemptionsSkipRequestQueue,
    @Nullable @JsonProperty("is_paused") Boolean isPaused
  ) {}
}
