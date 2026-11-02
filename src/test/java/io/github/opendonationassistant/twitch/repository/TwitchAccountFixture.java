package io.github.opendonationassistant.twitch.repository;

import static org.mockito.Mockito.mock;

import io.github.opendonationassistant.integration.media.MediaRPC;
import io.github.opendonationassistant.integration.twitch.TwitchClient;

/**
 * Test-only factory for {@link TwitchAccount} instances.
 *
 * <p>Account collaborators are mocked: tests that exercise the account itself
 * build it with their own mocks instead.
 */
public final class TwitchAccountFixture {

  private static final TwitchAccountData DEFAULT_DATA = new TwitchAccountData(
    "rec",
    "tw",
    "login",
    "rt"
  );

  private TwitchAccountFixture() {}

  public static TwitchAccount account() {
    return account(DEFAULT_DATA);
  }

  public static TwitchAccount account(TwitchAccountData data) {
    return new TwitchAccount(
      data,
      mock(TwitchRewardDataRepository.class),
      mock(MediaRPC.class),
      mock(TwitchClient.class)
    );
  }

  public static TwitchAccount account(
    String recipientId,
    String twitchId,
    String twitchLogin,
    String refreshTokenId
  ) {
    return account(
      new TwitchAccountData(
        recipientId,
        twitchId,
        twitchLogin,
        refreshTokenId
      )
    );
  }
}
