package com.compass.global.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.*;
import org.springframework.core.io.ClassPathResource;

class AiModelConfigurationTest {
  @Test
  void defaultAndOverride() throws Exception {
    var env = new StandardEnvironment();
    env.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    env.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
    for (var source :
        new YamlPropertySourceLoader()
            .load("application", new ClassPathResource("application.yml")))
      env.getPropertySources().addLast(source);
    assertEquals("gemini-4-argon", env.getProperty("app.ai.model.plan"));
    env.getPropertySources()
        .addFirst(new MapPropertySource("override", Map.of("AI_MODEL_PLAN", "custom")));
    assertEquals("custom", env.getProperty("app.ai.model.plan"));
  }
}
