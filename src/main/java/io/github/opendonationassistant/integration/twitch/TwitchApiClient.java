package io.github.opendonationassistant.integration.twitch;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
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
  public CompletableFuture<SubscribeResponse> subscribe(
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
  public CompletableFuture<SubscribeResponse> deleteSubscription(
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
}
