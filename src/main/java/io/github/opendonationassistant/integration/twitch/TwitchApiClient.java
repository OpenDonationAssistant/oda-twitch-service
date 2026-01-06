package io.github.opendonationassistant.integration.twitch;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.serde.annotation.Serdeable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Client("twitch-api")
public interface TwitchApiClient {

  @Post("/helix/eventsub/subscriptions")
  public CompletableFuture<SubscribeResponse> subscribe(
    @Header("Client-Id") String clientId,
    @Header("Authorization") String auth,
    @Body SubscribeRequest request
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
  public static record Transport(
    String method,
    String callback,
    String secret
  ) {}

  @Serdeable
  public static record SubscribeResponse(Integer total){}

  @Serdeable
  public static record DataWrapper<T>(T data) {}

  @Serdeable
  public static record GetUserResponse(
    String id,
    String login,
    String display_name
  ) {}
}
