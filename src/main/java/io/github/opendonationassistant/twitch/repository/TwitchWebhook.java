package io.github.opendonationassistant.twitch.repository;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

@Serdeable
@MappedEntity("twitch_webhooks")
public record TwitchWebhook(
  @Id @MappedProperty("recipient_id") String recipientId,
  @MappedProperty("twitch_id") String twitchId,
  @MappedProperty("subscription_ids") List<String> subscriptionIds
) {}
