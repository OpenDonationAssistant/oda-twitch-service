package io.github.opendonationassistant.twitch.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.integration.media.MediaRPC;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.DataWrapper;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SendChatMessageRequest;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SendChatMessageResponse;
import io.github.opendonationassistant.integration.twitch.TwitchClient;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class TwitchAccountTest {

  private static final TwitchAccountData DATA = new TwitchAccountData(
    "rec",
    "tw",
    "login",
    "rt"
  );

  private static final TwitchRewardData REWARD_DATA = new TwitchRewardData(
    "r-1",
    "rec",
    "rt",
    "widget",
    "music"
  );

  private TwitchRewardDataRepository rewards;
  private MediaRPC mediaRPC;
  private TwitchClient twitch;
  private TwitchAccount account;

  @BeforeEach
  public void setUp() {
    rewards = mock(TwitchRewardDataRepository.class);
    mediaRPC = mock(MediaRPC.class);
    twitch = mock(TwitchClient.class);
    account = new TwitchAccount(DATA, rewards, mediaRPC, twitch);
  }

  @Test
  public void returnsLinkedReward() {
    when(rewards.findByRecipientIdAndType("rec", "music")).thenReturn(
      Optional.of(REWARD_DATA)
    );

    var reward = account.reward("music");

    assertTrue(reward.isPresent());
    assertEquals(REWARD_DATA, reward.get().data());
  }

  @Test
  public void returnsEmptyWhenAccountHasNoLinkedReward() {
    when(rewards.findByRecipientIdAndType("rec", "music")).thenReturn(
      Optional.empty()
    );

    assertTrue(account.reward("music").isEmpty());
  }

  @Test
  public void sendsChatMessageWithAccountCredentials() {
    when(twitch.sendChatMessage(any(), any(), any())).thenReturn(
      CompletableFuture.completedFuture(
        new DataWrapper<List<SendChatMessageResponse>>(
          List.of(new SendChatMessageResponse("m-1", true)),
          null,
          null,
          null
        )
      )
    );

    account.sendMessage("hello").join();

    var captor = ArgumentCaptor.forClass(SendChatMessageRequest.class);
    verify(twitch).sendChatMessage(eq("rec"), eq("rt"), captor.capture());
    assertEquals("tw", captor.getValue().broadcasterId());
    assertEquals("tw", captor.getValue().senderId());
    assertEquals("hello", captor.getValue().message());
  }

  @Test
  public void cancelsRedemptionWithAccountCredentials() {
    when(
      twitch.updateRedemptionStatus(any(), any(), any(), any(), any(), any())
    ).thenReturn(CompletableFuture.<Void>completedFuture(null));

    account.cancelRedemption("r-1", "redemption-1").join();

    verify(twitch).updateRedemptionStatus(
      "rec",
      "rt",
      "tw",
      "r-1",
      "redemption-1",
      "CANCELED"
    );
  }

  @Test
  public void linkedRewardReportsFailuresThroughAccount() {
    when(rewards.findByRecipientIdAndType("rec", "music")).thenReturn(
      Optional.of(REWARD_DATA)
    );
    when(mediaRPC.add(any())).thenThrow(new RuntimeException("boom"));
    when(twitch.sendChatMessage(any(), any(), any())).thenReturn(
      CompletableFuture.completedFuture(
        new DataWrapper<List<SendChatMessageResponse>>(
          List.of(new SendChatMessageResponse("m-1", true)),
          null,
          null,
          null
        )
      )
    );
    when(
      twitch.updateRedemptionStatus(any(), any(), any(), any(), any(), any())
    ).thenReturn(CompletableFuture.<Void>completedFuture(null));

    account
      .reward("music")
      .orElseThrow()
      .addMedia("redemption-1", "media-1", "song.mp3", "bob");

    var captor = ArgumentCaptor.forClass(SendChatMessageRequest.class);
    verify(twitch).sendChatMessage(eq("rec"), eq("rt"), captor.capture());
    assertEquals("Не удалось добавить медиа: boom", captor.getValue().message());
    verify(twitch).updateRedemptionStatus(
      "rec",
      "rt",
      "tw",
      "r-1",
      "redemption-1",
      "CANCELED"
    );
  }
}
