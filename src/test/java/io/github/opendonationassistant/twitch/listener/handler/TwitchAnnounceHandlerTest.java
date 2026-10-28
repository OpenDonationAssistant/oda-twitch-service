package io.github.opendonationassistant.twitch.listener.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.integration.twitch.TwitchClient;
import io.micronaut.serde.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TwitchAnnounceHandlerTest {

  private TwitchClient twitch;
  private TwitchAnnounceHandler handler;

  @BeforeEach
  public void setUp() {
    twitch = mock(TwitchClient.class);
    when(
      twitch.sendAnnouncement(
        eq("rec"),
        eq("rt-1"),
        eq("tw-1"),
        eq("mod-1"),
        eq("hi"),
        any()
      )
    ).thenReturn(CompletableFuture.<Void>completedFuture(null));
    handler = new TwitchAnnounceHandler(mock(ObjectMapper.class), twitch);
  }

  @Test
  public void sendsAnnouncementWithColor() throws IOException {
    handler.handle(
      new TwitchAnnounceHandler.TwitchAnnounceCommand(
        "rec",
        "rt-1",
        "tw-1",
        "mod-1",
        "hi",
        "green"
      )
    );

    verify(twitch)
      .sendAnnouncement("rec", "rt-1", "tw-1", "mod-1", "hi", "green");
  }

  @Test
  public void sendsAnnouncementWithoutColor() throws IOException {
    handler.handle(
      new TwitchAnnounceHandler.TwitchAnnounceCommand(
        "rec",
        "rt-1",
        "tw-1",
        "mod-1",
        "hi",
        null
      )
    );

    verify(twitch).sendAnnouncement("rec", "rt-1", "tw-1", "mod-1", "hi", null);
  }
}
