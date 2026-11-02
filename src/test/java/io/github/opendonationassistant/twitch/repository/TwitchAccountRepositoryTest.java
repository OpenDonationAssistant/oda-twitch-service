package io.github.opendonationassistant.twitch.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.integration.media.MediaRPC;
import io.github.opendonationassistant.integration.twitch.TwitchClient;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TwitchAccountRepositoryTest {

  private static final TwitchAccountData DATA = new TwitchAccountData(
    "rec",
    "tw",
    "login",
    "rt"
  );

  private TwitchAccountDataRepository repository;
  private TwitchAccountRepository accounts;

  @BeforeEach
  public void setUp() {
    repository = mock(TwitchAccountDataRepository.class);
    accounts = new TwitchAccountRepository(
      repository,
      mock(TwitchRewardDataRepository.class),
      mock(MediaRPC.class),
      mock(TwitchClient.class)
    );
  }

  @Test
  public void findByTwitchIdReturnsLinkedAccount() {
    when(repository.findByTwitchId("tw")).thenReturn(Optional.of(DATA));

    var account = accounts.findByTwitchId("tw");

    assertTrue(account.isPresent());
    assertEquals(DATA, account.get().data());
  }

  @Test
  public void findByRecipientIdReturnsEmptyWhenAccountIsMissing() {
    when(repository.findByRecipientId("rec")).thenReturn(Optional.empty());

    assertTrue(accounts.findByRecipientId("rec").isEmpty());
  }

  @Test
  public void createReturnsLinkedAccount() {
    when(repository.save(any())).thenReturn(DATA);

    var account = accounts.create("rec", "tw", "login", "rt");

    verify(repository).save(DATA);
    assertEquals(DATA, account.data());
  }
}
