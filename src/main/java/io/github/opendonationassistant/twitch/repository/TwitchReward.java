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
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.jspecify.annotations.Nullable;

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

  public CompletableFuture<Void> addMedia(
    @Nullable String redemptionId,
    String mediaId,
    String url,
    String requester
  ) {
    final MediaRPC.AddMediaRequest request = new MediaRPC.AddMediaRequest(
      mediaId,
      url,
      requester,
      data.recipientId(),
      "twitch"
    );
    final CompletableFuture<MediaRPC.AddMediaResponse> response;
    try {
      response = mediaRPC.add(request);
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
      return handleFailure(redemptionId, e.getMessage());
    }
    return response
      .thenCompose(it -> {
        if (it.id() == null) {
          log.error(
            "Media service rejected add media request",
            Map.of(
              "recipientId",
              data.recipientId(),
              "requester",
              requester,
              "message",
              Objects.toString(it.message(), "")
            )
          );
          return handleFailure(redemptionId, it.message());
        }
        log.info(
          "Added media from reward redemption",
          Map.of(
            "recipientId",
            data.recipientId(),
            "requester",
            requester,
            "mediaId",
            it.id(),
            "message",
            Objects.toString(it.message(), "")
          )
        );
        return CompletableFuture.<Void>completedFuture(null);
      })
      .exceptionallyCompose(failure -> {
        log.error(
          "Failed to add media from reward redemption",
          Map.of(
            "recipientId",
            data.recipientId(),
            "requester",
            requester,
            "error",
            Objects.toString(failureReason(failure), "")
          )
        );
        return handleFailure(redemptionId, failureReason(failure));
      });
  }

  private CompletableFuture<Void> handleFailure(
    @Nullable String redemptionId,
    @Nullable String reason
  ) {
    final CompletableFuture<Void> refunded = returnRedemption(redemptionId);
    refunded.whenComplete((ignored, failure) -> sendFailureChatMessage(reason));
    return refunded;
  }

  private CompletableFuture<Void> sendFailureChatMessage(
    @Nullable String reason
  ) {
    try {
      return messaging
        .send(failureChatMessage(reason))
        .thenAccept(response -> {
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
        })
        .exceptionally(failure -> {
          log.error(
            "Failed to send chat message about failed media request: recipientId=" +
            data.recipientId(),
            asException(failure)
          );
          return null;
        });
    } catch (RuntimeException e) {
      log.error(
        "Failed to send chat message about failed media request: recipientId=" +
        data.recipientId(),
        e
      );
      return CompletableFuture.completedFuture(null);
    }
  }

  private CompletableFuture<Void> returnRedemption(
    @Nullable String redemptionId
  ) {
    if (redemptionId == null) {
      log.warn(
        "Cannot return reward redemption without redemption id",
        Map.of("recipientId", data.recipientId(), "rewardId", data.id())
      );
      return CompletableFuture.completedFuture(null);
    }
    final CompletableFuture<Void> cancellation;
    try {
      cancellation = refund.cancel(redemptionId);
    } catch (RuntimeException e) {
      log.error(
        "Failed to return reward redemption: recipientId=" +
        data.recipientId() +
        ", redemptionId=" +
        redemptionId,
        e
      );
      return CompletableFuture.completedFuture(null);
    }
    return cancellation
      .thenAccept(ignored ->
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
        )
      )
      .exceptionally(failure -> {
        log.error(
          "Failed to return reward redemption: recipientId=" +
          data.recipientId() +
          ", redemptionId=" +
          redemptionId,
          asException(failure)
        );
        return null;
      });
  }

  private static Exception asException(Throwable failure) {
    return failure instanceof Exception exception
      ? exception
      : new RuntimeException(failure);
  }

  private static @Nullable String failureReason(Throwable failure) {
    if (
      failure instanceof CompletionException ||
      failure instanceof ExecutionException
    ) {
      final Throwable cause = failure.getCause();
      if (cause != null) {
        return cause.getMessage();
      }
    }
    return failure.getMessage();
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
