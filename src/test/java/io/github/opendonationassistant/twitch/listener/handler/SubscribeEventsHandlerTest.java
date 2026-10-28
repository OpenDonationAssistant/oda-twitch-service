package io.github.opendonationassistant.twitch.listener.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.integration.twitch.TwitchApiClient;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.DataWrapper;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SubscribeRequest;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.Subscription;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.Transport;
import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import io.github.opendonationassistant.twitch.repository.TwitchWebhook;
import io.github.opendonationassistant.twitch.repository.TwitchWebhookRepository;
import io.micronaut.serde.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class SubscribeEventsHandlerTest {

  private static final String CLIENT_ID = "client-id";

  private TwitchApiClient apiClient;
  private TwitchWebhookRepository webhookRepository;
  private SubscribeEventsHandler handler;

  @BeforeEach
  public void setUp() {
    apiClient = mock(TwitchApiClient.class);
    webhookRepository = mock(TwitchWebhookRepository.class);
    handler = new SubscribeEventsHandler(
      mock(ObjectMapper.class),
      apiClient,
      mock(TwitchAccountRepository.class),
      webhookRepository,
      CLIENT_ID
    );
  }

  private static SubscribeEventsHandler.SubcribeTwitchEventsCommand subscribeCommand(
    String event
  ) {
    return new SubscribeEventsHandler.SubcribeTwitchEventsCommand(
      "tok",
      "rec",
      "tw",
      "rt",
      event
    );
  }

  private static Subscription subscription(String id) {
    return new Subscription(
      id,
      "channel.follow",
      "2",
      Map.of("broadcaster_user_id", "tw"),
      new Transport("webhook", "https://example.test/events", "secret")
    );
  }

  private void givenSubscribeSucceeds(String subscriptionId) {
    when(
      apiClient.subscribe(eq(CLIENT_ID), eq("Bearer tok"), any())
    ).thenReturn(
      CompletableFuture.completedFuture(
        new DataWrapper<Subscription[]>(
          new Subscription[] { subscription(subscriptionId) },
          null,
          null,
          null
        )
      )
    );
  }

  private SubscribeRequest capturedRequest() {
    var captor = ArgumentCaptor.forClass(SubscribeRequest.class);
    verify(apiClient).subscribe(
      eq(CLIENT_ID),
      eq("Bearer tok"),
      captor.capture()
    );
    return captor.getValue();
  }

  @Test
  public void savesWebhookWhenNoneExists() throws IOException {
    when(
      webhookRepository.findByRecipientIdAndRefreshTokenId("rec", "rt")
    ).thenReturn(Optional.empty());
    givenSubscribeSucceeds("sub-1");

    handler.handle(subscribeCommand("channel.follow"));

    var captor = ArgumentCaptor.forClass(TwitchWebhook.class);
    verify(webhookRepository).save(captor.capture());
    var saved = captor.getValue();
    assertEquals("rec", saved.recipientId());
    assertEquals("tw", saved.twitchId());
    assertEquals("rt", saved.refreshTokenId());
    assertEquals(List.of("sub-1"), saved.subscriptionIds());
  }

  @Test
  public void appendsSubscriptionIdWhenWebhookExists() throws IOException {
    when(
      webhookRepository.findByRecipientIdAndRefreshTokenId("rec", "rt")
    ).thenReturn(
      Optional.of(
        new TwitchWebhook("wh-1", "rec", "tw", "rt", List.of("old-1"))
      )
    );
    givenSubscribeSucceeds("sub-1");

    handler.handle(subscribeCommand("channel.follow"));

    var captor = ArgumentCaptor.forClass(TwitchWebhook.class);
    verify(webhookRepository).update(captor.capture());
    var updated = captor.getValue();
    assertEquals("wh-1", updated.id());
    assertEquals(List.of("old-1", "sub-1"), updated.subscriptionIds());
  }

  @Test
  public void doesNotPersistWhenSubscribeReturnsError() throws IOException {
    when(
      apiClient.subscribe(eq(CLIENT_ID), eq("Bearer tok"), any())
    ).thenReturn(
      CompletableFuture.completedFuture(
        new DataWrapper<Subscription[]>(null, "error", "bad request", 400)
      )
    );

    handler.handle(subscribeCommand("channel.follow"));

    verify(webhookRepository, never()).save(any());
    verify(webhookRepository, never()).update(any());
  }

  @Test
  public void doesNotPersistWhenSubscribeReturnsNoData() throws IOException {
    when(
      apiClient.subscribe(eq(CLIENT_ID), eq("Bearer tok"), any())
    ).thenReturn(
      CompletableFuture.completedFuture(
        new DataWrapper<Subscription[]>(new Subscription[0], null, null, null)
      )
    );

    handler.handle(subscribeCommand("channel.follow"));

    verify(webhookRepository, never()).save(any());
    verify(webhookRepository, never()).update(any());
  }

  @Test
  public void swallowsDuplicateSubscriptionError() {
    when(apiClient.subscribe(any(), any(), any())).thenReturn(
      CompletableFuture.<DataWrapper<Subscription[]>>failedFuture(
        new RuntimeException("subscription already exists")
      )
    );

    assertDoesNotThrow(() -> handler.handle(subscribeCommand("channel.follow"))
    );
    verify(webhookRepository, never()).save(any());
  }

  @Test
  public void rethrowsUnexpectedSubscribeError() {
    when(apiClient.subscribe(any(), any(), any())).thenReturn(
      CompletableFuture.<DataWrapper<Subscription[]>>failedFuture(
        new RuntimeException("boom")
      )
    );

    assertThrows(CompletionException.class, () ->
      handler.handle(subscribeCommand("channel.follow"))
    );
  }

  @Test
  public void sendsFollowEventWithVersionTwo() throws IOException {
    when(
      webhookRepository.findByRecipientIdAndRefreshTokenId("rec", "rt")
    ).thenReturn(Optional.empty());
    givenSubscribeSucceeds("sub-1");

    handler.handle(subscribeCommand("channel.follow"));

    assertEquals("channel.follow", capturedRequest().type());
    assertEquals("2", capturedRequest().version());
  }

  @Test
  public void sendsKnownEventWithVersionOne() throws IOException {
    when(
      webhookRepository.findByRecipientIdAndRefreshTokenId("rec", "rt")
    ).thenReturn(Optional.empty());
    givenSubscribeSucceeds("sub-1");

    handler.handle(subscribeCommand("channel.subscribe"));

    assertEquals("channel.subscribe", capturedRequest().type());
    assertEquals("1", capturedRequest().version());
  }

  @Test
  public void raidEventUsesToBroadcasterCondition() throws IOException {
    when(
      webhookRepository.findByRecipientIdAndRefreshTokenId("rec", "rt")
    ).thenReturn(Optional.empty());
    givenSubscribeSucceeds("sub-1");

    handler.handle(subscribeCommand("channel.raid"));

    assertEquals("channel.raid", capturedRequest().type());
    var conditions = capturedRequest().condition();
    assertEquals("tw", conditions.get("broadcaster_user_id"));
    assertEquals("tw", conditions.get("to_broadcaster_user_id"));
  }

  @Test
  public void authorizationRevokeAddsUserAndClientConditions()
    throws IOException {
    when(
      webhookRepository.findByRecipientIdAndRefreshTokenId("rec", "rt")
    ).thenReturn(Optional.empty());
    givenSubscribeSucceeds("sub-1");

    handler.handle(subscribeCommand("user.authorization.revoke"));

    assertEquals("user.authorization.revoke", capturedRequest().type());
    var conditions = capturedRequest().condition();
    assertEquals("tw", conditions.get("user_id"));
    assertEquals(CLIENT_ID, conditions.get("client_id"));
  }
}
