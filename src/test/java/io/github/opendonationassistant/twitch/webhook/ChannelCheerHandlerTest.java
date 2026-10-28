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
import io.github.opendonationassistant.events.twitch.events.TwitchChannelCheerEvent;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ChannelCheerHandlerTest {

  private TwitchFacade facade;
  private ChannelCheerHandler handler;

  @BeforeEach
  public void setUp() {
    facade = mock(TwitchFacade.class);
    when(facade.sendEvent(any()))
      .thenReturn(CompletableFuture.<Void>completedFuture(null));
    handler = new ChannelCheerHandler(facade);
  }

  @Test
  public void handlesCheerEventsOnly() {
    assertTrue(handler.canHandle("channel.cheer"));
    assertFalse(handler.canHandle("channel.follow"));
  }

  @Test
  public void sendsCheerEventWithMessageAndBits() {
    handler
      .handle(
        TwitchEventFixtures.context(
          TwitchEventFixtures
            .event()
            .userName("bob")
            .message("hello")
            .bits(500)
            .build()
        )
      )
      .join();

    var captured = capturedEvent();
    assertEquals("id-1", captured.id());
    assertEquals("rec", captured.recipientId());
    assertEquals("bob", captured.username());
    assertEquals("hello", captured.message());
    assertEquals("500", captured.bits());
  }

  @Test
  public void defaultsMessageAndBitsWhenFieldsAreNull() {
    handler
      .handle(TwitchEventFixtures.context(TwitchEventFixtures.event().build()))
      .join();

    var captured = capturedEvent();
    assertEquals("", captured.message());
    assertEquals("0", captured.bits());
  }

  private TwitchChannelCheerEvent capturedEvent() {
    var captor = ArgumentCaptor.forClass(Object.class);
    verify(facade).sendEvent(captor.capture());
    return assertInstanceOf(TwitchChannelCheerEvent.class, captor.getValue());
  }
}
