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
    context
      .event()
      .ifPresent(it -> {
        var reward = it.reward();
        var userInput = it.userInput();
        var userName = it.userName();
        if (reward == null || userInput == null || userName == null) {
          return;
        }
        context
          .account()
          .reward("music")
          .filter(r -> r.data().id().equals(reward.id()))
          .ifPresent(r ->
            r.addMedia(
              it.id(),
              mediaId(context.messageId()),
              userInput,
              userName
            )
          );
      });
    return CompletableFuture.completedFuture(null);
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
