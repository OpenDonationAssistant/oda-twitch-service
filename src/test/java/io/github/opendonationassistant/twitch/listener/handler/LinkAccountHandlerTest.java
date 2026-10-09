package io.github.opendonationassistant.twitch.listener.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import io.micronaut.serde.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LinkAccountHandlerTest {

  private TwitchAccountRepository repository;
  private LinkAccountHandler handler;

  @BeforeEach
  public void setUp() {
    repository = mock(TwitchAccountRepository.class);
    handler = new LinkAccountHandler(mock(ObjectMapper.class), repository);
  }

  @Test
  public void createsAccountFromCommand() throws IOException {
    handler.handle(
      new LinkAccountHandler.LinkTwitchAccount("rec", "tw-1", "login", "rt-1")
    );

    verify(repository).create("rec", "tw-1", "login", "rt-1");
  }
}
