package io.github.opendonationassistant.twitch.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

@Singleton
public class TwitchMetrics {

  public static final String EVENTS_RECEIVED_METRIC_NAME =
    "twitch.events.received";
  public static final String EVENTS_HANDLED_METRIC_NAME =
    "twitch.events.handled";
  public static final String EVENTS_UNHANDLED_METRIC_NAME =
    "twitch.events.unhandled";
  public static final String EVENTS_NOACCOUNT_METRIC_NAME =
    "twitch.events.noaccount";
  public static final String TYPE_TAG = "type";
  public static final String UNKNOWN = "unknown";

  private final MeterRegistry registry;

  @Inject
  public TwitchMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  public void eventReceived(@Nullable String type) {
    counter(EVENTS_RECEIVED_METRIC_NAME, type).increment();
  }

  public void eventHandled(@Nullable String type) {
    counter(EVENTS_HANDLED_METRIC_NAME, type).increment();
  }

  public void eventUnhandled(@Nullable String type) {
    counter(EVENTS_UNHANDLED_METRIC_NAME, type).increment();
  }

  public void eventNoAccount(@Nullable String type) {
    counter(EVENTS_NOACCOUNT_METRIC_NAME, type).increment();
  }

  private Counter counter(String name, @Nullable String type) {
    return registry.counter(
      name,
      TYPE_TAG,
      Optional.ofNullable(type).orElse(UNKNOWN)
    );
  }
}

