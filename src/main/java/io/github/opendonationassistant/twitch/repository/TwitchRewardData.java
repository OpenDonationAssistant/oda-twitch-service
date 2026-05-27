package io.github.opendonationassistant.twitch.repository;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.model.DataType;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@MappedEntity("reward")
public record TwitchRewardData(
  @Id @MappedProperty(value = "id") String id,
  @MappedProperty(value = "recipient_id", type = DataType.UUID)
  String recipientId,
  @MappedProperty(value = "refresh_token_id", type = DataType.UUID)
  String refreshTokenId,
  @MappedProperty("type") String type
) {}
