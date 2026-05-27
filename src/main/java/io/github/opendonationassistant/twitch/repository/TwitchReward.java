package io.github.opendonationassistant.twitch.repository;

import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.twitch.webhook.TwitchEventsWebhook.AddMediaCommand;

public class TwitchReward {

  private final TwitchRewardData data;
  private final RabbitClient rabbit;

  public TwitchReward(RabbitClient rabbit, TwitchRewardData data) {
    this.rabbit = rabbit;
    this.data = data;
  }

  public TwitchRewardData data() {
    return data;
  }

  public void sendAddMediaCommand(String url, String requester) {
    rabbit.sendCommand(
      new AddMediaCommand(url, requester, data.recipientId(), data.type())
    );
  }
}
