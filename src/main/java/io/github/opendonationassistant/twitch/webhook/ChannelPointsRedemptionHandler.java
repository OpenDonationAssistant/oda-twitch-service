package io.github.opendonationassistant.twitch.webhook;

import io.github.opendonationassistant.twitch.repository.TwitchRewardRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class ChannelPointsRedemptionHandler implements TwitchEventHandler {

  private final TwitchRewardRepository rewardRepository;

  @Inject
  public ChannelPointsRedemptionHandler(
    TwitchRewardRepository rewardRepository
  ) {
    this.rewardRepository = rewardRepository;
  }

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
        rewardRepository
          .findByRecipientIdAndType(context.account().recipientId(), "music")
          .filter(r -> r.data().id().equals(reward.id()))
          .ifPresent(r -> r.sendAddMediaCommand(userInput, userName));
      });
    return CompletableFuture.completedFuture(null);
  }
}
