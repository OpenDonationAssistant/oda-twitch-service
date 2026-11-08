package io.github.opendonationassistant.twitch.webhook;

import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

@Singleton
public class ChannelPointsRedemptionHandler implements TwitchEventHandler {

  @Override
  public boolean canHandle(String type) {
    return "channel.channel_points_custom_reward_redemption.add".equals(type);
  }

  @Override
  public CompletableFuture<?> handle(EventContext context) {
    return context
      .event()
      .map(it -> handle(context, it))
      .orElseGet(() -> CompletableFuture.completedFuture(null));
  }

  private CompletableFuture<?> handle(
    EventContext context,
    TwitchEventsWebhook.Event event
  ) {
    var reward = event.reward();
    var userInput = event.userInput();
    var userName = event.userName();
    if (reward == null || userInput == null || userName == null) {
      return CompletableFuture.completedFuture(null);
    }
    return context
      .account()
      .reward("music")
      .thenCompose(it ->
        it
          .filter(r -> r.data().id().equals(reward.id()))
          .map(r ->
            r.addMedia(
              event.id(),
              mediaId(context.messageId()),
              userInput,
              userName
            )
          )
          .orElseGet(() -> CompletableFuture.<Void>completedFuture(null))
      );
  }

  private static String mediaId(@Nullable String messageId) {
    if (messageId == null || messageId.isBlank()) {
      return UUID.randomUUID().toString();
    }
    return UUID.nameUUIDFromBytes(
      messageId.getBytes(StandardCharsets.UTF_8)
    ).toString();
  }
}
