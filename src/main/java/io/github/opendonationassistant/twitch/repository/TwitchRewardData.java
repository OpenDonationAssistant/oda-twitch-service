package io.github.opendonationassistant.twitch.repository;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.model.DataType;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@MappedEntity("twitch_rewards")
public record TwitchRewardData(
  @Id @MappedProperty(value = "id", type = DataType.UUID) String id,
  @MappedProperty("recipient_id") String recipientId,
  @MappedProperty(value = "refresh_token_id", type = DataType.UUID)
  String refreshTokenId,
  @MappedProperty("type") String type,
  @MappedProperty("cost") Integer cost
) {}
