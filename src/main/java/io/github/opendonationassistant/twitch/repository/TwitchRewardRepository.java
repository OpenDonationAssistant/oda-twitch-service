package io.github.opendonationassistant.twitch.repository;

import io.github.opendonationassistant.rabbit.RabbitClient;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Optional;

@Singleton
public class TwitchRewardRepository {

  private final TwitchRewardDataRepository repository;
  private final RabbitClient rabbit;

  @Inject
  public TwitchRewardRepository(
    TwitchRewardDataRepository repository,
    @Named("commands") RabbitClient rabbit
  ) {
    this.repository = repository;
    this.rabbit = rabbit;
  }

  public TwitchReward create(TwitchRewardData data) {
    var saved = repository.save(data);
    return new TwitchReward(rabbit, saved);
  }

  public Optional<TwitchReward> findByWidgetId(String widgetid) {
    return repository
      .findOneByWidgetId(widgetid)
      .map(data -> new TwitchReward(rabbit, data));
  }

  public Optional<TwitchReward> findByRecipientIdAndType(
    String recipientId,
    String type
  ) {
    return repository
      .findByRecipientIdAndType(recipientId, type)
      .map(data -> new TwitchReward(rabbit, data));
  }
}
