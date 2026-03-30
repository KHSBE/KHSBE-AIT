package com.compass.global.config;

import com.compass.domain.agent.application.WeatherService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
public class AiConfig {

    // 1. 기본 AI 자아 설정
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("당신은 'Compass'의 수석 AI 여행 플래너입니다. " +
                        "오늘은 " + LocalDate.now() + " 입니다. " +
                        "항상 친절하고 유쾌한 전문가의 톤으로 답변하세요.")
                .build();
    }

    // 2. 데이터 주고받을 DTO
    public record WeatherRequest(String location) {}
    public record WeatherResponse(String location, String weatherInfo) {}

    // ★ 3. 핵심 해결책: 최신 버전(1.1.0-M2)의 ToolCallback 방식으로 도구를 직접 생성하여 Bean으로 등록합니다!
    @Bean
    public ToolCallback currentWeatherFunction(WeatherService weatherService) {
        return FunctionToolCallback.builder("currentWeatherFunction",
                        (WeatherRequest request) -> {
                            System.out.println("💡 [AI 도구 사용] 검색 지역: " + request.location());
                            String realWeatherInfo = weatherService.getRealWeather(request.location());
                            return new WeatherResponse(request.location(), realWeatherInfo);
                        })
                .description("특정 지역의 현재 날씨와 온도를 가져옵니다. 반드시 'Seoul', 'Osaka'와 같이 영문 도시명으로 검색하세요.")
                .inputType(WeatherRequest.class)
                .build();
    }
}