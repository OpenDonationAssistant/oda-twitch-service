package io.github.opendonationassistant.twitch.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

public class TwitchMetricsTest {

  private static final String CHEER = "channel.cheer";

  @Test
  public void countsReceivedEventsPerType() {
    var registry = new SimpleMeterRegistry();
    var metrics = new TwitchMetrics(registry);

    metrics.eventReceived(CHEER);
    metrics.eventReceived(CHEER);
    metrics.eventReceived("channel.follow");

    assertEquals(
      2,
      registry
        .counter(
          TwitchMetrics.EVENTS_RECEIVED_METRIC_NAME,
          TwitchMetrics.TYPE_TAG,
          CHEER
        )
        .count()
    );
    assertEquals(
      1,
      registry
        .counter(
          TwitchMetrics.EVENTS_RECEIVED_METRIC_NAME,
          TwitchMetrics.TYPE_TAG,
          "channel.follow"
        )
        .count()
    );
  }

  @Test
  public void countsHandledEventsPerType() {
    var registry = new SimpleMeterRegistry();
    var metrics = new TwitchMetrics(registry);

    metrics.eventHandled(CHEER);
    metrics.eventHandled(CHEER);

    assertEquals(
      2,
      registry
        .counter(
          TwitchMetrics.EVENTS_HANDLED_METRIC_NAME,
          TwitchMetrics.TYPE_TAG,
          CHEER
        )
        .count()
    );
  }

  @Test
  public void countsUnhandledEventsPerType() {
    var registry = new SimpleMeterRegistry();
    var metrics = new TwitchMetrics(registry);

    metrics.eventUnhandled(CHEER);

    assertEquals(
      1,
      registry
        .counter(
          TwitchMetrics.EVENTS_UNHANDLED_METRIC_NAME,
          TwitchMetrics.TYPE_TAG,
          CHEER
        )
        .count()
    );
  }

  @Test
  public void countsNoAccountEventsPerType() {
    var registry = new SimpleMeterRegistry();
    var metrics = new TwitchMetrics(registry);

    metrics.eventNoAccount(CHEER);

    assertEquals(
      1,
      registry
        .counter(
          TwitchMetrics.EVENTS_NOACCOUNT_METRIC_NAME,
          TwitchMetrics.TYPE_TAG,
          CHEER
        )
        .count()
    );
  }

  @Test
  public void mapsMissingTypeToUnknown() {
    var registry = new SimpleMeterRegistry();
    var metrics = new TwitchMetrics(registry);

    metrics.eventReceived(null);

    assertEquals(
      1,
      registry
        .counter(
          TwitchMetrics.EVENTS_RECEIVED_METRIC_NAME,
          TwitchMetrics.TYPE_TAG,
          TwitchMetrics.UNKNOWN
        )
        .count()
    );
  }
}
