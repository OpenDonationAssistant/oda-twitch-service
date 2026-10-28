package io.github.opendonationassistant.twitch.webhook;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.twitch.repository.TwitchAccountData;
import io.github.opendonationassistant.twitch.repository.TwitchReward;
import io.github.opendonationassistant.twitch.repository.TwitchRewardData;
import io.github.opendonationassistant.twitch.repository.TwitchRewardRepository;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ChannelPointsRedemptionHandlerTest {

  private static final TwitchAccountData ACCOUNT = new TwitchAccountData(
    "rec",
    "tw",
    "login",
    "rt"
  );

  private TwitchRewardRepository rewardRepository;
  private ChannelPointsRedemptionHandler handler;

  @BeforeEach
  public void setUp() {
    rewardRepository = mock(TwitchRewardRepository.class);
    handler = new ChannelPointsRedemptionHandler(rewardRepository);
  }

  private static TwitchEventsWebhook.Event event(
    String rewardId,
    @Nullable String userInput,
    @Nullable String userName
  ) {
    return new TwitchEventsWebhook.Event(
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

  private static TwitchEventsWebhook.Event eventWithoutReward(String userInput) {
    return new TwitchEventsWebhook.Event(
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

  private static EventContext context(TwitchEventsWebhook.Event event) {
    return new EventContext("id-1", ACCOUNT, "bob", Optional.of(event));
  }

  @Test
  public void handlesPointsRedemptionEventsOnly() {
    assertTrue(
      handler.canHandle("channel.channel_points_custom_reward_redemption.add")
    );
    assertFalse(handler.canHandle("channel.follow"));
  }

  @Test
  public void sendsAddMediaCommandForMatchingReward() {
    var reward = mock(TwitchReward.class);
    when(reward.data())
      .thenReturn(new TwitchRewardData("r-1", "rec", "rt", "widget", "music"));
    when(rewardRepository.findByRecipientIdAndType("rec", "music"))
      .thenReturn(Optional.of(reward));

    handler.handle(context(event("r-1", "song.mp3", "bob"))).join();

    verify(reward).sendAddMediaCommand("song.mp3", "bob");
  }

  @Test
  public void ignoresRedemptionForUnknownReward() {
    var reward = mock(TwitchReward.class);
    when(reward.data())
      .thenReturn(new TwitchRewardData("r-1", "rec", "rt", "widget", "music"));
    when(rewardRepository.findByRecipientIdAndType("rec", "music"))
      .thenReturn(Optional.of(reward));

    handler.handle(context(event("r-2", "song.mp3", "bob"))).join();

    verify(reward, never()).sendAddMediaCommand(any(), any());
  }

  @Test
  public void ignoresContextWithoutEvent() {
    handler
      .handle(new EventContext("id-1", ACCOUNT, "bob", Optional.empty()))
      .join();

    verifyNoInteractions(rewardRepository);
  }

  @Test
  public void ignoresRedemptionWithoutReward() {
    handler.handle(context(eventWithoutReward("song.mp3"))).join();

    verifyNoInteractions(rewardRepository);
  }

  @Test
  public void ignoresRedemptionWithoutUserInput() {
    handler.handle(context(event("r-1", null, "bob"))).join();

    verifyNoInteractions(rewardRepository);
  }

  @Test
  public void ignoresRedemptionWithoutUserName() {
    handler.handle(context(event("r-1", "song.mp3", null))).join();

    verifyNoInteractions(rewardRepository);
  }
}
