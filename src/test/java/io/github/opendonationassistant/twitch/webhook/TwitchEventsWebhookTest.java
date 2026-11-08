package io.github.opendonationassistant.twitch.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.twitch.metrics.TwitchMetrics;
import io.github.opendonationassistant.twitch.repository.TwitchAccount;
import io.github.opendonationassistant.twitch.repository.TwitchAccountFixture;
import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micronaut.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

public class TwitchEventsWebhookTest {

  TwitchAccountRepository repository = mock(TwitchAccountRepository.class);

  @Test
  public void testReturnChallenge() {
    TwitchEventsWebhook webhook = new TwitchEventsWebhook(
      repository,
      List.of(),
      new TwitchMetrics(new SimpleMeterRegistry())
    );

    final HttpResponse<String> actual = webhook
      .twitchWebhook(
        "webhook_callback_verification",
        "message-1",
        new TwitchEventsWebhook.Message(
          "challenge",
          new TwitchEventsWebhook.Subscription("type", Map.of()),
          null
        )
      )
      .join();

    assertTrue(actual.code() == 200);
  }

  @Test
  public void testCountsHandledEvent() {
    var registry = new SimpleMeterRegistry();
    var metrics = new TwitchMetrics(registry);
    TwitchEventHandler handler = mock(TwitchEventHandler.class);
    when(handler.canHandle("channel.cheer")).thenReturn(true);
    when(handler.handle(any())).thenReturn(
      CompletableFuture.completedFuture(null)
    );
    givenAccount("123");
    TwitchEventsWebhook webhook = new TwitchEventsWebhook(
      repository,
      List.of(handler),
      metrics
    );

    webhook
      .twitchWebhook(
        "notification",
        "message-1",
        notification("channel.cheer")
      )
      .join();

    assertEquals(
      1,
      registry
        .counter(
          TwitchMetrics.EVENTS_RECEIVED_METRIC_NAME,
          TwitchMetrics.TYPE_TAG,
          "channel.cheer"
        )
        .count()
    );
    assertEquals(
      1,
      registry
        .counter(
          TwitchMetrics.EVENTS_HANDLED_METRIC_NAME,
          TwitchMetrics.TYPE_TAG,
          "channel.cheer"
        )
        .count()
    );
  }

  @Test
  public void testCountsUnhandledEvent() {
    var registry = new SimpleMeterRegistry();
    var metrics = new TwitchMetrics(registry);
    givenAccount("123");
    TwitchEventsWebhook webhook = new TwitchEventsWebhook(
      repository,
      List.of(),
      metrics
    );

    webhook
      .twitchWebhook(
        "notification",
        "message-1",
        notification("channel.unknown")
      )
      .join();

    assertEquals(
      1,
      registry
        .counter(
          TwitchMetrics.EVENTS_UNHANDLED_METRIC_NAME,
          TwitchMetrics.TYPE_TAG,
          "channel.unknown"
        )
        .count()
    );
  }

  @Test
  public void testCountsNoAccountEvent() {
    var registry = new SimpleMeterRegistry();
    var metrics = new TwitchMetrics(registry);
    when(repository.findByTwitchId("123")).thenReturn(
      CompletableFuture.completedFuture(Optional.empty())
    );
    TwitchEventsWebhook webhook = new TwitchEventsWebhook(
      repository,
      List.of(),
      metrics
    );

    webhook
      .twitchWebhook(
        "notification",
        "message-1",
        notification("channel.cheer")
      )
      .join();

    assertEquals(
      1,
      registry
        .counter(
          TwitchMetrics.EVENTS_NOACCOUNT_METRIC_NAME,
          TwitchMetrics.TYPE_TAG,
          "channel.cheer"
        )
        .count()
    );
  }

  @Test
  public void doesNotBlockWhileAccountLookupIsPending() {
    var registry = new SimpleMeterRegistry();
    var pending = new CompletableFuture<Optional<TwitchAccount>>();
    when(repository.findByTwitchId("123")).thenReturn(pending);
    TwitchEventsWebhook webhook = new TwitchEventsWebhook(
      repository,
      List.of(),
      new TwitchMetrics(registry)
    );

    var result = webhook.twitchWebhook(
      "notification",
      "message-1",
      notification("channel.cheer")
    );

    assertFalse(result.isDone());

    pending.complete(Optional.empty());

    assertTrue(result.isDone());
    result.join();
    assertEquals(
      1,
      registry
        .counter(
          TwitchMetrics.EVENTS_NOACCOUNT_METRIC_NAME,
          TwitchMetrics.TYPE_TAG,
          "channel.cheer"
        )
        .count()
    );
  }

  @Test
  public void completesOnlyAfterHandlerCompletes() {
    TwitchEventHandler handler = mock(TwitchEventHandler.class);
    when(handler.canHandle("channel.cheer")).thenReturn(true);
    var pending = new CompletableFuture<Void>();
    doReturn(pending).when(handler).handle(any());
    givenAccount("123");
    TwitchEventsWebhook webhook = new TwitchEventsWebhook(
      repository,
      List.of(handler),
      new TwitchMetrics(new SimpleMeterRegistry())
    );

    var result = webhook.twitchWebhook(
      "notification",
      "message-1",
      notification("channel.cheer")
    );

    assertFalse(result.isDone());

    pending.complete(null);

    assertTrue(result.isDone());
    result.join();
  }

  private void givenAccount(String twitchId) {
    when(repository.findByTwitchId(twitchId)).thenReturn(
      CompletableFuture.completedFuture(
        Optional.of(TwitchAccountFixture.account())
      )
    );
  }

  private static TwitchEventsWebhook.Message notification(String type) {
    return new TwitchEventsWebhook.Message(
      null,
      new TwitchEventsWebhook.Subscription(
        type,
        Map.of("broadcaster_user_id", "123")
      ),
      null
    );
  }
}
