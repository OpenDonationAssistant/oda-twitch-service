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
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SendChatMessageRequest;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.SendChatMessageResponse;
import io.github.opendonationassistant.integration.twitch.TwitchClient;
import io.github.opendonationassistant.twitch.repository.TwitchAccountData;
import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import io.micronaut.serde.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

public class SendChatMessageHandlerTest {

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
  private SendChatMessageHandler handler = new SendChatMessageHandler(
    mock(ObjectMapper.class),
    twitch,
    repository
  );

  private static SendChatMessageHandler.SendTwitchMessageCommand command() {
    return new SendChatMessageHandler.SendTwitchMessageCommand(
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

  @Test
  public void doesNothingWhenAccountMissing() {
    when(repository.findByRefreshTokenId("rt-1")).thenReturn(Optional.empty());

    assertDoesNotThrow(() -> handler.handle(command()));

    verifyNoInteractions(twitch);
  }

  @Test
  public void sendsChatMessage() {
    givenAccount();
    givenSendReturns(
      new DataWrapper<List<SendChatMessageResponse>>(
        List.of(new SendChatMessageResponse("msg-1", true)),
        null,
        null,
        200
      )
    );

    assertDoesNotThrow(() -> handler.handle(command()));

    verify(twitch).sendChatMessage(
      "rec",
      "rt-1",
      new SendChatMessageRequest("tw-recipient", "tw-sender", "hello")
    );
  }

  @Test
  public void doesNotThrowWhenSendReturnedError() {
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
  public void doesNotSendWhenAccountMissing() {
    when(repository.findByRefreshTokenId("rt-1")).thenReturn(Optional.empty());

    assertDoesNotThrow(() -> handler.handle(command()));

    verify(twitch, never()).sendChatMessage(any(), any(), any());
  }
}
