package io.github.opendonationassistant.twitch.repository;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.model.DataType;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

@Serdeable
@MappedEntity("twitch_webhooks")
public record TwitchWebhook(
  @Id @MappedProperty(value = "id", type = DataType.UUID) String id,
  @MappedProperty("recipient_id") String recipientId,
  @MappedProperty("twitch_id") String twitchId,
  @MappedProperty(value = "refresh_token_id", type = DataType.UUID)
  String refreshTokenId,
  @MappedProperty(value = "subscription_ids", type = DataType.STRING_ARRAY)
  List<String> subscriptionIds
) {}
