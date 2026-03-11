package com.compass.domain.agent.application;

import com.compass.domain.user.domain.User;
import com.compass.domain.user.domain.UserPreference;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelAgentService {

    private final ChatModel chatModel; // Gemini


    public String generateTravelPlan(User user, String request) {
        UserPreference pref = user.getPreference();

        // 1. 프롬프트 템플릿 작성 (에이전트의 성격 정의)
        String template = """
            당신은 전문 여행 플래너 'Compass'입니다. 
            사용자의 요청과 취향을 바탕으로 최적의 여행 일정을 JSON 형식으로 제안하세요.
            
            [사용자 정보]
            - 선호 카테고리: {category}
            - 식단 제한: {diet}
            - 여행 페이스: {pace}
            
            [사용자 요청]
            {userRequest}
            
            [출력 형식]
            반드시 다음 필드를 포함한 JSON 형식으로만 답변하세요:
            - title: 여행 제목
            - region: 지역
            - startDate, endDate: 날짜
            - itineraries: [ { day: 1, theme: '테마', places: [ { name: '장소명', memo: '추천 이유' } ] } ]
            """;

        // 2. 파라미터 바인딩
        PromptTemplate promptTemplate = new PromptTemplate(template);
        Prompt prompt = promptTemplate.create(Map.of(
                "category", pref != null ? pref.getPreferredCategory() : "없음",
                "diet", pref != null ? pref.getDietRestriction() : "없음",
                "pace", pref != null ? pref.getTravelPace() : "보통",
                "userRequest", request
        ));

        // 3. AI 호출 및 결과 반환
        return chatModel.call(prompt).getResult().getOutput().getContent();
    }
}