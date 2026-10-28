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
import io.github.opendonationassistant.events.twitch.events.TwitchUserBannedEvent;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ChannelBanHandlerTest {

  private TwitchFacade facade;
  private ChannelBanHandler handler;

  @BeforeEach
  public void setUp() {
    facade = mock(TwitchFacade.class);
    when(facade.sendEvent(any()))
      .thenReturn(CompletableFuture.<Void>completedFuture(null));
    handler = new ChannelBanHandler(facade);
  }

  @Test
  public void handlesBanEventsOnly() {
    assertTrue(handler.canHandle("channel.ban"));
    assertFalse(handler.canHandle("channel.follow"));
  }

  @Test
  public void sendsBanEventMarkingPermanentBans() {
    handler
      .handle(
        TwitchEventFixtures.context(
          TwitchEventFixtures.event().userName("bob").isPermanent(true).build()
        )
      )
      .join();

    var captured = capturedEvent();
    assertEquals("id-1", captured.id());
    assertEquals("rec", captured.recipientId());
    assertEquals("bob", captured.nickname());
    assertTrue(captured.permanent());
  }

  @Test
  public void sendsBanEventMarkingTemporaryBans() {
    handler
      .handle(
        TwitchEventFixtures.context(
          TwitchEventFixtures.event().userName("bob").isPermanent(false).build()
        )
      )
      .join();

    assertFalse(capturedEvent().permanent());
  }

  @Test
  public void treatsNullPermanentFlagAsTemporary() {
    handler
      .handle(TwitchEventFixtures.context(TwitchEventFixtures.event().build()))
      .join();

    assertFalse(capturedEvent().permanent());
  }

  private TwitchUserBannedEvent capturedEvent() {
    var captor = ArgumentCaptor.forClass(Object.class);
    verify(facade).sendEvent(captor.capture());
    return assertInstanceOf(TwitchUserBannedEvent.class, captor.getValue());
  }
}
