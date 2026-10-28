package io.github.opendonationassistant.twitch.listener.handler;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.integration.twitch.TwitchClient;
import io.github.opendonationassistant.twitch.repository.TwitchWebhook;
import io.github.opendonationassistant.twitch.repository.TwitchWebhookRepository;
import io.micronaut.serde.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class UnsubscribeAllTwitchEventsHandlerTest {

  private TwitchClient twitch;
  private TwitchWebhookRepository webhookRepository;
  private UnsubscribeAllTwitchEventsHandler handler;

  @BeforeEach
  public void setUp() {
    twitch = mock(TwitchClient.class);
    webhookRepository = mock(TwitchWebhookRepository.class);
    handler = new UnsubscribeAllTwitchEventsHandler(
      mock(ObjectMapper.class),
      twitch,
      webhookRepository
    );
  }

  private static UnsubscribeAllTwitchEventsHandler.UnsubscribeAllTwitchEventsCommand command() {
    return new UnsubscribeAllTwitchEventsHandler.UnsubscribeAllTwitchEventsCommand(
      "rec",
      "rt"
    );
  }

  @Test
  public void deletesAllSubscriptionsAndClearsThem() throws IOException {
    when(webhookRepository.findByRecipientIdAndRefreshTokenId("rec", "rt"))
      .thenReturn(
        Optional.of(
          new TwitchWebhook("wh-1", "rec", "tw", "rt", List.of("s-1", "s-2"))
        )
      );
    when(twitch.deleteSubscription(any(), any()))
      .thenReturn(CompletableFuture.<Void>completedFuture(null));

    handler.handle(command());

    verify(twitch).deleteSubscription(null, "s-1");
    verify(twitch).deleteSubscription(null, "s-2");
    var captor = ArgumentCaptor.forClass(TwitchWebhook.class);
    verify(webhookRepository).update(captor.capture());
    assertTrue(captor.getValue().subscriptionIds().isEmpty());
  }

  @Test
  public void doesNothingWhenNoWebhookExists() throws IOException {
    when(webhookRepository.findByRecipientIdAndRefreshTokenId("rec", "rt"))
      .thenReturn(Optional.empty());

    handler.handle(command());

    verifyNoInteractions(twitch);
    verify(webhookRepository, never()).update(any());
  }
}
