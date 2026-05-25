package io.github.opendonationassistant.twitch.listener.handler;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient;
import io.github.opendonationassistant.integration.twitch.TwitchClient;
import io.github.opendonationassistant.rabbit.TokenRPC;
import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import io.github.opendonationassistant.twitch.repository.TwitchRewardData;
import io.github.opendonationassistant.twitch.repository.TwitchRewardDataRepository;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.Nullable;

@Singleton
public class WidgetChangedEventHandler
  extends AbstractMessageHandler<WidgetChangedEventHandler.WidgetChangedEvent> {

  private final TimeBasedEpochGenerator uuid =
    Generators.timeBasedEpochGenerator();
  private final TwitchRewardDataRepository rewardRepository;
  private final TwitchAccountRepository accountRepository;
  private final TwitchClient twitch;
  private final TokenRPC token;

  @Inject
  public WidgetChangedEventHandler(
    ObjectMapper mapper,
    TwitchRewardDataRepository rewardRepository,
    TwitchAccountRepository accountRepository,
    TwitchClient twitch,
    TokenRPC token
  ) {
    super(mapper);
    this.rewardRepository = rewardRepository;
    this.accountRepository = accountRepository;
    this.twitch = twitch;
    this.token = token;
  }

  @Override
  public void handle(WidgetChangedEvent event) throws IOException {
    if (!"updated".equals(event.type())) {
      return;
    }

    var config = event.widget().config();
    if (config == null) {
      return;
    }

    var properties = config.properties();
    if (properties == null) {
      return;
    }

    var ownerId = event.widget().ownerId();
    if (ownerId == null) {
      return;
    }

    var account = accountRepository.findByRecipientId(ownerId);
    if (account.isEmpty()) {
      return;
    }

    var refreshTokenId = account.get().refreshTokenId();
    var recipientId = ownerId;

    rewardRepository.deleteByRecipientId(recipientId);

    processSystem(properties, "twitch", recipientId, refreshTokenId);
    // processSystem(properties, "vklive", recipientId, refreshTokenId);
    // processSystem(properties, "kick", recipientId, refreshTokenId);
  }

  private void processSystem(
    List<WidgetProperty> properties,
    String system,
    String recipientId,
    String refreshTokenId
  ) {
    var enabled = findBoolProperty(
      properties,
      system + "PointsRequestsEnabled"
    );
    if (!enabled) {
      return;
    }

    var title = findStringProperty(
      properties,
      "music-" + system + "-request-title"
    );
    if (title == null) {
      return;
    }
    Integer cost = findIntProperty(
      properties,
      "music-" + system + "-request-cost"
    );
    if (cost == null) {
      return;
    }

    var accessToken = token
      .token(new TokenRPC.TokenRequest(recipientId, refreshTokenId))
      .token();

    new TwitchApiClient.CreateCustomRewardRequest(
      title,
      cost,
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

    // twitch.createCustomReward(
    //   recipientId,
    //   system,
    //   title,
    //   cost
    // )

    var reward = new TwitchRewardData(
      uuid.generate().toString(),
      recipientId,
      refreshTokenId,
      system,
      cost
    );
    rewardRepository.save(reward);
  }

  private boolean findBoolProperty(
    List<WidgetProperty> properties,
    String name
  ) {
    return properties
      .stream()
      .filter(p -> name.equals(p.name()))
      .findFirst()
      .map(WidgetProperty::value)
      .map(v -> Boolean.TRUE.equals(v))
      .orElse(false);
  }

  private @Nullable String findStringProperty(
    List<WidgetProperty> properties,
    String name
  ) {
    return properties
      .stream()
      .filter(p -> name.equals(p.name()))
      .findFirst()
      .map(WidgetProperty::value)
      .map(Object::toString)
      .orElse(null);
  }

  private @Nullable Integer findIntProperty(
    List<WidgetProperty> properties,
    String name
  ) {
    return properties
      .stream()
      .filter(p -> name.equals(p.name()))
      .findFirst()
      .map(WidgetProperty::value)
      .filter(v -> v instanceof Number)
      .map(v -> ((Number) v).intValue())
      .orElse(null);
  }

  @Serdeable
  public static record WidgetChangedEvent(
    String type,
    Widget widget,
    String source,
    @Nullable String originId
  ) {}

  @Serdeable
  public static record Widget(
    String id,
    String type,
    Integer sortOrder,
    String name,
    Boolean enabled,
    String ownerId,
    WidgetConfig config
  ) {}

  @Serdeable
  public static record WidgetConfig(List<WidgetProperty> properties) {}

  @Serdeable
  public static record WidgetProperty(
    String name,
    @Nullable String displayName,
    @Nullable String type,
    @Nullable Object value
  ) {}
}
