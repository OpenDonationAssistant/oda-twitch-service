package io.github.opendonationassistant.twitch.webhook;

import io.github.opendonationassistant.events.twitch.TwitchFacade;
import io.github.opendonationassistant.events.twitch.events.TwitchStreamStartedEvent;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient;
import io.github.opendonationassistant.integration.twitch.TwitchIdClient;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
public class StreamOnlineHandler implements TwitchEventHandler {

  private final TwitchApiClient api;
  private final TwitchIdClient idClient;
  private final TwitchFacade facade;
  private final String clientId;
  private final String clientSecret;

  @Inject
  public StreamOnlineHandler(
    TwitchApiClient api,
    TwitchIdClient idClient,
    TwitchFacade facade,
    @Value("${twitch.client.id}") String clientId,
    @Value("${twitch.client.secret}") String clientSecret
  ) {
    this.api = api;
    this.idClient = idClient;
    this.facade = facade;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }

  @Override
  public boolean canHandle(String type) {
    return "stream.online".equals(type);
  }

  @Override
  public CompletableFuture<?> handle(EventContext context) {
    return getToken()
      .thenCompose(response ->
        api.getStreams(
          clientId,
          response.accessToken(),
          context.account().twitchId(),
          "live"
        )
      )
      .thenAccept(stream -> {
        Optional.ofNullable(stream.data().getFirst())
          .map(TwitchApiClient.Stream::thumbnailUrl)
          .ifPresent(thumbnailUrl ->
            facade.sendEvent(
              new TwitchStreamStartedEvent(
                context.id(),
                context.account().recipientId(),
                thumbnailUrl
              )
            )
          );
      });
  }

  private CompletableFuture<TwitchIdClient.GetAccessRecordResponse> getToken() {
    var params = new HashMap<String, String>();
    params.put("client_id", clientId);
    params.put("client_secret", clientSecret);
    params.put("grant_type", "client_credentials");
    return idClient.getToken(params);
  }
}
