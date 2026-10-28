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
import io.github.opendonationassistant.events.twitch.events.TwitchStreamEndedEvent;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class StreamOfflineHandlerTest {

  private TwitchFacade facade;
  private StreamOfflineHandler handler;

  @BeforeEach
  public void setUp() {
    facade = mock(TwitchFacade.class);
    when(facade.sendEvent(any()))
      .thenReturn(CompletableFuture.<Void>completedFuture(null));
    handler = new StreamOfflineHandler(facade);
  }

  @Test
  public void handlesStreamOfflineEventsOnly() {
    assertTrue(handler.canHandle("stream.offline"));
    assertFalse(handler.canHandle("stream.online"));
  }

  @Test
  public void sendsStreamEndedEvent() {
    handler
      .handle(TwitchEventFixtures.context(TwitchEventFixtures.event().build()))
      .join();

    var captor = ArgumentCaptor.forClass(Object.class);
    verify(facade).sendEvent(captor.capture());
    var captured = assertInstanceOf(
      TwitchStreamEndedEvent.class,
      captor.getValue()
    );
    assertEquals("id-1", captured.id());
    assertEquals("rec", captured.recipientId());
  }
}
