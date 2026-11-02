package io.github.opendonationassistant.twitch.repository;

import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.integration.media.MediaRPC;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.DataWrapper;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SendChatMessageResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

/**
 * A reward linked to a {@link TwitchAccount}.
 *
 * <p>The reward does not look up its account: the account constructs it and
 * supplies the messaging and refund functions bound to that account, so chat
 * notifications and redemption refunds always use the account's current
 * credentials.
 */
public class TwitchReward {

  private static final int MAX_CHAT_MESSAGE_LENGTH = 500;
  private static final String UNKNOWN_FAILURE = "неизвестная ошибка";

  /** Sends a chat message as the linked account. */
  @FunctionalInterface
  public interface Messaging {
    CompletableFuture<DataWrapper<List<SendChatMessageResponse>>> send(
      String content
    );
  }

  /** Cancels (refunds) a redemption of this reward. */
  @FunctionalInterface
  public interface RedemptionRefund {
    CompletableFuture<Void> cancel(String redemptionId);
  }

  private final ODALogger log = new ODALogger(this);
  private final TwitchRewardData data;
  private final MediaRPC mediaRPC;
  private final Messaging messaging;
  private final RedemptionRefund refund;

  public TwitchReward(
    TwitchRewardData data,
    MediaRPC mediaRPC,
    Messaging messaging,
    RedemptionRefund refund
  ) {
    this.data = data;
    this.mediaRPC = mediaRPC;
    this.messaging = messaging;
    this.refund = refund;
  }

  public TwitchRewardData data() {
    return data;
  }

  public void addMedia(
    @Nullable String redemptionId,
    String mediaId,
    String url,
    String requester
  ) {
    try {
      var response = mediaRPC.add(
        new MediaRPC.AddMediaRequest(
          mediaId,
          url,
          requester,
          data.recipientId(),
          "twitch"
        )
      );
      if (response.id() == null) {
        log.error(
          "Media service rejected add media request",
          Map.of(
            "recipientId",
            data.recipientId(),
            "requester",
            requester,
            "message",
            Objects.toString(response.message(), "")
          )
        );
        handleFailure(redemptionId, response.message());
        return;
      }
      log.info(
        "Added media from reward redemption",
        Map.of(
          "recipientId",
          data.recipientId(),
          "requester",
          requester,
          "mediaId",
          response.id(),
          "message",
          Objects.toString(response.message(), "")
        )
      );
    } catch (RuntimeException e) {
      log.error(
        "Failed to add media from reward redemption",
        Map.of(
          "recipientId",
          data.recipientId(),
          "requester",
          requester,
          "error",
          Objects.toString(e.getMessage(), "")
        )
      );
      handleFailure(redemptionId, e.getMessage());
    }
  }

  private void handleFailure(
    @Nullable String redemptionId,
    @Nullable String reason
  ) {
    sendFailureChatMessage(reason);
    returnRedemption(redemptionId);
  }

  private void sendFailureChatMessage(@Nullable String reason) {
    try {
      var response = messaging.send(failureChatMessage(reason)).join();
      if (response != null && response.error() != null) {
        log.error(
          "Failed to send chat message about failed media request",
          Map.of(
            "recipientId",
            data.recipientId(),
            "error",
            response.error(),
            "message",
            Objects.toString(response.message(), "")
          )
        );
      }
    } catch (RuntimeException e) {
      log.error(
        "Failed to send chat message about failed media request: recipientId=" +
        data.recipientId(),
        e
      );
    }
  }

  private void returnRedemption(@Nullable String redemptionId) {
    if (redemptionId == null) {
      log.warn(
        "Cannot return reward redemption without redemption id",
        Map.of("recipientId", data.recipientId(), "rewardId", data.id())
      );
      return;
    }
    try {
      refund.cancel(redemptionId).join();
      log.info(
        "Returned reward redemption after failed media request",
        Map.of(
          "recipientId",
          data.recipientId(),
          "rewardId",
          data.id(),
          "redemptionId",
          redemptionId
        )
      );
    } catch (RuntimeException e) {
      log.error(
        "Failed to return reward redemption: recipientId=" +
        data.recipientId() +
        ", redemptionId=" +
        redemptionId,
        e
      );
    }
  }

  private static String failureChatMessage(@Nullable String reason) {
    var detail = Optional.ofNullable(reason)
      .map(String::strip)
      .filter(it -> !it.isEmpty())
      .orElse(UNKNOWN_FAILURE);
    var message = "Не удалось добавить медиа: " + detail;
    return message.length() <= MAX_CHAT_MESSAGE_LENGTH
      ? message
      : message.substring(0, MAX_CHAT_MESSAGE_LENGTH);
  }
}
