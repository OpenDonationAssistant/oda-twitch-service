package io.github.opendonationassistant.twitch.listener.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.integration.twitch.TwitchIdClient;
import io.github.opendonationassistant.integration.twitch.TwitchIdClient.GetAccessRecordResponse;
import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.twitch.repository.TwitchAccountData;
import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import io.micronaut.serde.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class SubscribeAllEventsHandlerTest {

  private static final String CLIENT_ID = "client-id";
  private static final String CLIENT_SECRET = "client-secret";
  private static final Set<String> SUPPORTED_EVENTS = Set.of(
    "channel.follow",
    "channel.ban",
    "channel.subscribe",
    "channel.subscription.gift",
    "channel.subscription.message",
    "channel.channel_points_custom_reward_redemption.add",
    "channel.cheer",
    "channel.raid",
    "channel.poll.begin",
    "channel.poll.end",
    "channel.prediction.begin",
    "channel.prediction.end",
    "channel.hype_train.begin",
    "channel.hype_train.end",
    "channel.shoutout.create",
    "channel.shoutout.receive",
    "stream.online",
    "stream.offline",
    "channel.goal.begin",
    "channel.goal.progress",
    "channel.goal.end"
  );

  private static final TwitchAccountData ACCOUNT = new TwitchAccountData(
    "rec",
    "tw-1",
    "login",
    "rt-1"
  );

  private TwitchIdClient twitchIdClient = mock(TwitchIdClient.class);
  private RabbitClient rabbit = mock(RabbitClient.class);
  private TwitchAccountRepository repository = mock(TwitchAccountRepository.class);
  private SubscribeAllEventsHandler handler = new SubscribeAllEventsHandler(
      mock(ObjectMapper.class),
      twitchIdClient,
      rabbit,
      repository,
      CLIENT_ID,
      CLIENT_SECRET
    );

  private void givenAppToken(String value) {
    when(twitchIdClient.getToken(any()))
      .thenReturn(
        CompletableFuture.completedFuture(new GetAccessRecordResponse(value, null))
      );
  }

  private static SubscribeAllEventsHandler.SubscribeAllTwitchEventsCommand command() {
    return new SubscribeAllEventsHandler.SubscribeAllTwitchEventsCommand(
      "rec",
      "rt-1"
    );
  }

  @Test
  public void sendsOneCommandForEverySupportedEvent() throws IOException {
    givenAppToken("app-token");
    when(repository.findByRecipientId("rec")).thenReturn(Optional.of(ACCOUNT));

    handler.handle(command());

    var captor = ArgumentCaptor.forClass(
      SubscribeEventsHandler.SubcribeTwitchEventsCommand.class
    );
    verify(rabbit, times(SUPPORTED_EVENTS.size())).sendCommand(captor.capture());
    var commands = captor.getAllValues();
    assertEquals(SUPPORTED_EVENTS.size(), commands.size());
    assertTrue(
      commands
        .stream()
        .allMatch(c ->
          "app-token".equals(c.token()) &&
          "rec".equals(c.recipientId()) &&
          "tw-1".equals(c.twitchId()) &&
          "rt-1".equals(c.refreshTokenId())
        )
    );

    var events = commands
      .stream()
      .map(SubscribeEventsHandler.SubcribeTwitchEventsCommand::event)
      .collect(Collectors.toSet());
    assertEquals(SUPPORTED_EVENTS, events);
  }

  @Test
  public void requestsAppTokenWithClientCredentials() throws IOException {
    givenAppToken("app-token");
    when(repository.findByRecipientId("rec")).thenReturn(Optional.of(ACCOUNT));

    handler.handle(command());

    verify(twitchIdClient)
      .getToken(
        Map.of(
          "client_id",
          CLIENT_ID,
          "client_secret",
          CLIENT_SECRET,
          "grant_type",
          "client_credentials"
        )
      );
  }

  @Test
  public void sendsNoCommandsWhenAccountMissing() throws IOException {
    givenAppToken("app-token");
    when(repository.findByRecipientId("rec")).thenReturn(Optional.empty());

    handler.handle(command());

    verifyNoInteractions(rabbit);
  }
}
