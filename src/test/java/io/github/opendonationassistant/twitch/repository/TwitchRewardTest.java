package io.github.opendonationassistant.twitch.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.integration.media.MediaRPC;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.DataWrapper;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SendChatMessageResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class TwitchRewardTest {

  private static final TwitchRewardData DATA = new TwitchRewardData(
    "r-1",
    "rec",
    "rt",
    "widget",
    "music"
  );

  private MediaRPC mediaRPC;
  private TwitchReward.Messaging messaging;
  private TwitchReward.RedemptionRefund refund;
  private TwitchReward reward;

  @BeforeEach
  public void setUp() {
    mediaRPC = mock(MediaRPC.class);
    messaging = mock(TwitchReward.Messaging.class);
    refund = mock(TwitchReward.RedemptionRefund.class);
    reward = new TwitchReward(DATA, mediaRPC, messaging, refund);
  }

  private void givenMediaAdded() {
    when(mediaRPC.add(any())).thenReturn(
      CompletableFuture.completedFuture(
        new MediaRPC.AddMediaResponse("media-1", "ok")
      )
    );
  }

  private void givenMediaFailed(String message) {
    when(mediaRPC.add(any())).thenReturn(
      CompletableFuture.failedFuture(new RuntimeException(message))
    );
  }

  private void givenMediaRejected(String message) {
    when(mediaRPC.add(any())).thenReturn(
      CompletableFuture.completedFuture(
        new MediaRPC.AddMediaResponse(null, message)
      )
    );
  }

  private void givenChatMessageSent() {
    when(messaging.send(any())).thenReturn(
      CompletableFuture.completedFuture(
        new DataWrapper<List<SendChatMessageResponse>>(
          List.of(new SendChatMessageResponse("m-1", true)),
          null,
          null,
          null
        )
      )
    );
  }

  private void givenRedemptionReturned() {
    when(refund.cancel(any())).thenReturn(
      CompletableFuture.<Void>completedFuture(null)
    );
  }

  @Test
  public void sendsAddMediaRequest() {
    givenMediaAdded();

    reward.addMedia("redemption-1", "media-1", "song.mp3", "bob").join();

    verify(mediaRPC).add(
      new MediaRPC.AddMediaRequest("media-1", "song.mp3", "bob", "rec", "twitch")
    );
    verifyNoInteractions(messaging, refund);
  }

  @Test
  public void refundsBeforeNotifyingChat() {
    givenMediaFailed("boom");
    givenChatMessageSent();
    var pendingRefund = new CompletableFuture<Void>();
    when(refund.cancel(any())).thenReturn(pendingRefund);

    var result = reward.addMedia(
      "redemption-1",
      "media-1",
      "song.mp3",
      "bob"
    );

    verify(refund).cancel("redemption-1");
    verify(messaging, never()).send(any());
    assertFalse(result.isDone());

    pendingRefund.complete(null);

    assertEquals("Не удалось добавить медиа: boom", captureChatMessage());
    assertTrue(result.isDone());
  }

  @Test
  public void notifiesChatAndReturnsRedemptionWhenRpcThrows() {
    givenMediaFailed("boom");
    givenChatMessageSent();
    givenRedemptionReturned();

    assertDoesNotThrow(() ->
      reward
        .addMedia("redemption-1", "media-1", "song.mp3", "bob")
        .join()
    );

    assertEquals("Не удалось добавить медиа: boom", captureChatMessage());
    verify(refund).cancel("redemption-1");
  }

  @Test
  public void notifiesChatAndReturnsRedemptionWhenRpcInvocationThrows() {
    when(mediaRPC.add(any())).thenThrow(new RuntimeException("boom"));
    givenChatMessageSent();
    givenRedemptionReturned();

    assertDoesNotThrow(() ->
      reward
        .addMedia("redemption-1", "media-1", "song.mp3", "bob")
        .join()
    );

    assertEquals("Не удалось добавить медиа: boom", captureChatMessage());
    verify(refund).cancel("redemption-1");
  }

  @Test
  public void notifiesChatAndReturnsRedemptionWhenMediaRejected() {
    givenMediaRejected("Слишком длинное видео");
    givenChatMessageSent();
    givenRedemptionReturned();

    reward.addMedia("redemption-1", "media-1", "song.mp3", "bob").join();

    assertEquals(
      "Не удалось добавить медиа: Слишком длинное видео",
      captureChatMessage()
    );
    verify(refund).cancel("redemption-1");
  }

  @Test
  public void doesNotNotifyWhenMediaWasAdded() {
    givenMediaAdded();

    reward.addMedia("redemption-1", "media-1", "song.mp3", "bob").join();

    verifyNoInteractions(messaging, refund);
  }

  @Test
  public void returnsRedemptionEvenWhenChatMessageFails() {
    givenMediaFailed("boom");
    when(messaging.send(any())).thenReturn(
      CompletableFuture.failedFuture(new RuntimeException("chat is down"))
    );
    givenRedemptionReturned();

    assertDoesNotThrow(() ->
      reward
        .addMedia("redemption-1", "media-1", "song.mp3", "bob")
        .join()
    );

    verify(refund).cancel("redemption-1");
  }

  @Test
  public void completesWithoutWaitingForChatMessage() {
    givenMediaFailed("boom");
    when(messaging.send(any())).thenReturn(new CompletableFuture<>());
    givenRedemptionReturned();

    assertTimeoutPreemptively(Duration.ofSeconds(1), () ->
      reward.addMedia("redemption-1", "media-1", "song.mp3", "bob").join()
    );

    verify(messaging).send(any());
  }

  @Test
  public void skipsReturnWhenRedemptionIdIsMissing() {
    givenMediaFailed("boom");
    givenChatMessageSent();

    reward.addMedia(null, "media-1", "song.mp3", "bob").join();

    verify(messaging).send(any());
    verifyNoInteractions(refund);
  }

  @Test
  public void doesNotFailWhenRefundThrows() {
    givenMediaFailed("boom");
    givenChatMessageSent();
    when(refund.cancel(any())).thenReturn(
      CompletableFuture.failedFuture(new RuntimeException("twitch is down"))
    );

    assertDoesNotThrow(() ->
      reward
        .addMedia("redemption-1", "media-1", "song.mp3", "bob")
        .join()
    );

    verify(messaging).send(any());
  }

  @Test
  public void truncatesChatMessageToTwitchLimit() {
    givenMediaRejected("ошибка ".repeat(100));
    givenChatMessageSent();
    givenRedemptionReturned();

    reward.addMedia("redemption-1", "media-1", "song.mp3", "bob").join();

    assertTrue(captureChatMessage().length() <= 500);
  }

  @Test
  public void usesUnknownFailureWhenReasonIsMissing() {
    givenChatMessageSent();
    givenRedemptionReturned();

    when(mediaRPC.add(any())).thenReturn(
      CompletableFuture.failedFuture(new RuntimeException())
    );

    reward.addMedia("redemption-1", "media-1", "song.mp3", "bob").join();

    assertEquals(
      "Не удалось добавить медиа: неизвестная ошибка",
      captureChatMessage()
    );
  }

  private String captureChatMessage() {
    var captor = ArgumentCaptor.forClass(String.class);
    verify(messaging).send(captor.capture());
    return captor.getValue();
  }
}
