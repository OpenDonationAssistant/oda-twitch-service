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
import io.github.opendonationassistant.events.twitch.events.TwitchChannelSubscriptionMessageEvent;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ChannelSubscriptionMessageHandlerTest {

  private TwitchFacade facade;
  private ChannelSubscriptionMessageHandler handler;

  @BeforeEach
  public void setUp() {
    facade = mock(TwitchFacade.class);
    when(facade.sendEvent(any()))
      .thenReturn(CompletableFuture.<Void>completedFuture(null));
    handler = new ChannelSubscriptionMessageHandler(facade);
  }

  @Test
  public void handlesSubscriptionMessageEventsOnly() {
    assertTrue(handler.canHandle("channel.subscription.message"));
    assertFalse(handler.canHandle("channel.subscription.gift"));
  }

  @Test
  public void sendsSubscriptionMessageWithExtractedText() {
    handler
      .handle(
        TwitchEventFixtures.context(
          TwitchEventFixtures
            .event()
            .userName("bob")
            .tier("2000")
            .message(Map.of("text", "great stream"))
            .total(3)
            .cumulativeTotal(12)
            .streakMonths(4)
            .build()
        )
      )
      .join();

    var captured = capturedEvent();
    assertEquals("id-1", captured.id());
    assertEquals("rec", captured.recipientId());
    assertEquals("bob", captured.username());
    assertEquals("2000", captured.tier());
    assertEquals("great stream", captured.message().text());
    assertTrue(captured.message().emotes().isEmpty());
    // The outbound record maps event.total -> cumulativeMonths and event.cumulative_total -> totalMonths.
    assertEquals(3, captured.cumulativeMonths());
    assertEquals(12, captured.totalMonths());
    assertEquals(4, captured.streakMonths());
  }

  @Test
  public void defaultsTextWhenMessageHasNoTextNode() {
    handler
      .handle(
        TwitchEventFixtures.context(
          TwitchEventFixtures
            .event()
            .userName("bob")
            .message(Map.of("emotes", "x"))
            .build()
        )
      )
      .join();

    assertEquals("", capturedEvent().message().text());
  }

  @Test
  public void defaultsFieldsWhenEventFieldsAreNull() {
    handler
      .handle(TwitchEventFixtures.context(TwitchEventFixtures.event().build()))
      .join();

    var captured = capturedEvent();
    assertEquals("", captured.tier());
    assertEquals("", captured.message().text());
    assertEquals(0, captured.cumulativeMonths());
    assertEquals(0, captured.totalMonths());
    assertEquals(0, captured.streakMonths());
  }

  private TwitchChannelSubscriptionMessageEvent capturedEvent() {
    var captor = ArgumentCaptor.forClass(Object.class);
    verify(facade).sendEvent(captor.capture());
    return assertInstanceOf(
      TwitchChannelSubscriptionMessageEvent.class,
      captor.getValue()
    );
  }
}
