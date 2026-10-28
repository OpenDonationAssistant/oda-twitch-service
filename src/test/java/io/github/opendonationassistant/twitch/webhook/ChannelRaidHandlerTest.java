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
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ChannelRaidHandlerTest {

  private TwitchFacade facade;
  private ChannelRaidHandler handler;

  @BeforeEach
  public void setUp() {
    facade = mock(TwitchFacade.class);
    when(facade.sendEvent(any()))
      .thenReturn(CompletableFuture.<Void>completedFuture(null));
    handler = new ChannelRaidHandler(facade);
  }

  @Test
  public void handlesRaidEventsOnly() {
    assertTrue(handler.canHandle("channel.raid"));
    assertFalse(handler.canHandle("channel.cheer"));
  }

  @Test
  public void sendsRaidEventWithRaiderDetails() {
    handler
      .handle(
        TwitchEventFixtures.context(
          TwitchEventFixtures
            .event()
            .fromBroadcasterId("from-1")
            .fromBroadcasterName("FromName")
            .viewers(42)
            .build()
        )
      )
      .join();

    var captured = capturedEvent();
    assertEquals("id-1", captured.id());
    assertEquals("rec", captured.recipientId());
    assertEquals("from-1", captured.fromChannelId());
    assertEquals("FromName", captured.fromChannelName());
    assertEquals(42, captured.viewerCount());
  }

  @Test
  public void defaultsRaiderDetailsWhenFieldsAreNull() {
    handler
      .handle(TwitchEventFixtures.context(TwitchEventFixtures.event().build()))
      .join();

    var captured = capturedEvent();
    assertEquals("", captured.fromChannelId());
    assertEquals("", captured.fromChannelName());
    assertEquals(0, captured.viewerCount());
  }

  private ChannelRaidHandler.TwitchChannelRaidEvent capturedEvent() {
    var captor = ArgumentCaptor.forClass(Object.class);
    verify(facade).sendEvent(captor.capture());
    return assertInstanceOf(
      ChannelRaidHandler.TwitchChannelRaidEvent.class,
      captor.getValue()
    );
  }
}
