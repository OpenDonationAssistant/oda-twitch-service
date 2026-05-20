package io.github.opendonationassistant.twitch.repository;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;

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
    String twitchLogin,
    String refreshTokenId
  ) {
    // TODO check and throw error maybe
    return repository.save(
      new TwitchAccountData(recipientId, twitchId, twitchLogin, refreshTokenId)
    );
  }

  public Optional<TwitchAccountData> findByTwitchId(String twitchId) {
    return repository.findByTwitchId(twitchId);
  }

  public Optional<TwitchAccountData> findByRecipientId(String recipientId) {
    return repository.findByRecipientId(recipientId);
  }
}
