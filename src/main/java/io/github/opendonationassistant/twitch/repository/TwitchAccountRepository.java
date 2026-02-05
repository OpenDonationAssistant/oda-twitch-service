package io.github.opendonationassistant.twitch.repository;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class TwitchAccountRepository {

  private final TwitchAccountDataRepository repository;

  @Inject
  public TwitchAccountRepository(TwitchAccountDataRepository repository) {
    this.repository = repository;
  }

  public TwitchAccountData create(
    String recipientId,
    String twitchId,
    String twitchLogin
  ) {
    // TODO check and throw error maybe
    return repository.save(
      new TwitchAccountData(recipientId, twitchId, twitchLogin)
    );
  }

  public Optional<TwitchAccountData> findByTwitchId(String twitchId) {
    return repository.findByTwitchId(twitchId);
  }
}
