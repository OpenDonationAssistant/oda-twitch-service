package io.github.opendonationassistant.twitch.listener.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.integration.twitch.TwitchClient;
import io.github.opendonationassistant.twitch.repository.TwitchAccountData;
import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import io.micronaut.serde.ObjectMapper;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

public class SendTwitchShoutoutHandlerTest {

  private TwitchClient twitch = mock(TwitchClient.class);
  private TwitchAccountRepository repository = mock(
    TwitchAccountRepository.class
  );
  private SendTwitchShoutoutHandler handler = new SendTwitchShoutoutHandler(
    mock(ObjectMapper.class),
    twitch,
    repository
  );

  @Test
  public void sendsShoutoutFromLinkedAccount() throws IOException {
    when(repository.findByRecipientId("rec")).thenReturn(
      Optional.of(new TwitchAccountData("rec", "tw-1", "login", "rt-1"))
    );
    when(
      twitch.sendShoutout("rec", "rt-1", "tw-1", "target-1", "tw-1")
    ).thenReturn(CompletableFuture.<Void>completedFuture(null));

    handler.handle(
      new SendTwitchShoutoutHandler.TwitchShoutoutCommand("rec", "target-1")
    );

    verify(twitch).sendShoutout("rec", "rt-1", "tw-1", "target-1", "tw-1");
  }

  @Test
  public void doesNothingWhenAccountMissing() throws IOException {
    when(repository.findByRecipientId("rec")).thenReturn(Optional.empty());

    handler.handle(
      new SendTwitchShoutoutHandler.TwitchShoutoutCommand("rec", "target-1")
    );

    verifyNoInteractions(twitch);
  }
}
