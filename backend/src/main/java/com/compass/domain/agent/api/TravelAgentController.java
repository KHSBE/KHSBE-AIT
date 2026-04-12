package com.compass.domain.agent.api;

import com.compass.domain.agent.application.PlacesVerificationService;
import com.compass.domain.agent.application.PlanQualityValidator;
import com.compass.domain.agent.application.RequestQuotaService;
import com.compass.domain.agent.application.TravelAgentService;
import com.compass.domain.agent.dao.ChatMessageRepository;
import com.compass.domain.agent.dao.ChatSessionRepository;
import com.compass.domain.agent.domain.ChatMessage;
import com.compass.domain.agent.domain.ChatSession;
import com.compass.domain.agent.dto.AiPlanResponse;
import com.compass.domain.agent.dto.PlanRequest;
import com.compass.domain.plan.application.PlanCommandService;
import com.compass.domain.plan.dao.TravelPlanRepository;
import com.compass.domain.plan.domain.Itinerary;
import com.compass.domain.user.dao.UserRepository;
import com.compass.global.common.ApiResponse;
import com.compass.global.error.BusinessException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Slf4j
public class TravelAgentController {

    private final TravelAgentService agentService;
    private final PlacesVerificationService placesVerificationService;
    private final PlanQualityValidator planQualityValidator;
    private final RequestQuotaService requestQuotaService;
    private final PlanCommandService planService;
    private final TravelPlanRepository planRepository;
    private final UserRepository userRepository;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    @PostMapping("/plan")
    @Transactional
    public ApiResponse<AiPlanResponse> createPlan(@Valid @RequestBody PlanRequest req, Authentication auth) {
        var user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        requestQuotaService.assertPlanCreationAllowed(user.getId());

        var trimmedRequest = req.request().trim();
        var session = (req.sessionId() != null)
                ? sessionRepository.findByIdAndUserId(req.sessionId(), user.getId())
                .orElseThrow(() -> new BusinessException("채팅 세션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND))
                : sessionRepository.save(ChatSession.builder()
                .user(user)
                .sessionTitle(trimmedRequest.substring(0, Math.min(trimmedRequest.length(), 10)) + "...")
                .build());

        var history = messageRepository.findAllBySessionIdOrderBySentAtAsc(session.getId());

        AiPlanResponse aiRes = agentService.generateTravelPlan(user, trimmedRequest, history);
        planQualityValidator.validate(aiRes);
        AiPlanResponse verifiedPlan = placesVerificationService.verifyPlan(aiRes);
        PlacesVerificationService.AccommodationVerification accommodationVerification =
                placesVerificationService.verifyAccommodation(verifiedPlan.accommodation(), verifiedPlan.region());

        Long savedPlanId = planService.savePlanFromAi(user, verifiedPlan, accommodationVerification);
        String responseAccommodation = accommodationVerification != null
                ? accommodationVerification.name()
                : verifiedPlan.accommodation();

        messageRepository.save(ChatMessage.builder()
                .session(session)
                .senderRole("USER")
                .content(trimmedRequest)
                .build());

        messageRepository.save(ChatMessage.builder()
                .session(session)
                .senderRole("ASSISTANT")
                .content("요청하신 일정 초안 생성이 완료되었습니다.")
                .build());

        try {
            var savedPlan = planRepository.findByIdAndUserIdWithDetails(savedPlanId, user.getId())
                    .orElseThrow(() -> new BusinessException("저장된 일정을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

            Map<Integer, Itinerary> itineraryByDay = savedPlan.getItineraries().stream()
                    .collect(Collectors.toMap(Itinerary::getDayNumber, Function.identity(), (left, right) -> left));

            var enrichedDays = verifiedPlan.itineraries() == null
                    ? null
                    : verifiedPlan.itineraries().stream()
                    .map(day -> {
                        Itinerary savedItinerary = itineraryByDay.get(day.day());
                        Long itineraryId = savedItinerary != null ? savedItinerary.getId() : day.itineraryId();
                        return new AiPlanResponse.DayItinerary(
                                itineraryId,
                                day.day(),
                                day.theme(),
                                day.places()
                        );
                    })
                    .toList();

            AiPlanResponse enrichedPlan = new AiPlanResponse(
                    savedPlanId,
                    verifiedPlan.title(),
                    verifiedPlan.region(),
                    responseAccommodation,
                    verifiedPlan.startDate(),
                    verifiedPlan.endDate(),
                    enrichedDays
            );

            return ApiResponse.success(enrichedPlan);
        } catch (Exception e) {
            log.warn("[PLAN-CREATE] enrichment failed. savedPlanId={}, userId={}, message={}",
                    savedPlanId, user.getId(), e.getMessage());
            AiPlanResponse fallbackPlan = new AiPlanResponse(
                    savedPlanId,
                    verifiedPlan.title(),
                    verifiedPlan.region(),
                    responseAccommodation,
                    verifiedPlan.startDate(),
                    verifiedPlan.endDate(),
                    verifiedPlan.itineraries()
            );
            return ApiResponse.success(fallbackPlan);
        }
    }
}
