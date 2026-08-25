package io.github.opendonationassistant.twitch.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.twitch.metrics.TwitchMetrics;
import io.github.opendonationassistant.twitch.repository.TwitchAccountData;
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
        new TwitchEventsWebhook.Message(
          "challenge",
          new TwitchEventsWebhook.Subscription("type", Map.of()),
          null
        )
      )
      .join();

    assertTrue(actual.code() == 200);
    // assertEquals(Optional.of("challenge"), actual.body());
  }

  @Test
  public void testCountsHandledEvent() {
    var registry = new SimpleMeterRegistry();
    var metrics = new TwitchMetrics(registry);
    TwitchEventHandler handler = mock(TwitchEventHandler.class);
    when(handler.canHandle("channel.cheer")).thenReturn(true);
    when(handler.handle(any())).thenReturn(CompletableFuture.completedFuture(null));
    when(repository.findByTwitchId("123"))
      .thenReturn(
        Optional.of(
          new TwitchAccountData("recipient", "123", "login", "token")
        )
      );
    TwitchEventsWebhook webhook = new TwitchEventsWebhook(
      repository,
      List.of(handler),
      metrics
    );

    webhook
      .twitchWebhook(
        "notification",
        new TwitchEventsWebhook.Message(
          null,
          new TwitchEventsWebhook.Subscription(
            "channel.cheer",
            Map.of("broadcaster_user_id", "123")
          ),
          null
        )
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
    when(repository.findByTwitchId("123"))
      .thenReturn(
        Optional.of(
          new TwitchAccountData("recipient", "123", "login", "token")
        )
      );
    TwitchEventsWebhook webhook = new TwitchEventsWebhook(
      repository,
      List.of(),
      metrics
    );

    webhook
      .twitchWebhook(
        "notification",
        new TwitchEventsWebhook.Message(
          null,
          new TwitchEventsWebhook.Subscription(
            "channel.unknown",
            Map.of("broadcaster_user_id", "123")
          ),
          null
        )
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
    when(repository.findByTwitchId("123")).thenReturn(Optional.empty());
    TwitchEventsWebhook webhook = new TwitchEventsWebhook(
      repository,
      List.of(),
      metrics
    );

    webhook
      .twitchWebhook(
        "notification",
        new TwitchEventsWebhook.Message(
          null,
          new TwitchEventsWebhook.Subscription(
            "channel.cheer",
            Map.of("broadcaster_user_id", "123")
          ),
          null
        )
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
}
