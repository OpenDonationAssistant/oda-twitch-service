package io.github.opendonationassistant.twitch.webhook;

import io.github.opendonationassistant.twitch.repository.TwitchAccountData;
import io.github.opendonationassistant.twitch.webhook.TwitchEventsWebhook.Reward;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Test-only builders for the {@code Event} payload used across webhook handler tests.
 * Every field defaults to {@code null} so each test only sets what it asserts on.
 */
final class TwitchEventFixtures {

  private TwitchEventFixtures() {}

  static final TwitchAccountData ACCOUNT = new TwitchAccountData(
    "rec",
    "tw",
    "login",
    "rt"
  );

  static final String CONTEXT_USERNAME = "bob";

  static EventContext context(TwitchEventsWebhook.Event event) {
    return new EventContext("id-1", ACCOUNT, CONTEXT_USERNAME, Optional.of(event));
  }

  static EventContext contextWithoutEvent() {
    return new EventContext("id-1", ACCOUNT, CONTEXT_USERNAME, Optional.empty());
  }

  static Builder event() {
    return new Builder();
  }

  static final class Builder {

    private @Nullable String userName;
    private @Nullable String tier;
    private @Nullable Boolean isPermanent;
    private @Nullable Object message;
    private @Nullable Integer bits;
    private @Nullable String timestamp;
    private @Nullable String fromBroadcasterName;
    private @Nullable String fromBroadcasterId;
    private @Nullable Integer viewers;
    private @Nullable Integer total;
    private @Nullable Integer cumulativeTotal;
    private @Nullable Integer streakMonths;
    private @Nullable String userInput;
    private @Nullable Reward reward;

    Builder userName(String value) {
      this.userName = value;
      return this;
    }

    Builder tier(String value) {
      this.tier = value;
      return this;
    }

    Builder isPermanent(Boolean value) {
      this.isPermanent = value;
      return this;
    }

    Builder message(Object value) {
      this.message = value;
      return this;
    }

    Builder bits(Integer value) {
      this.bits = value;
      return this;
    }

    Builder timestamp(String value) {
      this.timestamp = value;
      return this;
    }

    Builder fromBroadcasterName(String value) {
      this.fromBroadcasterName = value;
      return this;
    }

    Builder fromBroadcasterId(String value) {
      this.fromBroadcasterId = value;
      return this;
    }

    Builder viewers(Integer value) {
      this.viewers = value;
      return this;
    }

    Builder total(Integer value) {
      this.total = value;
      return this;
    }

    Builder cumulativeTotal(Integer value) {
      this.cumulativeTotal = value;
      return this;
    }

    Builder streakMonths(Integer value) {
      this.streakMonths = value;
      return this;
    }

    Builder userInput(String value) {
      this.userInput = value;
      return this;
    }

    Builder reward(TwitchEventsWebhook.Reward value) {
      this.reward = value;
      return this;
    }

    TwitchEventsWebhook.Event build() {
      return new TwitchEventsWebhook.Event(
        null,
        userName,
        tier,
        null,
        null,
        isPermanent,
        message,
        bits,
        timestamp,
        fromBroadcasterName,
        fromBroadcasterId,
        viewers,
        total,
        cumulativeTotal,
        streakMonths,
        null,
        userInput,
        reward
      );
    }
  }
}
