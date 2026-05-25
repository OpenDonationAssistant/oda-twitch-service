package io.github.opendonationassistant.twitch.repository;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface TwitchRewardDataRepository
  extends CrudRepository<TwitchRewardData, String> {
  List<TwitchRewardData> findByRecipientId(String recipientId);
  Optional<TwitchRewardData> findByRecipientIdAndType(
    String recipientId,
    String type
  );
  void deleteByRecipientId(String recipientId);
}
