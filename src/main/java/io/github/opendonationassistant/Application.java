package io.github.opendonationassistant;

import io.github.opendonationassistant.rabbit.RabbitConfiguration;
import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.ApplicationContextConfigurer;
import io.micronaut.context.annotation.ContextConfigurer;
import io.micronaut.rabbitmq.connect.ChannelInitializer;
import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.*;
import jakarta.inject.Singleton;

@OpenAPIDefinition(info = @Info(title = "oda-twitch-service"))
public class Application {

  @ContextConfigurer
  public static class DefaultEnvironmentConfigurer
    implements ApplicationContextConfigurer {

    @Override
    public void configure(ApplicationContextBuilder builder) {
      builder.defaultEnvironments("standalone");
    }
  }

  public static void main(String[] args) {
    Micronaut.build(args).mainClass(Application.class).banner(false).start();
  }

  @Singleton
  public ChannelInitializer rabbitConfiguration() {
    return new RabbitConfiguration();
  }
}
