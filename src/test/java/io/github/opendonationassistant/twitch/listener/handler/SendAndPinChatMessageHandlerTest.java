package io.github.opendonationassistant.twitch.listener.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.opendonationassistant.integration.twitch.TwitchApiClient.DataWrapper;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SendChatMessageResponse;
import io.github.opendonationassistant.integration.twitch.TwitchClient;
import io.github.opendonationassistant.twitch.repository.TwitchAccountData;
import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import io.micronaut.serde.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SendAndPinChatMessageHandlerTest {

  private static final TwitchAccountData ACCOUNT = new TwitchAccountData(
    "rec",
    "tw-sender",
    "login",
    "rt-1"
  );

  private TwitchClient twitch = mock(TwitchClient.class);
  private TwitchAccountRepository repository = mock(
    TwitchAccountRepository.class
  );
  private SendAndPinChatMessageHandler handler =
    new SendAndPinChatMessageHandler(
      mock(ObjectMapper.class),
      twitch,
      repository
    );

  private static SendAndPinChatMessageHandler.SendAndPinChatMessageCommand command() {
    return new SendAndPinChatMessageHandler.SendAndPinChatMessageCommand(
      "rec",
      "rt-1",
      "tw-recipient",
      "hello"
    );
  }

  private void givenAccount() {
    when(repository.findByRefreshTokenId("rt-1")).thenReturn(
      Optional.of(ACCOUNT)
    );
  }

  private void givenSendReturns(
    DataWrapper<List<SendChatMessageResponse>> response
  ) {
    when(twitch.sendChatMessage(eq("rec"), eq("rt-1"), any())).thenReturn(
      CompletableFuture.completedFuture(response)
    );
  }

  private void givenPinReturns(
    @Nullable String error,
    @Nullable String message,
    int status
  ) {
    when(
      twitch.pinChatMessage(
        eq("rec"),
        eq("rt-1"),
        eq("tw-recipient"),
        eq("tw-sender"),
        eq("msg-1"),
        any()
      )
    ).thenReturn(
      CompletableFuture.completedFuture(
        new DataWrapper<Void>(null, error, message, status)
      )
    );
  }

  @Test
  public void doesNothingWhenAccountMissing() {
    when(repository.findByRefreshTokenId("rt-1")).thenReturn(Optional.empty());

    assertDoesNotThrow(() -> handler.handle(command()));

    verifyNoInteractions(twitch);
  }

  @Test
  public void pinsSentMessage() {
    givenAccount();
    givenSendReturns(
      new DataWrapper<List<SendChatMessageResponse>>(
        List.of(new SendChatMessageResponse("msg-1", true)),
        null,
        null,
        null
      )
    );
    givenPinReturns(null, null, 200);

    assertDoesNotThrow(() -> handler.handle(command()));

    verify(twitch).pinChatMessage(
      "rec",
      "rt-1",
      "tw-recipient",
      "tw-sender",
      "msg-1",
      null
    );
  }

  @Test
  public void doesNotPinWhenMessageWasNotSent() {
    givenAccount();
    givenSendReturns(
      new DataWrapper<List<SendChatMessageResponse>>(
        List.of(new SendChatMessageResponse("msg-1", false)),
        null,
        null,
        null
      )
    );

    assertDoesNotThrow(() -> handler.handle(command()));

    verify(twitch, never()).pinChatMessage(
      any(),
      any(),
      any(),
      any(),
      any(),
      any()
    );
  }

  @Test
  public void doesNotPinWhenSendReturnedNoData() {
    givenAccount();
    givenSendReturns(
      new DataWrapper<List<SendChatMessageResponse>>(
        List.of(),
        null,
        null,
        null
      )
    );

    assertDoesNotThrow(() -> handler.handle(command()));

    verify(twitch, never()).pinChatMessage(
      any(),
      any(),
      any(),
      any(),
      any(),
      any()
    );
  }

  @Test
  public void doesNotPinWhenSendReturnedError() {
    givenAccount();
    givenSendReturns(
      new DataWrapper<List<SendChatMessageResponse>>(
        null,
        "error",
        "bad request",
        400
      )
    );

    assertDoesNotThrow(() -> handler.handle(command()));

    verify(twitch, never()).pinChatMessage(
      any(),
      any(),
      any(),
      any(),
      any(),
      any()
    );
  }

  @Test
  public void swallowsSendFailure() {
    givenAccount();
    when(twitch.sendChatMessage(eq("rec"), eq("rt-1"), any())).thenThrow(
      new RuntimeException("boom")
    );

    assertDoesNotThrow(() -> handler.handle(command()));
  }

  @Test
  public void swallowsPinFailure() {
    givenAccount();
    givenSendReturns(
      new DataWrapper<List<SendChatMessageResponse>>(
        List.of(new SendChatMessageResponse("msg-1", true)),
        null,
        null,
        null
      )
    );
    givenPinReturns("pin-error", "cannot pin", 400);

    assertDoesNotThrow(() -> handler.handle(command()));
  }
}
