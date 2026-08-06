package io.github.opendonationassistant.twitch.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import io.micronaut.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class TwitchEventsWebhookTest {

  TwitchAccountRepository repository = mock(TwitchAccountRepository.class);

  @Test
  public void testReturnChallenge() {
    TwitchEventsWebhook webhook = new TwitchEventsWebhook(
      repository,
      List.of()
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
}
