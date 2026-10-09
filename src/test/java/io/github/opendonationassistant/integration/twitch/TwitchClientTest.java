package io.github.opendonationassistant.integration.twitch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.integration.twitch.TwitchApiClient.CreateCustomRewardRequest;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.CustomReward;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.DataWrapper;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.GetUserResponse;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SendAnnouncementRequest;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SendChatMessageRequest;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SendChatMessageResponse;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.Stream;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SubscribeRequest;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.Subscription;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.Transport;
import io.github.opendonationassistant.integration.twitch.TwitchIdClient.GetAccessRecordResponse;
import io.github.opendonationassistant.rabbit.TokenRPC;
import io.github.opendonationassistant.rabbit.TokenRPC.TokenRequest;
import io.github.opendonationassistant.rabbit.TokenRPC.TokenResponse;
import io.github.opendonationassistant.twitch.repository.TwitchAccountData;
import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

public class TwitchClientTest {

  private static final String CLIENT_ID = "client-id";
  private static final String CLIENT_SECRET = "client-secret";

  private TwitchApiClient api;
  private TwitchIdClient id;
  private TokenRPC tokenRPC;
  private TwitchAccountRepository accounts;
  private TwitchClient client;

  @BeforeEach
  public void setUp() {
    api = mock(TwitchApiClient.class);
    id = mock(TwitchIdClient.class);
    tokenRPC = mock(TokenRPC.class);
    accounts = mock(TwitchAccountRepository.class);
    client = new TwitchClient(api, id, tokenRPC, accounts, CLIENT_ID, CLIENT_SECRET);
  }

  private void givenAppToken(String value) {
    when(id.getToken(any()))
      .thenReturn(
        CompletableFuture.completedFuture(new GetAccessRecordResponse(value, null))
      );
  }

  private void givenUserToken(String value) {
    when(tokenRPC.token(any())).thenReturn(new TokenResponse(value, "ok"));
  }

  private static SubscribeRequest subscribeRequest() {
    return new SubscribeRequest(
      "channel.follow",
      "2",
      Map.of("broadcaster_user_id", "1"),
      new Transport("webhook", "https://example.test/events", "secret")
    );
  }

  private static CreateCustomRewardRequest createRewardRequest() {
    return new CreateCustomRewardRequest(
      "Song",
      100,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null
    );
  }

  @Test
  public void getAppTokenRequestsClientCredentialsGrant() {
    givenAppToken("app-token");

    var token = client.getAppToken().join();

    assertEquals("app-token", token.accessToken());
    verify(id)
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
  public void subscribeSendsAppTokenInBearerHeader() {
    givenAppToken("app-token");
    var request = subscribeRequest();
    when(api.subscribe(eq(CLIENT_ID), eq("Bearer app-token"), eq(request)))
      .thenReturn(
        CompletableFuture.completedFuture(
          new DataWrapper<Subscription[]>(new Subscription[0], null, null, null)
        )
      );

    client.subscribe(request).join();

    verify(api).subscribe(CLIENT_ID, "Bearer app-token", request);
  }

  @Test
  public void getSubscriptionsSendsAppTokenInBearerHeader() {
    givenAppToken("app-token");
    when(api.getSubscriptions(eq(CLIENT_ID), eq("Bearer app-token")))
      .thenReturn(
        CompletableFuture.completedFuture(
          new DataWrapper<Subscription[]>(new Subscription[0], null, null, null)
        )
      );

    client.getSubscriptions().join();

    verify(api).getSubscriptions(CLIENT_ID, "Bearer app-token");
  }

  @Test
  public void deleteSubscriptionSendsAppTokenInBearerHeader() {
    givenAppToken("app-token");
    when(
      api.deleteSubscription(
        eq(CLIENT_ID),
        eq("Bearer app-token"),
        isNull(),
        eq("sub-1")
      )
    ).thenReturn(CompletableFuture.<Void>completedFuture(null));

    client.deleteSubscription(null, "sub-1").join();

    verify(api).deleteSubscription(CLIENT_ID, "Bearer app-token", null, "sub-1");
  }

  @Test
  public void sendChatMessageUsesUserTokenFromRpc() {
    givenUserToken("user-token");
    var request = new SendChatMessageRequest("broadcaster", "sender", "hi");
    when(api.sendChatMessage(eq(CLIENT_ID), eq("Bearer user-token"), eq(request)))
      .thenReturn(
        CompletableFuture.completedFuture(
          new DataWrapper<List<SendChatMessageResponse>>(
            List.of(new SendChatMessageResponse("m-1", true)),
            null,
            null,
            null
          )
        )
      );

    client.sendChatMessage("rec-1", "rt-1", request).join();

    verify(tokenRPC).token(new TokenRequest("rec-1", "rt-1"));
    verify(api).sendChatMessage(CLIENT_ID, "Bearer user-token", request);
  }

  @Test
  public void sendChatMessageFailsWithProblemWhenRpcTokenValueMissing() {
    var request = new SendChatMessageRequest("broadcaster", "sender", "hi");
    when(tokenRPC.token(any()))
      .thenReturn(new TokenResponse(null, "no access token"));

    var thrown = assertThrows(
      CompletionException.class,
      () -> client.sendChatMessage("rec-1", "rt-1", request).join()
    );

    var problem = assertInstanceOf(Problem.class, thrown.getCause());
    assertEquals("Unauthorized", problem.getTitle());
    assertEquals("no access token", problem.getDetail());
  }

  @Test
  public void sendChatMessageCompletesExceptionallyWhenRpcReturnsNull() {
    // tokenRPC is intentionally left unstubbed: Mockito returns null by default.
    var request = new SendChatMessageRequest("broadcaster", "sender", "hi");

    var future = client.sendChatMessage("rec-1", "rt-1", request);

    var thrown = assertThrows(CompletionException.class, future::join);
    assertNotNull(thrown.getCause());
  }

  @Test
  public void sendAnnouncementPassesColorToApi() {
    givenUserToken("user-token");
    var expected = new SendAnnouncementRequest("hi", "green");
    when(
      api.sendAnnouncement(
        eq(CLIENT_ID),
        eq("Bearer user-token"),
        eq("b-1"),
        eq("m-1"),
        eq(expected)
      )
    ).thenReturn(CompletableFuture.<Void>completedFuture(null));

    client.sendAnnouncement("rec-1", "rt-1", "b-1", "m-1", "hi", "green").join();

    verify(api)
      .sendAnnouncement(CLIENT_ID, "Bearer user-token", "b-1", "m-1", expected);
  }

  @Test
  public void pinChatMessagePassesDurationToApi() {
    givenUserToken("user-token");
    when(
      api.pinChatMessage(
        eq(CLIENT_ID),
        eq("Bearer user-token"),
        eq("b-1"),
        eq("m-1"),
        eq("msg-1"),
        eq(30)
      )
    ).thenReturn(
      CompletableFuture.completedFuture(new DataWrapper<Void>(null, null, null, null))
    );

    client.pinChatMessage("rec-1", "rt-1", "b-1", "m-1", "msg-1", 30).join();

    verify(api)
      .pinChatMessage(CLIENT_ID, "Bearer user-token", "b-1", "m-1", "msg-1", 30);
  }

  @Test
  public void sendShoutoutPassesIdentifiersToApi() {
    givenUserToken("user-token");
    when(
      api.sendShoutout(
        eq(CLIENT_ID),
        eq("Bearer user-token"),
        eq("from-1"),
        eq("to-1"),
        eq("mod-1")
      )
    ).thenReturn(CompletableFuture.<Void>completedFuture(null));

    client.sendShoutout("rec-1", "rt-1", "from-1", "to-1", "mod-1").join();

    verify(api)
      .sendShoutout(CLIENT_ID, "Bearer user-token", "from-1", "to-1", "mod-1");
  }

  @Test
  public void getStreamsUsesUserTokenFromRpc() {
    givenUserToken("user-token");
    when(
      api.getStreams(eq(CLIENT_ID), eq("Bearer user-token"), eq("tw-1"), eq("live"))
    ).thenReturn(
      CompletableFuture.completedFuture(
        new DataWrapper<List<Stream>>(
          List.of(new Stream("thumb.jpg")),
          null,
          null,
          null
        )
      )
    );

    client.getStreams("rec-1", "rt-1", "tw-1", "live").join();

    verify(api).getStreams(CLIENT_ID, "Bearer user-token", "tw-1", "live");
  }

  @Test
  public void getUserUsesUserTokenFromRpc() {
    givenUserToken("user-token");
    when(api.getUser(eq(CLIENT_ID), eq("Bearer user-token"), eq("login")))
      .thenReturn(
        CompletableFuture.completedFuture(
          new DataWrapper<GetUserResponse>(
            new GetUserResponse("user-1", "login", "Login"),
            null,
            null,
            null
          )
        )
      );

    client.getUser("rec-1", "rt-1", "login").join();

    verify(api).getUser(CLIENT_ID, "Bearer user-token", "login");
  }

  @Test
  public void createCustomRewardReturnsEmptyResponseWhenAccountMissing() {
    when(accounts.findByRecipientId("rec-1")).thenReturn(Optional.empty());
    var request = createRewardRequest();

    var result = client.createCustomReward("rec-1", "rt-1", request).join();

    assertTrue(Objects.requireNonNull(result.data()).isEmpty());
    assertEquals(200, result.status());
    verifyNoInteractions(api);
  }

  @Test
  public void createCustomRewardSendsAccountTwitchIdAsBroadcaster() {
    when(accounts.findByRecipientId("rec-1"))
      .thenReturn(
        Optional.of(new TwitchAccountData("rec-1", "tw-1", "login", "rt-1"))
      );
    givenUserToken("user-token");
    var request = createRewardRequest();
    when(
      api.createCustomReward(
        eq(CLIENT_ID),
        eq("Bearer user-token"),
        eq("tw-1"),
        eq(request)
      )
    ).thenReturn(
      CompletableFuture.completedFuture(
        new DataWrapper<List<CustomReward>>(List.of(), null, null, null)
      )
    );

    client.createCustomReward("rec-1", "rt-1", request).join();

    verify(api).createCustomReward(CLIENT_ID, "Bearer user-token", "tw-1", request);
  }
}
