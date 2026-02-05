package io.github.opendonationassistant.twitch.repository;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface TwitchAccountDataRepository
  extends CrudRepository<TwitchAccountData, String> {
  Optional<TwitchAccountData> findByTwitchId(String twitchId);
}
