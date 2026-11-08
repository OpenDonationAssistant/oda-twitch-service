package io.github.opendonationassistant.twitch.repository;

import io.github.opendonationassistant.integration.media.MediaRPC;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.DataWrapper;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SendChatMessageRequest;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SendChatMessageResponse;
import io.github.opendonationassistant.integration.twitch.TwitchClient;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class TwitchAccount {

  private static final String CANCELED_REDEMPTION_STATUS = "CANCELED";

  private final TwitchAccountData data;
  private final TwitchRewardDataRepository rewards;
  private final MediaRPC mediaRPC;
  private final TwitchClient twitch;

  public TwitchAccount(
    TwitchAccountData data,
    TwitchRewardDataRepository rewards,
    MediaRPC mediaRPC,
    TwitchClient twitch
  ) {
    this.data = data;
    this.rewards = rewards;
    this.mediaRPC = mediaRPC;
    this.twitch = twitch;
  }

  public TwitchAccountData data() {
    return data;
  }

  public String recipientId() {
    return data.recipientId();
  }

  public String twitchId() {
    return data.twitchId();
  }

  public String refreshTokenId() {
    return data.refreshTokenId();
  }

  public CompletableFuture<Optional<TwitchReward>> reward(String type) {
    return rewards
      .findByRecipientIdAndType(data.recipientId(), type)
      .thenApply(found ->
        found.map(it ->
          new TwitchReward(
            it,
            mediaRPC,
            this::sendMessage,
            redemptionId -> cancelRedemption(it.id(), redemptionId)
          )
        )
      );
  }

  public CompletableFuture<DataWrapper<List<SendChatMessageResponse>>> sendMessage(
    String content
  ) {
    return twitch.sendChatMessage(
      data.recipientId(),
      data.refreshTokenId(),
      new SendChatMessageRequest(
        data.twitchId(),
        data.twitchId(),
        content
      )
    );
  }

  public CompletableFuture<Void> cancelRedemption(
    String rewardId,
    String redemptionId
  ) {
    return twitch.updateRedemptionStatus(
      data.recipientId(),
      data.refreshTokenId(),
      data.twitchId(),
      rewardId,
      redemptionId,
      CANCELED_REDEMPTION_STATUS
    );
  }
}
