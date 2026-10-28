package io.github.opendonationassistant.twitch.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.events.twitch.TwitchFacade;
import io.github.opendonationassistant.events.twitch.events.TwitchStreamStartedEvent;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.DataWrapper;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.Stream;
import io.github.opendonationassistant.integration.twitch.TwitchClient;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class StreamOnlineHandlerTest {

  private TwitchFacade facade;
  private TwitchClient client;
  private StreamOnlineHandler handler;

  @BeforeEach
  public void setUp() {
    facade = mock(TwitchFacade.class);
    client = mock(TwitchClient.class);
    when(facade.sendEvent(any()))
      .thenReturn(CompletableFuture.<Void>completedFuture(null));
    handler = new StreamOnlineHandler(facade, client);
  }

  @Test
  public void handlesStreamOnlineEventsOnly() {
    assertTrue(handler.canHandle("stream.online"));
    assertFalse(handler.canHandle("stream.offline"));
  }

  @Test
  public void sendsStreamStartedEventWithThumbnail() {
    givenStreams(
      new DataWrapper<List<Stream>>(
        List.of(new Stream("thumb.jpg")),
        null,
        null,
        null
      )
    );

    handler
      .handle(TwitchEventFixtures.context(TwitchEventFixtures.event().build()))
      .join();

    var captor = ArgumentCaptor.forClass(Object.class);
    verify(facade).sendEvent(captor.capture());
    var captured = assertInstanceOf(
      TwitchStreamStartedEvent.class,
      captor.getValue()
    );
    assertEquals("id-1", captured.id());
    assertEquals("rec", captured.recipientId());
    assertEquals("thumb.jpg", captured.thumbnailUrl());
  }

  @Test
  public void doesNotSendEventWhenApiReturnsError() {
    givenStreams(
      new DataWrapper<List<Stream>>(null, "error", "bad request", 400)
    );

    handler
      .handle(TwitchEventFixtures.context(TwitchEventFixtures.event().build()))
      .join();

    verify(facade, never()).sendEvent(any());
  }

  @Test
  public void doesNotSendEventWhenApiReturnsNoStreams() {
    givenStreams(new DataWrapper<List<Stream>>(List.of(), null, null, null));

    handler
      .handle(TwitchEventFixtures.context(TwitchEventFixtures.event().build()))
      .join();

    verify(facade, never()).sendEvent(any());
  }

  private void givenStreams(DataWrapper<List<Stream>> response) {
    when(client.getStreams("rec", "rt", "tw", "live"))
      .thenReturn(CompletableFuture.completedFuture(response));
  }
}
