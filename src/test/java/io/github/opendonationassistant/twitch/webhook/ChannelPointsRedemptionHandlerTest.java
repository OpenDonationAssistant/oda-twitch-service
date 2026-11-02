package io.github.opendonationassistant.twitch.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.twitch.repository.TwitchAccount;
import io.github.opendonationassistant.twitch.repository.TwitchReward;
import io.github.opendonationassistant.twitch.repository.TwitchRewardData;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ChannelPointsRedemptionHandlerTest {

  private static ExecutorService directExecutor() {
    return new AbstractExecutorService() {
      @Override
      public void shutdown() {}

      @Override
      public List<Runnable> shutdownNow() {
        return List.of();
      }

      @Override
      public boolean isShutdown() {
        return false;
      }

      @Override
      public boolean isTerminated() {
        return false;
      }

      @Override
      public boolean awaitTermination(long timeout, TimeUnit unit) {
        return true;
      }

      @Override
      public void execute(Runnable command) {
        command.run();
      }
    };
  }

  private TwitchAccount account;
  private ChannelPointsRedemptionHandler handler;

  @BeforeEach
  public void setUp() {
    account = mock(TwitchAccount.class);
    handler = new ChannelPointsRedemptionHandler();
  }

  private static TwitchEventsWebhook.Event event(
    String rewardId,
    @Nullable String userInput,
    @Nullable String userName
  ) {
    return new TwitchEventsWebhook.Event(
      "redemption-1",
      null,
      userName,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      userInput,
      new TwitchEventsWebhook.Reward(rewardId, "Song", 100, null)
    );
  }

  private static TwitchEventsWebhook.Event eventWithoutReward(
    String userInput
  ) {
    return new TwitchEventsWebhook.Event(
      "redemption-1",
      null,
      "bob",
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      userInput,
      null
    );
  }

  private EventContext context(TwitchEventsWebhook.Event event) {
    return context("message-1", event);
  }

  private EventContext context(
    @Nullable String messageId,
    TwitchEventsWebhook.Event event
  ) {
    return new EventContext(
      "id-1",
      messageId,
      account,
      "bob",
      Optional.of(event)
    );
  }

  @Test
  public void handlesPointsRedemptionEventsOnly() {
    assertTrue(
      handler.canHandle("channel.channel_points_custom_reward_redemption.add")
    );
    assertFalse(handler.canHandle("channel.follow"));
  }

  @Test
  public void addsMediaForMatchingReward() {
    var reward = matchingReward();

    handler.handle(context(event("r-1", "song.mp3", "bob"))).join();

    verify(reward).addMedia(
      "redemption-1",
      mediaIdOf("message-1"),
      "song.mp3",
      "bob"
    );
  }

  @Test
  public void ignoresRedemptionForUnknownReward() {
    var reward = matchingReward();

    handler.handle(context(event("r-2", "song.mp3", "bob"))).join();

    verify(reward, never()).addMedia(any(), any(), any(), any());
  }

  @Test
  public void ignoresRedemptionWhenAccountHasNoLinkedReward() {
    when(account.reward("music")).thenReturn(Optional.empty());

    handler.handle(context(event("r-1", "song.mp3", "bob"))).join();

    verify(account).reward("music");
  }

  @Test
  public void ignoresContextWithoutEvent() {
    handler
      .handle(
        new EventContext("id-1", "message-1", account, "bob", Optional.empty())
      )
      .join();

    verifyNoInteractions(account);
  }

  @Test
  public void ignoresRedemptionWithoutReward() {
    handler.handle(context(eventWithoutReward("song.mp3"))).join();

    verifyNoInteractions(account);
  }

  @Test
  public void ignoresRedemptionWithoutUserInput() {
    handler.handle(context(event("r-1", null, "bob"))).join();

    verifyNoInteractions(account);
  }

  @Test
  public void ignoresRedemptionWithoutUserName() {
    handler.handle(context(event("r-1", "song.mp3", null))).join();

    verifyNoInteractions(account);
  }

  @Test
  public void derivesSameMediaIdForRetriedDelivery() {
    var reward = matchingReward();
    var event = event("r-1", "song.mp3", "bob");

    handler.handle(context("message-1", event)).join();
    handler.handle(context("message-1", event)).join();

    var captor = ArgumentCaptor.forClass(String.class);
    verify(reward, times(2)).addMedia(
      eq("redemption-1"),
      captor.capture(),
      eq("song.mp3"),
      eq("bob")
    );
    assertEquals(mediaIdOf("message-1"), captor.getAllValues().get(0));
    assertEquals(captor.getAllValues().get(0), captor.getAllValues().get(1));
  }

  @Test
  public void derivesDifferentMediaIdsForDifferentDeliveries() {
    var reward = matchingReward();
    var event = event("r-1", "song.mp3", "bob");

    handler.handle(context("message-1", event)).join();
    handler.handle(context("message-2", event)).join();

    var captor = ArgumentCaptor.forClass(String.class);
    verify(reward, times(2)).addMedia(
      eq("redemption-1"),
      captor.capture(),
      eq("song.mp3"),
      eq("bob")
    );
    assertNotEquals(captor.getAllValues().get(0), captor.getAllValues().get(1));
  }

  @Test
  public void generatesMediaIdWhenMessageIdIsMissing() {
    var reward = matchingReward();

    handler.handle(context(null, event("r-1", "song.mp3", "bob"))).join();

    var captor = ArgumentCaptor.forClass(String.class);
    verify(reward).addMedia(
      eq("redemption-1"),
      captor.capture(),
      eq("song.mp3"),
      eq("bob")
    );
    assertEquals(
      captor.getValue(),
      UUID.fromString(captor.getValue()).toString()
    );
  }

  private TwitchReward matchingReward() {
    var reward = mock(TwitchReward.class);
    when(reward.data()).thenReturn(
      new TwitchRewardData("r-1", "rec", "rt", "widget", "music")
    );
    when(account.reward("music")).thenReturn(Optional.of(reward));
    return reward;
  }

  private static String mediaIdOf(String messageId) {
    return UUID.nameUUIDFromBytes(
      messageId.getBytes(StandardCharsets.UTF_8)
    ).toString();
  }
}
