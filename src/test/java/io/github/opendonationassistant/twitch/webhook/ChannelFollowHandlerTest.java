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
import io.github.opendonationassistant.events.twitch.events.TwitchChannelFollowEvent;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ChannelFollowHandlerTest {

  private TwitchFacade facade;
  private ChannelFollowHandler handler;

  @BeforeEach
  public void setUp() {
    facade = mock(TwitchFacade.class);
    when(facade.sendEvent(any()))
      .thenReturn(CompletableFuture.<Void>completedFuture(null));
    handler = new ChannelFollowHandler(facade);
  }

  @Test
  public void handlesFollowEventsOnly() {
    assertTrue(handler.canHandle("channel.follow"));
    assertFalse(handler.canHandle("channel.cheer"));
  }

  @Test
  public void sendsFollowEventWithParsedTimestamp() {
    handler
      .handle(
        TwitchEventFixtures.context(
          TwitchEventFixtures
            .event()
            .userName("bob")
            .timestamp("2026-01-02T03:04:05Z")
            .build()
        )
      )
      .join();

    var captured = capturedEvent();
    assertEquals("id-1", captured.id());
    assertEquals("rec", captured.recipientId());
    assertEquals("bob", captured.username());
    assertEquals(Instant.parse("2026-01-02T03:04:05Z"), captured.timestamp());
  }

  @Test
  public void fallsBackToCurrentTimeWhenTimestampIsNull() {
    var before = Instant.now();
    handler
      .handle(TwitchEventFixtures.context(TwitchEventFixtures.event().build()))
      .join();
    var after = Instant.now();

    var timestamp = capturedEvent().timestamp();
    assertFalse(timestamp.isBefore(before));
    assertFalse(timestamp.isAfter(after));
  }

  private TwitchChannelFollowEvent capturedEvent() {
    var captor = ArgumentCaptor.forClass(Object.class);
    verify(facade).sendEvent(captor.capture());
    return assertInstanceOf(TwitchChannelFollowEvent.class, captor.getValue());
  }
}
