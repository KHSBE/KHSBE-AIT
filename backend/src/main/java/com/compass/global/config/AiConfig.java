package com.compass.global.config;

import com.compass.domain.agent.application.WeatherService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
public class AiConfig {

    private static final String DEFAULT_SYSTEM_PROMPT =
            "당신은 'Compass' 여행 AI 플래너입니다. 오늘은 " + LocalDate.now() + " 입니다. " +
            "정확하고 실용적인 답변을 우선으로 제공합니다.";

    @Bean("generalChatClient")
    public ChatClient generalChatClient(
            ChatClient.Builder builder,
            @Value("${app.ai.model.general:gemini-3.1-flash-lite-preview}") String generalModel
    ) {
        return builder.clone()
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .defaultOptions(GoogleGenAiChatOptions.builder().model(generalModel).build())
                .build();
    }

    @Bean("planChatClient")
    public ChatClient planChatClient(
            ChatClient.Builder builder,
            @Value("${app.ai.model.plan:gemini-3.1-pro}") String planModel
    ) {
        return builder.clone()
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .defaultOptions(GoogleGenAiChatOptions.builder().model(planModel).build())
                .build();
    }

    public record WeatherRequest(String location) {}
    public record WeatherResponse(String location, String weatherInfo) {}

    @Bean
    public ToolCallback currentWeatherFunction(WeatherService weatherService) {
        return FunctionToolCallback.builder("currentWeatherFunction",
                        (WeatherRequest request) -> {
                            String realWeatherInfo = weatherService.getRealWeather(request.location());
                            return new WeatherResponse(request.location(), realWeatherInfo);
                        })
                .description("특정 지역의 현재 날씨와 온도를 조회합니다. 예: Seoul, Osaka")
                .inputType(WeatherRequest.class)
                .build();
    }
}
