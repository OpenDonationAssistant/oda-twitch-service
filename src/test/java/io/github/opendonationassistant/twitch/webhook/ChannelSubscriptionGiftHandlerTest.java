package io.github.opendonationassistant.twitch.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.events.twitch.TwitchFacade;
import io.github.opendonationassistant.events.twitch.events.TwitchChannelSubscriptionGiftEvent;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ChannelSubscriptionGiftHandlerTest {

  private TwitchFacade facade;
  private ChannelSubscriptionGiftHandler handler;

  @BeforeEach
  public void setUp() {
    facade = mock(TwitchFacade.class);
    when(facade.sendEvent(any()))
      .thenReturn(CompletableFuture.<Void>completedFuture(null));
    handler = new ChannelSubscriptionGiftHandler(facade);
  }

  @Test
  public void handlesSubscriptionGiftEventsOnly() {
    assertTrue(handler.canHandle("channel.subscription.gift"));
    assertFalse(handler.canHandle("channel.subscription.message"));
  }

  @Test
  public void sendsGiftEventWithTierAndCounts() {
    handler
      .handle(
        TwitchEventFixtures.context(
          TwitchEventFixtures
            .event()
            .userName("bob")
            .tier("1000")
            .total(5)
            .cumulativeTotal(20)
            .build()
        )
      )
      .join();

    var captured = capturedEvent();
    assertEquals("id-1", captured.id());
    assertEquals("rec", captured.recipientId());
    assertEquals("bob", captured.username());
    assertEquals("1000", captured.tier());
    // The outbound record maps event.total -> amount and event.cumulative_total -> total.
    assertEquals(5, captured.amount().intValue());
    assertEquals(20, captured.total().intValue());
  }

  @Test
  public void defaultsFieldsWhenEventFieldsAreNull() {
    handler
      .handle(TwitchEventFixtures.context(TwitchEventFixtures.event().build()))
      .join();

    var captured = capturedEvent();
    assertEquals("", captured.tier());
    assertEquals(0, captured.amount().intValue());
    assertEquals(0, captured.total().intValue());
  }

  private TwitchChannelSubscriptionGiftEvent capturedEvent() {
    var captor = ArgumentCaptor.forClass(Object.class);
    verify(facade).sendEvent(captor.capture());
    return assertInstanceOf(
      TwitchChannelSubscriptionGiftEvent.class,
      captor.getValue()
    );
  }
}
