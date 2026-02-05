package io.github.opendonationassistant.twitch.repository;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@MappedEntity("accounts")
public record TwitchAccountData(
  @MappedProperty("recipient_id") String recipientId,
  @Id @MappedProperty("twitch_id") String twitchId,
  @MappedProperty("twitch_login") String twitchLogin
) {}
