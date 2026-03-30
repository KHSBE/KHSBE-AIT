package com.compass.domain.agent.application;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class WeatherService {

    private final RestClient restClient;

    @Value("${weather.api-key:default}")
    private String apiKey;

    public WeatherService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl("https://api.openweathermap.org/data/2.5").build();
    }

    public String getRealWeather(String location) {
        System.out.println("🌍 [외부 API 통신] OpenWeatherMap에서 '" + location + "'의 날씨를 조회합니다.");
        try {
            String searchCity = location.contains("오사카") ? "Osaka" :
                    location.contains("부산") ? "Busan" : location;

            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/weather")
                            .queryParam("q", searchCity)
                            .queryParam("appid", apiKey)
                            .queryParam("units", "metric")
                            .queryParam("lang", "kr")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("weather")) {
                String description = response.get("weather").get(0).get("description").asText();
                double temp = response.get("main").get("temp").asDouble();
                return String.format("%s (현재 기온: %.1f도)", description, temp);
            }
        } catch (Exception e) {
            System.out.println("🚨 날씨 API 호출 실패: " + e.getMessage());
            return "현재 기상청 서버 통신 문제로 정확한 날씨를 알 수 없습니다.";
        }
        return "날씨 정보를 찾을 수 없습니다.";
    }
}