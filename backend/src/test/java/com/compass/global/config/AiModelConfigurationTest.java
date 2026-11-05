package com.compass.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiModelConfigurationTest {
    @Test
    void defaultsUseFlash38ForChatAndKeepProForPlans() throws IOException {
        var environment = configuration(Map.of());
        assertEquals("gemini-3.8-flash", environment.getProperty("app.ai.model.general"));
        assertEquals("gemini-3.8-flash", environment.getProperty("spring.ai.google.genai.chat.options.model"));
        assertEquals("gemini-3.1-pro-preview", environment.getProperty("app.ai.model.plan"));
    }

    @Test
    void modelOverridesAlsoReachTheUnderlyingChatModel() throws IOException {
        var environment = configuration(Map.of(
                "AI_MODEL_GENERAL", "gemini-3.7-flash",
                "AI_MODEL_PLAN", "custom-plan-model"
        ));
        assertEquals("gemini-3.7-flash", environment.getProperty("app.ai.model.general"));
        assertEquals("gemini-3.7-flash", environment.getProperty("spring.ai.google.genai.chat.options.model"));
        assertEquals("custom-plan-model", environment.getProperty("app.ai.model.plan"));
    }

    private StandardEnvironment configuration(Map<String, Object> overrides) throws IOException {
        var environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addFirst(new MapPropertySource("test-overrides", overrides));
        for (var source : new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"))) {
            environment.getPropertySources().addLast(source);
        }
        return environment;
    }
}
