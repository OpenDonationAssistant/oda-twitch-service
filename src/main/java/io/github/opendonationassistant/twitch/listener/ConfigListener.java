package io.github.opendonationassistant.twitch.listener;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import io.github.opendonationassistant.commons.logging.ODALogger;
import io.github.opendonationassistant.events.AbstractMessageHandler;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient;
import io.github.opendonationassistant.integration.twitch.TwitchApiClient.UpdateCustomRewardRequest;
import io.github.opendonationassistant.integration.twitch.TwitchClient;
import io.github.opendonationassistant.rabbit.Exchange;
import io.github.opendonationassistant.twitch.repository.TwitchAccountData;
import io.github.opendonationassistant.twitch.repository.TwitchAccountRepository;
import io.github.opendonationassistant.twitch.repository.TwitchRewardData;
import io.github.opendonationassistant.twitch.repository.TwitchRewardDataRepository;
import io.github.opendonationassistant.twitch.repository.TwitchRewardRepository;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

@RabbitListener(executor = "config-listener")
public class ConfigListener
  extends AbstractMessageHandler<ConfigListener.WidgetChangedEvent> {

  private ODALogger log = new ODALogger(this);

  private static final String WIDGET_TYPE = "media";
  public static final String QUEUE_NAME = "twitch.config";
  public static final io.github.opendonationassistant.rabbit.Queue QUEUE =
    new io.github.opendonationassistant.rabbit.Queue(QUEUE_NAME);
  public static final Exchange BINDING = Exchange.Exchange(
    "changes.widgets",
    Map.of(WIDGET_TYPE, ConfigListener.QUEUE)
  );

  private final TimeBasedEpochGenerator uuid =
    Generators.timeBasedEpochGenerator();
  private final TwitchRewardRepository rewardRepository;
  private final TwitchAccountRepository accountRepository;
  private final TwitchClient twitch;

  @Inject
  public ConfigListener(
    ObjectMapper mapper,
    TwitchRewardRepository rewardRepository,
    TwitchAccountRepository accountRepository,
    TwitchClient twitch
  ) {
    super(mapper);
    this.rewardRepository = rewardRepository;
    this.accountRepository = accountRepository;
    this.twitch = twitch;
  }

  @Queue(QUEUE_NAME)
  public void handle(WidgetChangedEvent event) throws IOException {
    if (!"updated".equals(event.type())) {
      return;
    }

    var widget = event.widget();
    if (widget == null) {
      return;
    }
    if (!WIDGET_TYPE.equals(widget.type())) {
      return;
    }

    var widgetId = widget.id();
    if (widgetId == null) {
      return;
    }

    var config = widget.config();
    if (config == null) {
      return;
    }

    var properties = config.properties();
    if (properties == null) {
      return;
    }

    Optional.ofNullable(widget.ownerId())
      .flatMap(accountRepository::findByRecipientId)
      .ifPresent(account ->
        processSystem(widgetId, properties, "music", account)
      );
  }

  private void processSystem(
    String widgetId,
    List<WidgetProperty> properties,
    String system,
    TwitchAccountData account
  ) {
    var refreshTokenId = account.refreshTokenId();
    var enabled = findBoolProperty(
      properties,
      system + "PointsRequestsEnabled"
    );
    log.info("music-" + system + "-request-title: " + enabled);
    if (!enabled) {
      return;
    }

    var title = findStringProperty(
      properties,
      "music-" + system + "-request-title"
    );
    log.info("music-" + system + "-request-title: " + title);
    if (title == null) {
      return;
    }
    Integer cost = findIntProperty(
      properties,
      "music-" + system + "-request-cost"
    );
    log.info("music-" + system + "-request-cost: " + cost);
    if (cost == null) {
      return;
    }

    rewardRepository
      .findByWidgetId(widgetId)
      .ifPresentOrElse(
        reward -> {
          twitch
            .updateCustomReward(
              account.recipientId(),
              account.refreshTokenId(),
              account.twitchId(),
              reward.data().id(),
              new UpdateCustomRewardRequest(
                title,
                cost,
                null,
                true,
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
              )
            )
            .join();
        },
        () -> {
          twitch
            .createCustomReward(
              account.recipientId(),
              account.refreshTokenId(),
              new TwitchApiClient.CreateCustomRewardRequest(
                title,
                cost,
                null,
                true,
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null
              )
            )
            .join()
            .data()
            .forEach(reward -> {
              rewardRepository.create(
                new TwitchRewardData(
                  reward.id(),
                  account.recipientId(),
                  account.refreshTokenId(),
                  widgetId,
                  "music"
                )
              );
            });
        }
      );
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
