package io.github.opendonationassistant.twitch.repository;

import io.github.opendonationassistant.integration.media.MediaRPC;
import io.github.opendonationassistant.integration.twitch.TwitchClient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
public class TwitchAccountRepository {

  private final TwitchAccountDataRepository repository;
  private final TwitchRewardDataRepository rewards;
  private final MediaRPC mediaRPC;
  private final TwitchClient twitch;

  @Inject
  public TwitchAccountRepository(
    TwitchAccountDataRepository repository,
    TwitchRewardDataRepository rewards,
    MediaRPC mediaRPC,
    TwitchClient twitch
  ) {
    this.repository = repository;
    this.rewards = rewards;
    this.mediaRPC = mediaRPC;
    this.twitch = twitch;
  }

  public TwitchAccount create(
    String recipientId,
    String twitchId,
    String twitchLogin,
    String refreshTokenId
  ) {
    // TODO maybe check and throw error/update
    return convert(
      repository.save(
        new TwitchAccountData(recipientId, twitchId, twitchLogin, refreshTokenId)
      )
    );
  }

  public CompletableFuture<Optional<TwitchAccount>> findByTwitchId(
    String twitchId
  ) {
    return repository
      .findByTwitchId(twitchId)
      .thenApply(found -> found.map(this::convert));
  }

  public Optional<TwitchAccount> findByRecipientId(String recipientId) {
    return repository.findByRecipientId(recipientId).map(this::convert);
  }

  public Optional<TwitchAccount> findByRefreshTokenId(
    String refreshTokenId
  ) {
    return repository.findByRefreshTokenId(refreshTokenId).map(this::convert);
  }

  private TwitchAccount convert(TwitchAccountData data) {
    return new TwitchAccount(data, rewards, mediaRPC, twitch);
  }
}
